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
        ensureArgumentNotNull("result", result);
        ensureArgumentNotNull("state", state);
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        ExecutionArgs<S, R> args = new ExecutionArgs<S, R>(ready, result, state);
        ExecutionOptions options = new ExecutionOptions(asyncOptions);
        ExecutionState<S, R> exec = new ExecutionState<S, R>(args, options);
        return completeAsync(exec);
    }
    
    private static <S, R> CompletableFuture<R> completeAsync(ExecutionState<S, R> exec) {
        if (mustExit(exec)) {
            return exec.future;
        }

        if (exec.args.ready.test(exec.args.state)) {
            exec.future.complete(exec.args.result);
        }
        else {
            THREAD_POOL.execute(() -> completeAsync(exec));
        }
        
        return exec.future;
    }
    
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state) {
        return applyAsync(ready, action, state, DEFAULT_ASYNC_OPTIONS);
    }
    
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("ready", ready);
        ensureArgumentNotNull("action", action);
        ensureArgumentNotNull("state", state);
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        ExecutionArgs<S, R> args = new ExecutionArgs<S, R>(ready, action, state);
        ExecutionOptions options = new ExecutionOptions(asyncOptions);
        ExecutionState<S, R> exec = new ExecutionState<S, R>(args, options);
        return applyAsync(exec);
    }
    
    private static <S, R> CompletableFuture<R> applyAsync(ExecutionState<S, R> exec) {
        if (mustExit(exec)) {
            return exec.future;
        }
        
        ExecutionArgs<ExecutionState<S, R>, ExecutionState<S, R>> completeArgs = new ExecutionArgs<ExecutionState<S, R>, ExecutionState<S, R>>(e -> readyWrapper(e), exec, exec);
        ExecutionState<ExecutionState<S, R>, ExecutionState<S, R>> completeExec = new ExecutionState<ExecutionState<S, R>, ExecutionState<S, R>>(completeArgs, exec.options);
        CompletableFuture<ExecutionState<S, R>> whenReady = WhenReady.completeAsync(completeExec); 
        whenReady.thenApplyAsync(e -> applyActionWrapper(e));
        setExceptionalCompletion(completeExec.future, exec.future);
        
        return exec.future;
    }
    
    private static<S, R> boolean readyWrapper(ExecutionState<S, R> exec) {
        return exec.args.ready.test(exec.args.state);
    }
    
    private static<S, R> ExecutionState<S, R> applyActionWrapper(ExecutionState<S, R> exec) {
        if (mustExit(exec)) {
            return exec;
        }

        R result = exec.args.action.apply(exec.args.state);
        exec.future.complete(result);
        
        return exec;
    }
    
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state) {
        return startApplyLoopAsync(ready, done, action, state, DEFAULT_ASYNC_OPTIONS);
    }
    
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("ready", ready);
        ensureArgumentNotNull("done", done);
        ensureArgumentNotNull("action", action);
        ensureArgumentNotNull("state", state);
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

        ExecutionArgs<ExecutionState<S, R>, ExecutionState<S, R>> applyArgs = new ExecutionArgs<ExecutionState<S, R>, ExecutionState<S, R>>(e -> readyWrapper(e), e -> loopActionWrapper(e), exec);
        ExecutionState<ExecutionState<S, R>, ExecutionState<S, R>> applyExec = new ExecutionState<ExecutionState<S, R>, ExecutionState<S, R>>(applyArgs, exec.options);
        setExceptionalCompletion(applyExec.future, exec.future);
        WhenReady.applyAsync(applyExec);
        
        return exec.future;
    }
    
    private static<S, R> ExecutionState<S, R> loopActionWrapper(ExecutionState<S, R> exec) {
        if (mustExit(exec)) {
            return exec;
        }

        R result = exec.args.action.apply(exec.args.state);
        
        if (exec.args.done.test(exec.args.state)) {
            exec.future.complete(result);
        }
        else {
            startApplyLoopAsync(exec);
        }
        
        return exec;
    }
    
    private static<S, R> boolean mustExit(ExecutionState<S, R> exec) {
        if (exec.future.isDone()) {
            return true;
        }
        
        if (exec.options.timeoutMillis > 0) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > exec.options.startTimeMillis + exec.options.timeoutMillis) {
                exec.future.completeExceptionally(new TimeoutException("Operation timed out."));
                return true;
            }
        }

        return false;
    }
    
    private static<S, R> void setExceptionalCompletion(CompletableFuture<S> future1, CompletableFuture<R> future2) {
        future1.whenCompleteAsync((s, throwable) -> {
            if (throwable != null && !future2.isDone()) {
                future2.completeExceptionally(throwable);
            }
        });
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

