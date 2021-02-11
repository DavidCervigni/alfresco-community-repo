package org.alfresco.util.cache;

import java.io.Serializable;

public interface RefreshableCacheEvent extends Serializable
{
    /**
     * Get the cache id
     */
    String getCacheId();
    
        
    /**
     * Get the affected key/tenant id
     */
    String getKey();
    
}