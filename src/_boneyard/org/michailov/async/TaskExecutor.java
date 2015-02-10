package org.michailov.async.io;

import java.util.concurrent.*;

public class TaskExecutor extends ScheduledThreadPoolExecutor {
    private static Object SYNC_OBJ = new Object();
    private static TaskExecutor _taskExecutor;
    
    public static TaskExecutor getTaskExecutor() {
        ensureInitialized();
        
        return _taskExecutor;
    }
    
    private TaskExecutor(int corePoolSize) {
        super(corePoolSize);
    }
    
    private static void ensureInitialized() {
        // Do a quick check.
        if (_taskExecutor == null) {
            // Take the lock.
            synchronized(SYNC_OBJ) {
                // Now check for sure.
                if (_taskExecutor == null) {
                    int processorCount = Runtime.getRuntime().availableProcessors();
                    int corePoolSize = 2 * processorCount; // Magic formula.
                    _taskExecutor = new TaskExecutor(corePoolSize);
                }
            }
        }
    }

}
