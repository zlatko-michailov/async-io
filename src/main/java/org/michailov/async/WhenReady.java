package org.michailov.async;

import java.util.concurrent.*;
import java.util.function.*;

public final class WhenReady {
    
    private static final ForkJoinPool THREAD_POOL = ForkJoinPool.commonPool();
    private static final AsyncOptions DEFAULT_ASYNC_OPTIONS = new AsyncOptions();

    public static <S, R> CompletableFuture<R> completeAsync(Predicate<S> ready, R result, S state) {
        return completeAsync(ready, result, state, DEFAULT_ASYNC_OPTIONS);
    }
    
    public static <S, R> CompletableFuture<R> completeAsync(Predicate<S> ready, R result, S state, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("ready", ready);
        //ensureArgumentNotNull("result", result);
        //ensureArgumentNotNull("state", state);
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        ExecutionArgs<S, R> args = new ExecutionArgs<S, R>(ready, result, state);
        ExecutionOptions options = new ExecutionOptions(asyncOptions);
        ExecutionState<S, R> exec = new ExecutionState<S, R>(args, options);
        return completeAsync(exec);
    }
    
    private static <S, R> CompletableFuture<R> completeAsync(ExecutionState<S, R> exec) {
        try {
            if (!mustExit(exec)) {
                if (readyWrapper(exec)) {
                    completeFutureSafe(exec.future, exec.args.result);
                }
                else {
                    THREAD_POOL.submit(() -> completeAsync(exec));
                }
            }
        }
        catch (Throwable ex) {
            completeFutureExceptionallySafe(exec.future, ex);
        }
        
        return exec.future;
    }
    
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state) {
        return applyAsync(ready, action, state, DEFAULT_ASYNC_OPTIONS);
    }
    
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("ready", ready);
        ensureArgumentNotNull("action", action);
        //ensureArgumentNotNull("state", state);
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        ExecutionArgs<S, R> args = new ExecutionArgs<S, R>(ready, s -> true, action, state);
        ExecutionOptions options = new ExecutionOptions(asyncOptions);
        ExecutionState<S, R> exec = new ExecutionState<S, R>(args, options);
        
        // TODO: Merge applyLoop and startApplyLoop
        return startApplyLoopAsync(exec);
    }
    
    private static <S, R> CompletableFuture<R> applyAsync(ExecutionState<S, R> exec) {
        if (mustExit(exec)) {
            return exec.future;
        }
        
        ExecutionArgs<ExecutionState<S, R>, ExecutionState<S, R>> completeArgs = 
                new ExecutionArgs<ExecutionState<S, R>, ExecutionState<S, R>>(
                        //e -> readyWrapper(e), exec, exec);
                        e -> readyWrapper(e), e -> doneWrapper(e), e -> actionWrapper(e), exec, exec);
        ExecutionState<ExecutionState<S, R>, ExecutionState<S, R>> completeExec = 
                new ExecutionState<ExecutionState<S, R>, ExecutionState<S, R>>(
                        completeArgs, exec.options);
        
        CompletableFuture<ExecutionState<S, R>> whenReady = WhenReady.completeAsync(completeExec); 
        whenReady.whenCompleteAsync((e, ex) -> applyActionWrapper(e, ex));
        
        return exec.future;
    }
    
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state) {
        return startApplyLoopAsync(ready, done, action, state, DEFAULT_ASYNC_OPTIONS);
    }
    
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("ready", ready);
        ensureArgumentNotNull("done", done);
        ensureArgumentNotNull("action", action);
        //ensureArgumentNotNull("state", state);
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        ExecutionArgs<S, R> args = new ExecutionArgs<S, R>(ready, done, action, state);
        ExecutionOptions options = new ExecutionOptions(asyncOptions);
        ExecutionState<S, R> exec = new ExecutionState<S, R>(args, options);
        
        return startApplyLoopAsync(exec);
    }
    
    private static <S, R> CompletableFuture<R> startApplyLoopAsync(ExecutionState<S, R> exec) {
        if (mustExit(exec)) {
            return exec.future;
        }

        /*ExecutionArgs<S, R> args2 = new ExecutionArgs<S, R>(
                        //e -> readyWrapper(e), e -> doneWrapper(e), e -> loopActionWrapper(e, null), exec);
                        e -> readyWrapper(e), e -> doneWrapper(e), e -> actionWrapper(e), exec, exec);
        ExecutionState<ExecutionState<S, R>, ExecutionState<S, R>> applyExec = 
                new ExecutionState<ExecutionState<S, R>, ExecutionState<S, R>>(
                        applyArgs, exec.options);*/
        
        ExecutionState<S, R> exec2 = new ExecutionState<S, R>(exec.args, exec.options);
        CompletableFuture<R> whenReady = WhenReady.completeAsync(exec2); 
        whenReady.whenCompleteAsync((e, ex) -> loopActionWrapper(exec, ex));

        return exec.future;
    }
    
    private static<S, R> boolean readyWrapper(ExecutionState<S, R> exec) {
        boolean isReady = exec.args.ready.test(exec.args.state);
        return isReady;
    }
    
    private static<S, R> boolean doneWrapper(ExecutionState<S, R> exec) {
        boolean isDone = exec.args.done.test(exec.args.state);
        return isDone;
    }
    
    private static<S, R> ExecutionState<S, R> actionWrapper(ExecutionState<S, R> exec) {
        R result = exec.args.action.apply(exec.args.state);
        
        if (doneWrapper(exec)) {
            completeFutureSafe(exec.future, result);
        }
        
        return exec;
    }
    
    private static<S, R> ExecutionState<S, R> applyActionWrapper(ExecutionState<S, R> exec, Throwable exception) {
        try {
            if (exception != null) {
                completeFutureExceptionallySafe(exec.future, exception);
                return exec;
            }
    
            if (!mustExit(exec)) {
                actionWrapper(exec);
            }
        }
        catch (Throwable ex) {
            completeFutureExceptionallySafe(exec.future, ex);
        }
        
        return exec;
    }
    
    private static<S, R> void loopActionWrapper(ExecutionState<S, R> exec, Throwable exception) {
        try {
            if (exception != null) {
                completeFutureExceptionallySafe(exec.future, exception);
            }
            
            if (!mustExit(exec)) {
                actionWrapper(exec);
                
                if (!mustExit(exec)) {
                    THREAD_POOL.submit(() -> startApplyLoopAsync(exec));
                }
            }
        }
        catch (Throwable ex) {
            completeFutureExceptionallySafe(exec.future, ex);
        }
    }
    
    private static<S, R> boolean mustExit(ExecutionState<S, R> exec) {
        if (exec.future.isDone()) {
            return true;
        }
        
        if (exec.options.timeoutMillis > 0) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > exec.options.startTimeMillis + exec.options.timeoutMillis) {
                completeFutureExceptionallySafe(exec.future, new TimeoutException("Operation timed out."));
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

