package org.michailov.async;

import java.util.function.*;

final class WhenReadyArguments<S, R> {

    final WhenReadytMode mode;
    final Predicate<S> ready;
    final Predicate<S> readyOrDone;
    final Predicate<S> done;
    final Function<S, R> action;
    final R result;
    final S state;
    final AsyncOptions asyncOptions;
    final long startTimeMillis;
    final long timeoutMillis;
    int readyTestCount;
    
    WhenReadyArguments(Predicate<S> ready, R result, S state, AsyncOptions asyncOptions) {
        this(WhenReadytMode.ONCE, ready, null, null, result, state, asyncOptions);
    }
    
    WhenReadyArguments(WhenReadytMode mode, Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        this(mode, ready, done, action, null, state, asyncOptions);
    }
    
    private WhenReadyArguments(WhenReadytMode mode, Predicate<S> ready, Predicate<S> done, Function<S, R> action, R result, S state, AsyncOptions asyncOptions) {
        Predicate<S> readyOrDone;
       
        if (done != null && done != ready) {
            readyOrDone = ready.or(done);
        }
        else {
            readyOrDone = ready;
            done = s -> true;
        }

        this.mode = mode;
        this.ready = ready;
        this.readyOrDone = readyOrDone;
        this.done = done;
        this.action = action;
        this.result = result;
        this.state = state;
        this.asyncOptions = asyncOptions;
        this.startTimeMillis = System.currentTimeMillis();
        this.timeoutMillis = asyncOptions.timeout >= 0 ? asyncOptions.timeUnit.toMillis(asyncOptions.timeout) : AsyncOptions.TIMEOUT_INFINITE;
        this.readyTestCount = 0;
    }
}


enum WhenReadytMode {
    IDLE,
    ONCE,
    LOOP
}
