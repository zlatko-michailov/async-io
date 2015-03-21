package org.michailov.async;

import java.util.concurrent.*;
import java.util.function.*;

public abstract class AsyncAgent {

    private static final Predicate<AsyncAgent> READY = agent -> agent.ready();
    private static final Predicate<AsyncAgent> DONE = agent -> agent.done();
    private static final Function<AsyncAgent, Void> APPLY = agent -> agent.applyWrapper();
    
    protected final AsyncOptions _asyncOptions;
    protected AgentMode _mode;
    
    protected AsyncAgent(AsyncOptions asyncOptions) {
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        this._asyncOptions = asyncOptions;
        this._mode = AgentMode.IDLE;
    }
    
    public CompletableFuture<Void> applyAsync() {
        ensureMode(AgentMode.ONCE);
        return WhenReady.applyAsync(READY, APPLY, this, _asyncOptions);
    }
    
    public CompletableFuture<Void> startApplyLoopAsync() {
        ensureMode(AgentMode.LOOP);
        return WhenReady.startApplyLoopAsync(READY, DONE, APPLY, this, _asyncOptions);
    }
    
    protected boolean ready() {
        return false;
    }
    
    protected boolean done() {
        return true;
    }
    
    protected void apply() {
    }
    
    private Void applyWrapper() {
        apply();
        
        // If this was a one-time operation, become idle.
        if (_mode == AgentMode.ONCE) {
            setIdle();
        }
        
        return null;
    }
    
    /*
     * Marks this agent as "idle".
     */
    protected void setIdle() {
        _mode = AgentMode.IDLE;
    }

    /**
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this agent as "idle".
     * 
     * @param   ex  An exception to complete with.
     */
    protected void setIdleAndThrow(Throwable ex) {
        setIdle();
        throw new AsyncException(ex);
    }
    
    /**
     * Ensures the mode is good for starting a new operation.
     * 
     * @param   mode    The mode of the operation.
     */
    protected void ensureMode(AgentMode mode) {
        // The agent must be in idle mode.
        if (_mode != AgentMode.IDLE) {
            throw new IllegalStateException("There is already an operation in progress. Await for the returned CompletableFuture to complete, and then retry.");
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


enum AgentMode {
    IDLE,
    ONCE,
    LOOP
}
