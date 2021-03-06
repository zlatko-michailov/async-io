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

package org.michailov.async.test;

import java.util.concurrent.*;
import java.util.function.*;
import org.junit.*;
import org.michailov.async.*;

public class WhenReadyTest {

    @Test
    public void testCompleteAsync() throws Throwable {
        final int READY_AFTER_COUNT = 5;
        final int DONE_AFTER_COUNT = -1;
        final long ACTION_MILLIS = 0;
        final long TIMEOUT_MILLIS = AsyncOptions.TIMEOUT_INFINITE;
        
        System.out.println("\ntestCompleteAsync {");

        testWhenReadyMethod(WhenReadyMethod.COMPLETE_ASYNC, READY_AFTER_COUNT, DONE_AFTER_COUNT, ACTION_MILLIS, TIMEOUT_MILLIS);
        
        System.out.println("} // testCompleteAsync");
    }

    @Test (expected = TimeoutException.class)
    public void testCompleteAsyncTimeout() throws Throwable {
        final int READY_AFTER_COUNT = 5;
        final int DONE_AFTER_COUNT = -1;
        final long ACTION_MILLIS = 50;
        final long TIMEOUT_MILLIS = 100;
        
        System.out.println("\ntestCompleteAsyncTimeout {");

        testWhenReadyMethod(WhenReadyMethod.COMPLETE_ASYNC, READY_AFTER_COUNT, DONE_AFTER_COUNT, ACTION_MILLIS, TIMEOUT_MILLIS);
        
        System.out.println("} // testCompleteAsyncTimeout");
    }

    @Test
    public void testApplyAsync() throws Throwable {
        final int READY_AFTER_COUNT = 7;
        final int DONE_AFTER_COUNT = -1;
        final long ACTION_MILLIS = 10;
        final long TIMEOUT_MILLIS = AsyncOptions.TIMEOUT_INFINITE;
        
        System.out.println("\ntestApplyAsync {");

        testWhenReadyMethod(WhenReadyMethod.APPLY_ASYNC, READY_AFTER_COUNT, DONE_AFTER_COUNT, ACTION_MILLIS, TIMEOUT_MILLIS);
        
        System.out.println("} // testApplyAsync");
    }

    @Test (expected = TimeoutException.class)
    public void testApplyAsyncTimeout() throws Throwable {
        final int READY_AFTER_COUNT = 7;
        final int DONE_AFTER_COUNT = -1;
        final long ACTION_MILLIS = 50;
        final long TIMEOUT_MILLIS = 100;
        
        System.out.println("\ntestApplyAsyncTimeout {");

        testWhenReadyMethod(WhenReadyMethod.APPLY_ASYNC, READY_AFTER_COUNT, DONE_AFTER_COUNT, ACTION_MILLIS, TIMEOUT_MILLIS);
        
        System.out.println("} // testApplyAsyncTimeout");
    }

    @Test
    public void testStartApplyLoopAsync() throws Throwable {
        final int READY_AFTER_COUNT = 3;
        final int DONE_AFTER_COUNT = 5;
        final long ACTION_MILLIS = 10;
        final long TIMEOUT_MILLIS = AsyncOptions.TIMEOUT_INFINITE;
        
        System.out.println("\ntestStartApplyLoopAsync {");

        testWhenReadyMethod(WhenReadyMethod.START_APPLY_LOOP_ASYNC, READY_AFTER_COUNT, DONE_AFTER_COUNT, ACTION_MILLIS, TIMEOUT_MILLIS);
        
        System.out.println("} // testStartApplyLoopAsync");
    }

    @Test (expected = TimeoutException.class)
    public void testStartApplyLoopAsyncTimeout() throws Throwable {
        final int READY_AFTER_COUNT = 4;
        final int DONE_AFTER_COUNT = 5;
        final long ACTION_MILLIS = 20;
        final long TIMEOUT_MILLIS = 100;
        
        System.out.println("\ntestStartApplyLoopAsyncTimeout {");

        testWhenReadyMethod(WhenReadyMethod.START_APPLY_LOOP_ASYNC, READY_AFTER_COUNT, DONE_AFTER_COUNT, ACTION_MILLIS, TIMEOUT_MILLIS);
        
        System.out.println("} // testStartApplyLoopAsyncTimeout");
    }

    private void testWhenReadyMethod(WhenReadyMethod whenReadyMethod, int readyAfterCount, int doneAfterCount, long actionMillis, long timeoutMillis) throws Throwable {
        WhenReadySimulator simulator = new WhenReadySimulator(readyAfterCount, doneAfterCount, actionMillis);
        
        AsyncOptions asyncOptions = new AsyncOptions();
        asyncOptions.timeout = timeoutMillis;
        asyncOptions.timeUnit = TimeUnit.MILLISECONDS;
        
        Predicate<WhenReadySimulator> ready = s -> WhenReadySimulator.ready(s);
        Predicate<WhenReadySimulator> done = s -> WhenReadySimulator.done(s);
        Function<WhenReadySimulator, Integer> action = s -> WhenReadySimulator.action(s);
        
        CompletableFuture<Integer> future = null;
        int expectedResult = -1;
        int expectedActionCount = -1;
        int expectedReadyCount = -1;
        
        switch (whenReadyMethod) {
        case COMPLETE_ASYNC:
            future = WhenReady.completeAsync(ready, Integer.valueOf(readyAfterCount), simulator, asyncOptions);
            expectedResult = readyAfterCount;
            expectedActionCount = 0;
            expectedReadyCount = readyAfterCount;
            break;
        case APPLY_ASYNC:
            future = WhenReady.applyAsync(ready, action, simulator, asyncOptions);
            expectedResult = 1;
            expectedActionCount = 1;
            expectedReadyCount = readyAfterCount + expectedActionCount;
            break;
        case START_APPLY_LOOP_ASYNC:
            future = WhenReady.startApplyLoopAsync(ready, done, action, simulator, asyncOptions);
            expectedResult = doneAfterCount;
            expectedActionCount = doneAfterCount;
            expectedReadyCount = readyAfterCount + expectedActionCount + 1;
            break;
        }
        
        int result = -1;
        try {
            Integer futureResult = future.get();
            Assert.assertNotEquals(futureResult, null);
            
            if (futureResult != null) {
                result = futureResult.intValue();
            }
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Throwable cause = ex.getCause();
            if (cause != null) {
                throw cause;
            }
        }
        
        System.out.println(String.format("readyAfterCount=%1$d, doneAfterCount=%2$d, result=%3$d, simulator.readyCount=%4$d, simulator.actionCount=%5$d", 
                                          readyAfterCount, doneAfterCount, result, simulator.getReadyCount(), simulator.getActionCount()));
        Assert.assertEquals(expectedResult, result);
        Assert.assertEquals(expectedReadyCount, simulator.getReadyCount());
        Assert.assertEquals(expectedActionCount, simulator.getActionCount());
    }

    enum WhenReadyMethod {
        COMPLETE_ASYNC,
        APPLY_ASYNC,
        START_APPLY_LOOP_ASYNC
    }
}
