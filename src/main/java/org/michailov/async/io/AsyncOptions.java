package org.michailov.async.io;

import java.util.concurrent.*;

public class AsyncOptions {
    public static final long TIMEOUT_INFINITE = -1;
    
    public long timeout = TIMEOUT_INFINITE;
    public TimeUnit timeUnit = TimeUnit.MILLISECONDS;
}
