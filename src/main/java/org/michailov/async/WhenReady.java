package org.michailov.async;

import java.util.concurrent.*;
import java.util.function.*;

public final class WhenReady {
    
    private static final String TIMEOUT_MESSAGE = "Operation timed out.";
    private static final ForkJoinPool THREAD_POOL = ForkJoinPool.commonPool();
    private static final AsyncOptions DEFAULT_ASYNC_OPTIONS = new AsyncOptions();

    public static <S, R> CompletableFuture<R> completeAsync(Predicate<S> ready, R result, S state) {
        return completeAsync(ready, result, state, DEFAULT_ASYNC_OPTIONS);
    }
    
    public static <S, R> CompletableFuture<R> completeAsync(Predicate<S> ready, R result, S state, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("ready", ready);
        ensureArgumentNotNull("asyncOptions", asyncOptions);

        CompletableFuture<R> future = new CompletableFuture<R>();
        WhenReadyArguments<S, R> args = new WhenReadyArguments<S, R>(ready, result, state, asyncOptions);
        return completeAsync(future, args);
    }
    
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state) {
        return applyAsync(ready, action, state, DEFAULT_ASYNC_OPTIONS);
    }
    
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        final Predicate<S> DONE_PERMANENTLY = s -> true;
        return startApplyLoopAsync(ready, DONE_PERMANENTLY, action, state, asyncOptions);
    }
    
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state) {
        return startApplyLoopAsync(ready, done, action, state, DEFAULT_ASYNC_OPTIONS);
    }
    
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("ready", ready);
        ensureArgumentNotNull("done", done);
        ensureArgumentNotNull("action", action);
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        CompletableFuture<R> future = new CompletableFuture<R>();
        WhenReadyArguments<S, R> args = new WhenReadyArguments<S, R>(ready, done, action, state, asyncOptions);
        return startApplyLoopAsync(future, args);
    }
    
    private static <S, R> CompletableFuture<R> startApplyLoopAsync(CompletableFuture<R> future, WhenReadyArguments<S, R> args) {
        if (mustExit(future, args)) {
            return future;
        }

        CompletableFuture<R> whenReady = new CompletableFuture<R>();
        WhenReady.completeAsync(whenReady, args); 
        whenReady.whenCompleteAsync((a, ex) -> applyLoopAsync(future, args, ex));

        return future;
    }
    
    private static <S, R> CompletableFuture<R> completeAsync(CompletableFuture<R> future, WhenReadyArguments<S, R> args) {
        try {
            if (!mustExit(future, args)) {
                if (args.ready.test(args.state)) {
                    completeFutureSafe(future, args.result);
                }
                else {
                    THREAD_POOL.execute(() -> completeAsync(future, args));
                }
            }
        }
        catch (Throwable ex) {
            completeFutureExceptionallySafe(future, ex);
        }
        
        return future;
    }
    
    private static<S, R> void applyLoopAsync(CompletableFuture<R> future, WhenReadyArguments<S, R> args, Throwable exception) {
        try {
            if (exception != null) {
                completeFutureExceptionallySafe(future, exception);
            }
            
            if (!mustExit(future, args)) {
                R result = args.action.apply(args.state);
                
                if (args.done.test(args.state)) {
                    completeFutureSafe(future, result);
                }
                
                if (!mustExit(future, args)) {
                    THREAD_POOL.execute(() -> startApplyLoopAsync(future, args));
                }
            }
        }
        catch (Throwable ex) {
            completeFutureExceptionallySafe(future, ex);
        }
    }
    
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
    
    /**
     * Helper that checks an argument for null.
     */
    private static void ensureArgumentNotNull(String argName, Object argValue) {
        if (argValue == null) {
            throw new IllegalArgumentException(String.format("Argument %1$s may not be null.", argName));
        }
    }

}

