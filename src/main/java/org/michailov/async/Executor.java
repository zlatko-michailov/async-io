package org.michailov.async;

import java.util.concurrent.*;

class Executor {
    
    private static final ForkJoinPool MAIN_THREAD_POOL = ForkJoinPool.commonPool();
    private static final int DELAY_THREAD_COUNT = 1;
    private static final ScheduledThreadPoolExecutor DELAY_THREAD_POOL = new ScheduledThreadPoolExecutor(DELAY_THREAD_COUNT);
    
    static void execute(Runnable task) {
        MAIN_THREAD_POOL.execute(task);
    }

    static void executeAfter(Runnable task, long delayMillis) {
        DELAY_THREAD_POOL.schedule(() -> execute(task), delayMillis, TimeUnit.MILLISECONDS);
    }

}
