/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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

import static org.alfresco.util.cache.RefreshState.DONE;
import static org.alfresco.util.cache.RefreshState.IDLE;
import static org.alfresco.util.cache.RefreshState.RUNNING;
import static org.alfresco.util.cache.RefreshState.WAITING;

import java.util.Objects;

class Refresh
{
    private final String key;

    private volatile RefreshState state = WAITING;

    Refresh(String key)
    {
        this.key = key;
    }

    /**
     * @return the tenantId
     */
    public String getKey()
    {
        return key;
    }

    /**
     * @return the state
     */
    public RefreshState getState()
    {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(RefreshState state)
    {
        this.state = state;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Refresh refresh = (Refresh) o;
        return Objects.equals(key, refresh.key) &&
               state == refresh.state;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(key, state);
    }

    @Override
    public String toString()
    {
        return "Refresh [key=" + key + ", state=" + state + ", hashCode()=" + hashCode() + "]";
    }

    public boolean isIdle()
    {
        return state == IDLE;
    }

    public boolean isWaiting()
    {
        return state == WAITING;
    }

    public boolean isRunning()
    {
        return state == RUNNING;
    }

    public boolean isDone()
    {
        return state == DONE;
    }
}
