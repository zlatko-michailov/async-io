package org.michailov.async;

import java.util.concurrent.*;
import java.util.function.*;

/**
 * Abstract functionality that encapsulates context in which {@link WhenReady} patterns are called.
 * This is a convenience base class for classes that perform async operations.
 * 
 * @see     WhenReady
 * 
 * @author  Zlatko Michailov
 */
public abstract class AsyncAgent {

    private static final Predicate<AsyncAgent> READY = agent -> agent.ready();
    private static final Predicate<AsyncAgent> DONE = agent -> agent.done();
    private static final Function<AsyncAgent, Void> ACTION = agent -> agent.actionWrapper();
    
    private final AsyncOptions _asyncOptions;
    private volatile WhenReadytMode _mode;
    
    /**
     * Base constructor for derived classes to call. 
     * 
     * @param   asyncOptions    {@link AsyncOptions} that will control all async operations on this agent. 
     */
    protected AsyncAgent(AsyncOptions asyncOptions) {
        Util.ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        this._asyncOptions = asyncOptions;
        this._mode = WhenReadytMode.IDLE;
    }
    
    /**
     * Starts a new {@link WhenReady#applyAsync} operation.
     * 
     * @return      A future that will get completed with the result returned from {@link #action} when the {@link #ready} predicate returns true.
     */
    public CompletableFuture<Void> applyAsync() {
        ensureMode(WhenReadytMode.ONCE);
        return WhenReady.applyAsync(READY, ACTION, this, _asyncOptions);
    }
    
    /**
     * Starts a new {@link WhenReady#startApplyLoopAsync} operation.
     * 
     * @return      A future that will get completed with the result returned from the last execution of {@link #action}.
     */
    public CompletableFuture<Void> startApplyLoopAsync() {
        ensureMode(WhenReadytMode.LOOP);
        return WhenReady.startApplyLoopAsync(READY, DONE, ACTION, this, _asyncOptions);
    }
    
    /**
     * The '<i>ready</i>' predicate passed to {@link WhenReady#completeAsync}, {@link WhenReady#applyAsync}, or {@link WhenReady#startApplyLoopAsync}.
     * 
     * @return      Derived classes should override this method to return true iff it is time to execute {@link #action}.
     */
    protected boolean ready() {
        return false;
    }
    
    /**
     * The '<i>done</i>' predicate passed to {@link WhenReady#startApplyLoopAsync}.
     * 
     * @return      Derived classes should override this method to return true iff it is time for the loop to stop.
     */
    protected boolean done() {
        return true;
    }
    
    /**
     * The '<i>action</i>' function passed to {@link WhenReady#applyAsync} or {@link WhenReady#startApplyLoopAsync}.
     * <p>
     * Derived classes should override this method.
     */
    protected void action() {
    }
    
    private Void actionWrapper() {
        action();
        
        // If this was a one-time operation, become idle.
        if (_mode == WhenReadytMode.ONCE) {
            setIdle();
        }
        
        return null;
    }
    
    /**
     * Marks this agent as 'idle'.
     */
    protected void setIdle() {
        _mode = WhenReadytMode.IDLE;
    }

    /**
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this agent as 'idle'.
     * 
     * @param   ex  An exception to complete with.
     */
    protected void setIdleAndThrow(Throwable ex) {
        setIdle();
        throw new AsyncException(ex);
    }
    
    /**
     * Ensures the current mode is good for starting a new operation.
     * 
     * @param   mode    The mode of the operation.
     */
    private void ensureMode(WhenReadytMode mode) {
        // The agent must be in idle mode.
        if (_mode != WhenReadytMode.IDLE) {
            throw new IllegalStateException("There is already an operation in progress. Await for the returned CompletableFuture to complete, and then retry.");
        }
        
        _mode = mode;
    }

}

