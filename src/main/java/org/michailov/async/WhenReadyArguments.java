package org.michailov.async;

import java.util.function.*;

final class WhenReadyArguments<S, R> {
    
    final Predicate<S> ready;
    final Predicate<S> done;
    final Function<S, R> action;
    final R result;
    final S state;
    final AsyncOptions asyncOptions;
    final long startTimeMillis;
    final long timeoutMillis;
    
    WhenReadyArguments(Predicate<S> ready, R result, S state, AsyncOptions asyncOptions) {
        this(ready, null, null, result, state, asyncOptions);
    }
    
    WhenReadyArguments(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        this(ready, done, action, null, state, asyncOptions);
    }
    
    private WhenReadyArguments(Predicate<S> ready, Predicate<S> done, Function<S, R> action, R result, S state, AsyncOptions asyncOptions) {
        this.ready = ready;
        this.done = done;
        this.action = action;
        this.result = result;
        this.state = state;
        this.asyncOptions = asyncOptions;
        this.startTimeMillis = System.currentTimeMillis();
        this.timeoutMillis = asyncOptions.timeout >= 0 ? asyncOptions.timeUnit.toMillis(asyncOptions.timeout) : AsyncOptions.TIMEOUT_INFINITE;
    }
}

