/**
 * Copyright (c) Zlatko Michailov
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private static final int THROTTLE_THRESHOLD = 4;
    private static final int RETRY_DELAY_MILLIS = 10;
    private static final int MAX_DELAY_MILLIS = 200;
    
    /**
     * Schedules an operation for an immediate execution on the main thread pool.
     * 
     * @param   runnable The operation to be executed.
     */
    static void execute(Runnable runnable) {
        MAIN_THREAD_POOL.execute(runnable);
    }

    /**
     * Schedules an operation for a delayed execution.
     * 
     * @param   runnable    The operation to be executed.
     * @param   delayMillis The amount of milliseconds to delay the execution.
     */
    static void executeAfter(Runnable runnable, long delayMillis) {
        DELAY_THREAD_POOL.schedule(() -> execute(runnable), delayMillis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Schedules an operation for delayed or immediate execution based on the retry count.
     * 
     * @param   runnable    The operation to be executed.
     * @param   retryCount  Count of "dummy" retries. 
     */
    static void throttleExecute(Runnable runnable, int retryCount) {
        if (retryCount % THROTTLE_THRESHOLD == 0) {
            long delayMillis = Math.min(retryCount * RETRY_DELAY_MILLIS, MAX_DELAY_MILLIS);
            executeAfter(runnable, delayMillis);
        }
        else {
            execute(runnable);
        }
    }
    
}
