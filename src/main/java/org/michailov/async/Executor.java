package org.michailov.async;

import java.util.concurrent.*;

/**
 * This class abstracts away the mess in Java aync scheduling.  
 * 
 * @author Zlatko Michailov
 */
class Executor {
    
    private static final ForkJoinPool MAIN_THREAD_POOL = ForkJoinPool.commonPool();
    private static final int DELAY_THREAD_COUNT = 1;
    private static final ScheduledThreadPoolExecutor DELAY_THREAD_POOL = new ScheduledThreadPoolExecutor(DELAY_THREAD_COUNT);
    
    /**
     * Schedules an operation for an immediate execution on the main thread pool.
     * 
     * @param   runnable The operation to be executed.
     */
    static void execute(Runnable runnable) {
        MAIN_THREAD_POOL.execute(runnable);
    }

    /**
     * Schedules an operation for a delayed execution on the main thread pool.
     * 
     * @param   runnable    The operation to be executed.
     * @param   delayMillis The amount of milliseconds to delay the execution.
     */
    static void executeAfter(Runnable runnable, long delayMillis) {
        DELAY_THREAD_POOL.schedule(() -> execute(runnable), delayMillis, TimeUnit.MILLISECONDS);
    }

}
