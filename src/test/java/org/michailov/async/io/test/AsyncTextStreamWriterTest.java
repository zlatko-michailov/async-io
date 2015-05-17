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

package org.michailov.async.io.test;

import java.util.concurrent.*;
import java.util.function.*;
import org.junit.*;
import org.michailov.async.*;
import org.michailov.async.io.*;

public class AsyncTextStreamWriterTest {
    
    @Test
    public void testApplyAsync() {
        System.out.println("\ntestApplyAsync {");

        testAsyncAgentMethod(AsyncAgentMethod.APPLY_ASYNC);
        
        System.out.println("} // testApplyAsync");
    }
    
    @Test
    public void teststartApplyLoopAsync() {
        System.out.println("\ntestStartApplyLoopAsync {");
        
        testAsyncAgentMethod(AsyncAgentMethod.START_APPLY_LOOP_ASYNC);
        
        System.out.println("} // testStartApplyLoopAsync");
    }

    public void testAsyncAgentMethod(AsyncAgentMethod method) {
        final int STREAM_LENGTH = 1000; // Just big enough.
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int LINE_COUNT = 14;

        // Prepare state.
        TestState state = new TestState(); 
        state.lines = InputStreamSimulator.LINES;
        state.options = new TextStreamAsyncOptions();
        state.options.charset = InputStreamSimulator.CHARSET;
        OutputStreamSimulator simulator = new OutputStreamSimulator(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        state.writer = new AsyncTextStreamWriter(simulator, state.options);
        state.streamBytes = simulator.getContentBytes();
        
        // Start input loop.
        Predicate<TestState> ready = st -> st.stringsPosition < LINE_COUNT && st.writer.getStringRingBuffer().getAvailableToWrite() > 0;
        Predicate<TestState> done = st -> st.stringsPosition == LINE_COUNT;
        Function<TestState, Void> action = st -> { st.writer.getStringRingBuffer().write(st.lines[st.stringsPosition++]); return null; };
        state.inputFuture = WhenReady.startApplyLoopAsync(ready, done, action, state)
                                .whenCompleteAsync((nl, ex) -> state.writer.getStringRingBuffer().setEOF());
        
        switch (method) {
        case APPLY_ASYNC:
            state.testFuture = new CompletableFuture<Void>();
            state.writer.applyAsync()
                            .thenApplyAsync(nl -> 
                                WhenReady.completeAsync(st -> st.writer.isEmpty(), null, state))
                                    .thenApplyAsync(nl2 -> continueApply(state));
            break;
            
        case START_APPLY_LOOP_ASYNC:
            state.testFuture = state.writer.startApplyLoopAsync();
            break;
        }
        
        try {
            // Wait for the input and test futures to complete.
            state.inputFuture.get();
            state.testFuture.get();
            
            // Verify the written bytes.
            int streamPosition = 0;
            for (int i = 0; i < LINE_COUNT; i++) {
                byte[] bytesx = state.lines[i].getBytes(state.options.charset);
                for (int j = 0; j < bytesx.length; j++) {
                    int bx = bytesx[j];
                    int ba = state.streamBytes[streamPosition];
                    System.out.println(String.format("[%1$d] %2$d = %3$d", streamPosition, bx, ba));
                    Assert.assertEquals(bx, ba);
                    streamPosition++;
                }
                
                int bx = '\r';
                int ba = state.streamBytes[streamPosition];
                System.out.println(String.format("[%1$d] %2$d = %3$d", streamPosition, bx, ba));
                Assert.assertEquals(bx, ba);
                streamPosition++;
                
                bx = '\n';
                ba = state.streamBytes[streamPosition];
                System.out.println(String.format("[%1$d] %2$d = %3$d", streamPosition, bx, ba));
                Assert.assertEquals(bx, ba);
                streamPosition++;
            }
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }

    private Object continueApply(TestState state) {
        if (!state.writer.isEOF()) {
            if (!state.writer.isEmpty()) {
                state.writer.applyAsync()
                                .thenApplyAsync(nl ->
                                    WhenReady.completeAsync(st -> st.writer.isEmpty(), null, state)
                                        .thenApply(nl2 -> continueApply(state)));
            }
            else {
                state.testFuture.complete(null);
            }
        }
        
        return null;
    }

    private class TestState {
        String[] lines;
        TextStreamAsyncOptions options;
        AsyncTextStreamWriter writer;
        byte[] streamBytes;
        CompletableFuture<Void> inputFuture;
        CompletableFuture<Void> testFuture;
        int stringsPosition;
    }
    
    private enum AsyncAgentMethod {
        APPLY_ASYNC,
        START_APPLY_LOOP_ASYNC
    }

}
