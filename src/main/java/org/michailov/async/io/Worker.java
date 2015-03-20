package org.michailov.async.io;

import java.util.concurrent.CompletableFuture;

import org.michailov.async.AsyncException;
import org.michailov.async.WhenReady;

abstract class Worker {

    protected static final int EOF = -1;
    
    protected final CompletableFuture<Void> _eof;
    protected WorkerMode _mode;
    
    protected Worker() {
        _eof = new CompletableFuture<Void>();
        _mode = WorkerMode.IDLE;
    }

    /**
     * Returns a future that completes when the worker reaches the end of data.
     * 
     * @return  A future that completes when the worker reaches the end of data.
     */
    public CompletableFuture<Void> getEOF() {
        return _eof;
    }
    
    /*
     * Marks this worker as available for new operations.
     */
    protected void setIdle() {
        _mode = WorkerMode.IDLE;
    }

    /**
     * Completes the EOF future on this worker normally. 
     * Marks this worker as "idle".
     */
    protected void completeEOF() {
        _eof.complete(null);
        
        setIdle();
    }
    
    /**
     * Completes the EOF future on this worker exceptionally.
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this worker as "idle".
     * 
     * @param   ex  An exception to complete with.
     */
    protected void completeEOFExceptionallyAndThrow(Throwable ex) {
        _eof.completeExceptionally(ex);
        
        setIdle();
        
        throw new AsyncException(ex);
    }
    
    /**
     * Ensures the state is good for starting a new operation.
     * 
     * @param   mode    The mode of the operation.
     */
    protected void ensureReadableState(WorkerMode mode) {
        // The reader must be in idle mode.
        if (_mode != WorkerMode.IDLE) {
            throw new IllegalStateException("There is already an operation in progress. Await for the returned CompletableFuture to complete, and then retry.");
        }
        
        // If EOF has already been reached, the caller shouldn't have continued.
        if (_eof.isDone()) {
            throw new IllegalStateException("Attempting to read past the end of data.");
        }
        
        _mode = mode;
    }

    /**
     * Ensures the value for the given argument is not null. Throws if it is.
     * 
     * @param   argName     Name of the argument that is being checked.
     * @param   argValue    Value that is being checked.
     */
    protected static void ensureArgumentNotNull(String argName, Object argValue) {
        if (argValue == null) {
            throw new IllegalArgumentException(String.format("Argument %1$s may not be null.", argName));
        }
    }
    
}

enum WorkerMode {
    IDLE,
    ONCE,
    LOOP
}

