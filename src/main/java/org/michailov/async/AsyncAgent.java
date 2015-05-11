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
    private volatile WhenReadyMode _mode;
    
    /**
     * Base constructor for derived classes to call. 
     * 
     * @param   asyncOptions    {@link AsyncOptions} that will control all async operations on this agent. 
     */
    protected AsyncAgent(AsyncOptions asyncOptions) {
        Util.ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        this._asyncOptions = asyncOptions;
        this._mode = WhenReadyMode.IDLE;
    }
    
    /**
     * Starts a new {@link WhenReady#applyAsync} operation.
     * 
     * @return      A future that will get completed with the result returned from {@link #action} when the {@link #ready} predicate returns true.
     */
    public CompletableFuture<Void> applyAsync() {
        ensureMode(WhenReadyMode.ONCE);
        return WhenReady.applyAsync(READY, ACTION, this, _asyncOptions);
    }
    
    /**
     * Starts a new {@link WhenReady#startApplyLoopAsync} operation.
     * 
     * @return      A future that will get completed with the result returned from the last execution of {@link #action}.
     */
    public CompletableFuture<Void> startApplyLoopAsync() {
        ensureMode(WhenReadyMode.LOOP);
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
        if (_mode == WhenReadyMode.ONCE) {
            setIdle();
        }
        
        return null;
    }
    
    /**
     * Checks whether this AsyncAgent is idle, i.e. whether it can accept requests for async operations.
     * 
     * @return      true iff this AsyncAgent is idle. 
     */
    public boolean isIdle() {
        return _mode == WhenReadyMode.IDLE;
    }
    
    /**
     * Marks this agent as 'idle'.
     */
    protected void setIdle() {
        _mode = WhenReadyMode.IDLE;
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
    private void ensureMode(WhenReadyMode mode) {
        // The agent must be in idle mode.
        if (_mode != WhenReadyMode.IDLE) {
            throw new IllegalStateException("There is already an operation in progress. Await for the returned CompletableFuture to complete, and then retry.");
        }
        
        _mode = mode;
    }

}

