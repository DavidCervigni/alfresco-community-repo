package org.alfresco.util.cache;


public interface AsynchronouslyRefreshedCacheRegistry
{
      /**
     * Register a listener
     * @param listener
     */
    void register(RefreshableCacheListener listener);
    
    /**
     * Fire an event 
     * @param event
     * @param toAll - true goes to all listeners, false only to listeners that have a matching cacheId 
     */
    void broadcastEvent(RefreshableCacheEvent event, boolean toAll);

}
