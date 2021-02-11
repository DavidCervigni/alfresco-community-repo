/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.node.db;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.alfresco.repo.node.cleanup.AbstractNodeCleanupWorker;
import org.alfresco.repo.transaction.RetryingTransactionHelper;

/**
 * Cleans up deleted nodes and dangling transactions that are old enough.
 * 
 * @author Derek Hulley
 * @since 2.2 SP2
 */
public class DeletedNodeCleanupWorker extends AbstractNodeCleanupWorker
{
    private long minPurgeAgeMs        = 7L * 24L * 3600L * 1000L;
    // used for tests, to consider only transactions after a certain commit time
    private long fromCustomCommitTime = -1;

    // Unused transactions will be purged in chunks determined by commit time boundaries. 'index.tracking.purgeSize' specifies the size
    // of the chunk (in ms). Default is a couple of hours.
    private int purgeSize = 2 * 60 * 60 * 1000; // 2h in ms

    /**
     * {@inheritDoc}
     */
    protected List<String> doCleanInternal()
    {
        if (minPurgeAgeMs < 0)
        {
            return singletonList("Minimum purge age is negative; purge disabled");
        }

        return Stream
            .of(
                purgeOldDeletedNodes(minPurgeAgeMs),
                purgeOldEmptyTransactions(minPurgeAgeMs))
            .flatMap(Collection::stream)
            .collect(toList());
    }

    /**
     * Set the minimum age (days) that nodes and transactions must be before they get purged.
     * The default is 7 days.
     *
     * @param minPurgeAgeDays the minimum age (in days) before nodes and transactions get purged
     */
    public void setMinPurgeAgeDays(final int minPurgeAgeDays)
    {
        this.minPurgeAgeMs = ((long) minPurgeAgeDays) * 24L * 3600L * 1000L;
    }

    /**
     * Set a custom "from commit time" that will consider only the transactions after this specified time
     * Setting a negative value or 0 will trigger the default behaviour to get the oldest "from time" for any deleted node
     *
     * @param fromCustomCommitTime the custom from commit time value
     */
    public void setFromCustomCommitTime(final long fromCustomCommitTime)
    {
        this.fromCustomCommitTime = fromCustomCommitTime;
    }

    /**
     * Set the purge transaction block size. This determines how many unused transactions are purged in one go.
     *
     * @param purgeSize int
     */
    public void setPurgeSize(int purgeSize)
    {
        this.purgeSize = purgeSize;
    }

	/**
     * Cleans up deleted nodes that are older than the given minimum age.
     * 
     * @param minAge        the minimum age of a transaction or deleted node
     * @return              Returns log message results
     */
    private List<String> purgeOldDeletedNodes(long minAge)
    {
        final List<String> results = new ArrayList<>(100);

        final long maxCommitTime = currentTimeMillis() - minAge;
        long fromCommitTime = fromCustomCommitTime;
        if (fromCommitTime <= 0L)
        {
            fromCommitTime = nodeDAO.getMinTxnCommitTimeForDeletedNodes();
        }
        if ( fromCommitTime == 0L )
        {
              String msg = "There are no old nodes to purge.";
              results.add(msg);
              return results;
        }

        long loopPurgeSize = purgeSize;
        long purgeCount;
        while (true)
        {
            // Ensure we keep the lock
            refreshLock();
            
            final RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
            txnHelper.setMaxRetries(5);                             // Limit number of retries
            txnHelper.setRetryWaitIncrementMs(1000);                // 1 second to allow other cleanups time to get through

            long toCommitTime = fromCommitTime + loopPurgeSize;
            if(toCommitTime > maxCommitTime)
            {
                toCommitTime = maxCommitTime;
            }
            
            try
            {
                final long start = fromCommitTime;
                final long finish = toCommitTime;
                purgeCount = txnHelper.doInTransaction(
                    () -> (long) nodeDAO.purgeNodes(start, finish),
                    false,
                    true);

                if (purgeCount > 0)
                {
                    results.add(
                        "Purged old nodes: \n" +
                        "   From commit time (ms):    " + fromCommitTime + "\n" +
                        "   To commit time (ms):      " + toCommitTime + "\n" +
                        "   Purge count:     " + purgeCount);
                }

                fromCommitTime += loopPurgeSize;
                
                // If the delete succeeded, double the loopPurgeSize
                loopPurgeSize *= 2L;
                if (loopPurgeSize > purgeSize)
                {
                    loopPurgeSize = purgeSize;
                }
            }
            catch (Throwable e)
            {
                String msg =
                    "Failed to purge nodes. \n" +
                    "  If the purgable set is too large for the available DB resources \n" +
                    "  then the nodes can be purged manually as well. \n" +
                    "  Set log level to WARN for this class to get exception log: \n" +
                    "   From commit time (ms):    " + fromCommitTime + "\n" +
                    "   To commit time (ms):      " + toCommitTime + "\n" +
                    "   Error:       " + e.getMessage();
                // It failed; do a full log in WARN mode
                if (logger.isWarnEnabled())
                {
                    logger.warn(msg, e);
                }
                else
                {
                    logger.error(msg);
                }
                results.add(msg);
                
                // If delete failed, halve the loopPurgeSize and try again
                loopPurgeSize /= 2L;
                // If the purge size drops below 10% of the original size, the entire process must stop
                if (loopPurgeSize < 0.1 * purgeSize)
                {
                    msg ="Failed to purge nodes. \n" +
                         " The purge time interval dropped below 10% of the original size (" + purgeSize + "), so the purging process was stopped.";
                    if (logger.isWarnEnabled())
                    {
                        logger.warn(msg, e);
                    }
                    else
                    {
                        logger.error(msg);
                    }
                    results.add(msg);
                    break;
                }
            }
                      
            if(fromCommitTime >= maxCommitTime)
            {
                break;
            }
        }
            
        // Done
        return results;
    }

    /**
     * Cleans up unused transactions that are older than the given minimum age.
     * 
     * @param minAge        the minimum age of a transaction or deleted node
     * @return              Returns log message results
     */
    private List<String> purgeOldEmptyTransactions(long minAge)
    {
        if (minAge < 0)
        {
            return emptyList();
        }
        final List<String> results = new ArrayList<>(100);

        final long maxCommitTime = currentTimeMillis() - minAge;
        long fromCommitTime = fromCustomCommitTime;
        if (fromCommitTime <= 0L)
        {
            fromCommitTime = nodeDAO.getMinUnusedTxnCommitTime();
        }
    	// delete unused transactions in batches of size 'purgeTxnBlockSize'
        while (true)
        {
            // Ensure we keep the lock
            refreshLock();
            
            RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
            txnHelper.setMaxRetries(5);                             // Limit number of retries
            txnHelper.setRetryWaitIncrementMs(1000);                // 1 second to allow other cleanups time to get through

            long toCommitTime = fromCommitTime + purgeSize;
            if(toCommitTime >= maxCommitTime)
            {
            	toCommitTime = maxCommitTime;
            }

            // Purge transactions
            try
            {
                final long start = fromCommitTime;
                final long finish = toCommitTime;
                long purgeCount = txnHelper.doInTransaction(
                    () -> (long) nodeDAO.deleteTxnsUnused(start, finish),
                    false,
                    true);
                if (purgeCount > 0)
                {
                    results.add("Purged old txns: \n" +
                                "   From commit time (ms):    " + fromCommitTime + "\n" +
                                "   To commit time (ms):      " + toCommitTime + "\n" +
                                "   Purge count:     " + purgeCount);
                }
            }
            catch (Throwable e)
            {
                final String msg =
                    "Failed to purge txns." +
                    "  Set log level to WARN for this class to get exception log: \n" +
                    "   From commit time:      " + fromCommitTime + "\n" +
                    "   To commit time (ms):   " + toCommitTime + "\n" +
                    "   Error:       " + e.getMessage();
                // It failed; do a full log in WARN mode
                if (logger.isWarnEnabled())
                {
                    logger.warn(msg, e);
                }
                else
                {
                    logger.error(msg);
                }
                results.add(msg);
                break;
            }

            fromCommitTime += purgeSize;
            if(fromCommitTime >= maxCommitTime)
            {
            	break;
            }
        }
        // Done
        return results;
    }
}