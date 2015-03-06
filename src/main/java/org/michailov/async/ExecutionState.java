package org.michailov.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

final class ExecutionState<S, R> {
    
    CompletableFuture<R> future;
    final ExecutionArgs<S, R> args;
    final ExecutionOptions options;
    
    ExecutionState(ExecutionArgs<S, R> args, ExecutionOptions options) {
        this.future = new CompletableFuture<R>();
        this.args = args;
        this.options = options;
    }
}


final class ExecutionOptions {
    
    final long startTimeMillis;
    final long timeoutMillis;
    
    ExecutionOptions(AsyncOptions asyncOptions) {
        this.startTimeMillis = System.currentTimeMillis();
        this.timeoutMillis = asyncOptions.timeout >= 0 ? asyncOptions.timeUnit.toMillis(asyncOptions.timeout) : AsyncOptions.TIMEOUT_INFINITE;
    }
}


final class ExecutionArgs<S, R> {
    
    final Predicate<S> ready;
    final Predicate<S> done;
    final Function<S, R> action;
    final R result;
    final S state;
    
    // Constructors for new operations.
    ExecutionArgs(Predicate<S> ready, R result, S state) {
        this(ready, null, null, result, state);
    }
    
    ExecutionArgs (Predicate<S> ready, Function<S, R> action, S state) {
        this(ready, null, action, null, state);
    }
    
    ExecutionArgs(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state) {
        this(ready, done, action, null, state);
    }
    
    private ExecutionArgs(Predicate<S> ready, Predicate<S> done, Function<S, R> action, R result, S state) {
        this.ready = ready;
        this.done = done;
        this.action = action;
        this.result = result;
        this.state = state;
    }
    
}
