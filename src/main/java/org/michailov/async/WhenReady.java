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
 * This class enables async processing of sync APIs. 
 * For instance, one can process plain old sync InputStream's asynchronously. 
 * <p>
 * Three ways to hook async processing on sync APIs are offered:
 * <ul>
 * <li> {@link #completeAsync completeAsync} - returns a future that gets completed when a predicate reports that a sync API 
 * is ready to be consumed without blocking. The caller can hook any continuation on the returned future. 
 * This is the lowest-level pattern. It offers the highest degree of flexibility.
 *   
 * <li> {@link #applyAsync applyAsync} - executes a sync action asynchronously once a predicate reports that the action is 
 * ready to be called. The returned future gets completed after the action has executed and finished. 
 * This is implemented as a continuation of {@link #completeAsync} that executes the given action.
 *      
 * <li> {@link #startApplyLoopAsync startApplyLoopAsync} - starts executing a sync action asynchronously once a predicate 
 * reports that the action is ready to be called, and continues until another predicate reports that the action should no 
 * longer be called. The returned future gets completed after the loop has finished. 
 * This is implemented as a loop of {@link #completeAsync} and a continuation that executes the given action. 
 * </ul>
 * 
 * @author  Zlatko Michailov
 */
public final class WhenReady {
    
    private static final String TIMEOUT_MESSAGE = "Operation timed out.";

    /**
     * This class offers only static methods. 
     */
    private WhenReady() {
    }

    /**
     * Returns a future that gets completed with a value of <i>result</i> when the <i>ready</i> predicate returns true.
     * <p>
     * This overload uses default {@link AsyncOptions}. 
     * 
     * @param <S>           Type of <i>state</i> passed in to <i>ready</i>.
     * @param <R>           Type of <i>result</i> set on the future upon completion.
     * @param ready         A predicate that should return true when a given sync API is ready to be consumed without blocking.
     * @param result        The value that should be set on the future upon completion.
     * @param state         State that is passed to the predicate in order for it to make the right decision. May be null. 
     * @return              A future that will get completed with a value of <i>result</i> when the <i>ready</i> predicate returns true.
     */
    public static <S, R> CompletableFuture<R> completeAsync(Predicate<S> ready, R result, S state) {
        return completeAsync(ready, result, state, AsyncOptions.DEFAULT);
    }
    
    /**
     * Returns a future that gets completed with a value of <i>result</i> when the <i>ready</i> predicate returns true.
     * 
     * @param <S>           Type of <i>state</i> passed in to <i>ready</i>.
     * @param <R>           Type of <i>result</i> set on the future upon completion.
     * @param ready         A predicate that should return true when a given sync API is ready to be consumed without blocking.
     * @param result        The value that should be set on the future upon completion.
     * @param state         State that is passed to the predicate in order for it to make the right decision. May be null. 
     * @param asyncOptions  {@link AsyncOptions} that control this async call. 
     * @return              A future that will get completed with a value of <i>result</i> when the <i>ready</i> predicate returns true.
     */
    public static <S, R> CompletableFuture<R> completeAsync(Predicate<S> ready, R result, S state, AsyncOptions asyncOptions) {
        Util.ensureArgumentNotNull("ready", ready);
        Util.ensureArgumentNotNull("asyncOptions", asyncOptions);

        CompletableFuture<R> future = new CompletableFuture<R>();
        WhenReadyArguments<S, R> args = new WhenReadyArguments<S, R>(ready, result, state, asyncOptions);
        return completeAsync(future, args);
    }
    
    /**
     * Returns a future that represents the continuation of {@link #completeAsync} that executes
     * <i>action</i>. The returned future gets completed with the result of <i>action</i>. 
     * <p>
     * This overload uses default {@link AsyncOptions}. 
     * 
     * @param <S>           Type of <i>state</i> passed in to <i>ready</i> as well as to <i>action</i>.
     * @param <R>           Type of the result returned from <i>action</i> set on the future upon completion.
     * @param ready         A predicate that should return true when a given sync API is ready to be consumed without blocking.
     * @param action        A function that gets called once <i>ready</i> returns true, and whose result is used to complete the returned future.
     * @param state         State that is passed to <i>ready</i> as well as to <i>action</i>. May be null. 
     * @return              A future that will get completed with the result returned from <i>action</i> when the <i>ready</i> predicate returns true.
     */
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state) {
        return applyAsync(ready, action, state, AsyncOptions.DEFAULT);
    }
    
    /**
     * Returns a future that represents the continuation of {@link #completeAsync} that executes
     * <i>action</i>. The returned future gets completed with the result of <i>action</i>. 
     * 
     * @param <S>           Type of <i>state</i> passed in to <i>ready</i> as well as to <i>action</i>.
     * @param <R>           Type of the result returned from <i>action</i> set on the future upon completion.
     * @param ready         A predicate that should return true when a given sync API is ready to be consumed without blocking.
     * @param action        A function that gets called once <i>ready</i> returns true, and whose result is used to complete the returned future.
     * @param state         State that is passed to <i>ready</i> as well as to <i>action</i>. May be null. 
     * @param asyncOptions  {@link AsyncOptions} that control this async call. 
     * @return              A future that will get completed with the result returned from <i>action</i> when the <i>ready</i> predicate returns true.
     */
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        return startApplyLoopAsync(WhenReadytMode.ONCE, ready, ready, action, state, asyncOptions);
    }
    
    /**
     * Returns a future that represents a loop of continuations of {@link #completeAsync} that execute
     * <i>action</i>. The loop ends when <i>done</i> returns true. 
     * The returned future gets completed with the result from the last invocation of <i>action</i>. 
     * <p>
     * This overload uses default {@link AsyncOptions}. 
     * 
     * @param <S>           Type of <i>state</i> passed in to <i>ready</i> as well as to <i>action</i>.
     * @param <R>           Type of the result returned from <i>action</i> set on the future upon completion.
     * @param ready         A predicate that should return true when a given sync API is ready to be consumed without blocking.
     * @param done          A predicate that should return true when a given sync API should no longer be consumed.
     * @param action        A function that gets called once <i>ready</i> returns true, and whose result is used to complete the returned future.
     * @param state         State that is passed to <i>ready</i> as well as to <i>action</i>. May be null. 
     * @return              A future that will get completed with the result returned from the last execution of <i>action</i>.
     */
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state) {
        return startApplyLoopAsync(ready, done, action, state, AsyncOptions.DEFAULT);
    }
    
    /**
     * Returns a future that represents a loop of continuations of {@link #completeAsync} that execute
     * <i>action</i>. The loop ends when <i>done</i> returns true. 
     * The returned future gets completed with the result from the last invocation of <i>action</i>. 
     * 
     * @param <S>           Type of <i>state</i> passed in to <i>ready</i> as well as to <i>action</i>.
     * @param <R>           Type of the result returned from <i>action</i> set on the future upon completion.
     * @param ready         A predicate that should return true when a given sync API is ready to be consumed without blocking.
     * @param done          A predicate that should return true when a given sync API should no longer be consumed.
     * @param action        A function that gets called once <i>ready</i> returns true, and whose result is used to complete the returned future.
     * @param state         State that is passed to <i>ready</i> as well as to <i>action</i>. May be null. 
     * @param asyncOptions  {@link AsyncOptions} that control this async call. 
     * @return              A future that will get completed with the result returned from the last execution of <i>action</i>.
     */
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        return startApplyLoopAsync(WhenReadytMode.LOOP, ready, done, action, state, asyncOptions);
    }

    /**
     * Private entry point for all {@link #applyAsync} and {@link #startApplyLoop} overloads.
     */
    private static <S, R> CompletableFuture<R> startApplyLoopAsync(WhenReadytMode mode, Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        Util.ensureArgumentNotNull("ready", ready);
        Util.ensureArgumentNotNull("done", done);
        Util.ensureArgumentNotNull("action", action);
        Util.ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        CompletableFuture<R> future = new CompletableFuture<R>();
        WhenReadyArguments<S, R> args = new WhenReadyArguments<S, R>(mode, ready, done, action, state, asyncOptions);
        return startApplyLoopAsync(future, args);
    }
    
    /**
     * Shared implementation of all {@link #applyAsync} and {@link #startApplyLoop} overloads. 
     */
    private static <S, R> CompletableFuture<R> startApplyLoopAsync(CompletableFuture<R> future, WhenReadyArguments<S, R> args) {
        if (mustExit(future, args)) {
            return future;
        }

        CompletableFuture<R> whenReady = new CompletableFuture<R>();
        WhenReady.completeAsync(whenReady, args); 
        whenReady.whenCompleteAsync((a, ex) -> applyLoopAsync(future, args, ex));

        return future;
    }
    
    /**
     * Shared implementation of all {@link #completeAsync} overloads. 
     */
    private static <S, R> CompletableFuture<R> completeAsync(CompletableFuture<R> future, WhenReadyArguments<S, R> args) {
        try {
            if (!mustExit(future, args)) {
                if (args.readyOrDone.test(args.state)) {
                    args.readyRetryCount = 0;
                    completeFutureSafe(future, args.result);
                }
                else {
                    Executor.throttleExecute(() -> completeAsync(future, args), ++args.readyRetryCount);
                }
            }
        }
        catch (Throwable ex) {
            completeFutureExceptionallySafe(future, ex);
        }
        
        return future;
    }
    
    /**
     * Continuation of {@link #completeAsync} for all {@link #applyAsync} and {@link #startApplyLoop} overloads.
     */
    private static<S, R> void applyLoopAsync(CompletableFuture<R> future, WhenReadyArguments<S, R> args, Throwable exception) {
        try {
            if (exception != null) {
                completeFutureExceptionallySafe(future, exception);
            }
            
            if (!mustExit(future, args)) {
                R result = null;
       
                if (args.mode == WhenReadytMode.ONCE) {
                    if (args.ready.test(args.state)) {
                        result = args.action.apply(args.state);
                    }
                }
                else {
                    // Call action() as long as possible to avoid excessive rescheduling. 
                    while (args.ready.test(args.state) && !args.done.test(args.state)) {
                        result = args.action.apply(args.state);
                    }
                }
                
                if (args.done.test(args.state)) {
                    completeFutureSafe(future, result);
                }
                
                if (!mustExit(future, args)) {
                    Executor.execute(() -> startApplyLoopAsync(future, args));
                }
            }
        }
        catch (Throwable ex) {
            completeFutureExceptionallySafe(future, ex);
        }
    }
    
    /**
     * Checks whether an apply loop must exit. 
     */
    private static<S, R> boolean mustExit(CompletableFuture<R> future, WhenReadyArguments<S, R> args) {
        if (future.isDone()) {
            return true;
        }
        
        if (args.timeoutMillis > 0) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > args.startTimeMillis + args.timeoutMillis) {
                completeFutureExceptionallySafe(future, new TimeoutException(TIMEOUT_MESSAGE));
                return true;
            }
        }

        return false;
    }
    
    /**
     * "Safe" methods must not throw. 
     */
    private static<R> void completeFutureSafe(CompletableFuture<R> future, R result) {
        try {
            if (future != null && !future.isDone()) {
                future.complete(result);
            }
        }
        catch (Throwable ex) {
            completeFutureExceptionallySafe(future, ex);
        }
    }
    
    /**
     * "Safe" methods must not throw. 
     */
    private static<R> void completeFutureExceptionallySafe(CompletableFuture<R> future, Throwable exception) {
        try {
            if (future != null && !future.isDone()) {
                future.completeExceptionally(exception);
            }
        }
        catch (Throwable ex) {
            // Swallow this exception - we can't do anything about it.
        }
    }
    
}

