package org.michailov.async;

import java.util.concurrent.*;
import java.util.function.*;

public final class WhenReady {
    private static final ForkJoinPool THREAD_POOL = ForkJoinPool.commonPool();

    // TODO: Add AsyncOptions.
    // TODO: Check arguments.
    public static <S, R> CompletableFuture<R> completeAsync(Predicate<S> ready, R result, S state) {
        return completeAsync(null, ready, result, state);
    }
    
    // TODO: Add AsyncOptions and check for timeout.
    private static <S, R> CompletableFuture<R> completeAsync(CompletableFuture<R> taskOrNull, Predicate<S> ready, R result, S state) {
        // Ensure a task.
        CompletableFuture<R> task = taskOrNull != null ? taskOrNull : new CompletableFuture<R>();
        
        // If the task is complete, quietly bail out.
        if (task.isDone()) {
            return task;
        }

        if (ready.test(state)) {
            // The predicate is ready.
            // Complete sync.
            task.complete(result);
        }
        else {
            // The predicate is not ready.
            // Schedule self to try again.
            THREAD_POOL.execute(() -> completeAsync(task, ready, result, state));
        }
        
        return task;
    }
    
    // TODO: Add AsyncOptions and check for timeout.
    // TODO: Check arguments.
    public static <S, R> CompletableFuture<R> applyAsync(Predicate<S> ready, Function<S, R> action, S state) {
        return applyAsync(null, ready, action, state);
    }
    
    // TODO: Add AsyncOptions and check for timeout.
    // TODO: Check arguments.
    private static <S, R> CompletableFuture<R> applyAsync(CompletableFuture<R> taskOrNull, Predicate<S> ready, Function<S, R> action, S state) {
        // Ensure a task.
        CompletableFuture<R> task = taskOrNull != null ? taskOrNull : new CompletableFuture<R>();
        
        // If the task is complete, quietly bail out.
        if (task.isDone()) {
            return task;
        }
        
        // Build a continuation chain.
        CompletableFuture<S> whenReady = completeAsync(ready, null, state); 
        whenReady.thenApplyAsync((S s) -> {
            // If the task is complete, quietly bail out.
            if (task.isDone()) {
                return null;
            }

            // Execute the action and return the result.
            return action.apply(s);
        });
        
        // Return the task that represents the whole operation.
        return task;
    }
    
    // TODO: Add AsyncOptions.
    // TODO: Check arguments.
    public static <S, R> CompletableFuture<R> startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state) {
        return startApplyLoopAsync(null, ready, done, action, state);
    }
    
    // TODO: Add AsyncOptions and check for timeout.
    private static <S, R> CompletableFuture<R> startApplyLoopAsync(CompletableFuture<R> taskOrNull, Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state) {
        // Ensure a task.
        CompletableFuture<R> task = taskOrNull != null ? taskOrNull : new CompletableFuture<R>();
        
        // If the task is complete, quietly bail out.
        if (task.isDone()) {
            return task;
        }

        // When the predicate is ready again -
        applyAsync(ready, (S s) -> {
            // If the task is complete, quietly bail out.
            if (task.isDone()) {
                return null;
            }

            // Execute the action and keep the result.
            R result = action.apply(s);
            
            if (!done.test(s)) {
                // Schedule next iteration.
                startApplyLoopAsync(task, ready, done, action, state);
            }
            
            // Return the result of the action.
            return result;
        }, state);
        
        // Return the task that represents the whole operation.
        return task;
    }
}
