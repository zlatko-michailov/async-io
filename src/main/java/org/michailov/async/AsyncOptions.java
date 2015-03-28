package org.michailov.async;

import java.util.concurrent.*;

/**
 * Common options that control the execution of async methods from this package.
 *  
 * @author Zlatko Michailov
 */
public class AsyncOptions {
    
    /**
     * Shared instance that represents the "default" set of options.
     * Use this field to avoid unnecessary heap allocations. 
     */
    public static final AsyncOptions DEFAULT = new AsyncOptions();
    
    /**
     * Sentinel that represents an infinite number of time units.
     */
    public static final long TIMEOUT_INFINITE = -1;
    
    /**
     * Number of time units before which a given async call should complete.
     * Once this time is up, the future returned from the given async call will be completed exceptionally.
     * Default is {@link #TIMEOUT_INFINITE}.
     */
    public long timeout = TIMEOUT_INFINITE;
    
    /**
     * Unit type of the timeout.
     * Default is TimeUnit.MILLISECONDS.
     */
    public TimeUnit timeUnit = TimeUnit.MILLISECONDS;
}
