/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.util.cache;

import java.util.Objects;

/**
 * A generic event with the cache id and affected tenant
 * 
 * @author Andy
 */
public class RefreshableCacheEventImpl implements RefreshableCacheEvent
{
    private static final long serialVersionUID = 1324638640132648062L;

    private final String cacheId;
    private final String key;

    RefreshableCacheEventImpl(final String cacheId, final String key)
    {
        this.cacheId = cacheId;
        this.key = key;
    }

    @Override
    public String getCacheId()
    {
        return cacheId;
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public String toString()
    {
        return "AbstractRefreshableCacheEvent [cacheId=" + cacheId + ", tenantId=" + key + "]";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshableCacheEventImpl that = (RefreshableCacheEventImpl) o;
        return Objects.equals(cacheId, that.cacheId) &&
               Objects.equals(key, that.key);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(cacheId, key);
    }
}
