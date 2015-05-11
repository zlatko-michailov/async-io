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

import java.util.function.*;

final class WhenReadyArguments<S, R> {

    final WhenReadyMode mode;
    final Predicate<S> ready;
    final Predicate<S> readyOrDone;
    final Predicate<S> done;
    final Function<S, R> action;
    final R result;
    final S state;
    final AsyncOptions asyncOptions;
    final long startTimeMillis;
    final long timeoutMillis;
    int readyRetryCount;
    
    WhenReadyArguments(Predicate<S> ready, R result, S state, AsyncOptions asyncOptions) {
        this(WhenReadyMode.ONCE, ready, null, null, result, state, asyncOptions);
    }
    
    WhenReadyArguments(WhenReadyMode mode, Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state, AsyncOptions asyncOptions) {
        this(mode, ready, done, action, null, state, asyncOptions);
    }
    
    private WhenReadyArguments(WhenReadyMode mode, Predicate<S> ready, Predicate<S> done, Function<S, R> action, R result, S state, AsyncOptions asyncOptions) {
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
        this.readyRetryCount = 0;
    }
}


enum WhenReadyMode {
    IDLE,
    ONCE,
    LOOP
}
