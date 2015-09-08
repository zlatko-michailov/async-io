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
import org.junit.*;
import org.michailov.async.io.*;

public class AsyncTextStreamReaderTest {

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
        final int STREAM_LENGTH = 101; // Make sure this matches the end of a line!
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int LINE_COUNT = 14;

        // Prepare state.
        TestState state = new TestState(); 
        state.lines = InputStreamSimulator.LINES;
        state.options = new TextStreamAsyncOptions();
        state.options.charset = InputStreamSimulator.CHARSET;
        InputStreamSimulator simulator = new InputStreamSimulator(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        
        switch (method) {
        case APPLY_ASYNC:
            // Create a reader without a watcher.
            state.reader = new AsyncTextStreamReader(simulator, state.options);
            
            // Prepare for verification.
            state.verificationFuture = new CompletableFuture<Void>();
            
            // Start reader loop.
            state.reader.applyAsync()
                            .thenApplyAsync(nl -> verifyApply(state));
            break;
            
        case START_APPLY_LOOP_ASYNC:
            // Create a reader with a watcher.
            state.reader = new AsyncTextStreamReader(simulator, srb -> onAvailableToRead(state), state.options);
            
            // The test is done when the reader is done.
            // Start reader loop.
            state.verificationFuture = state.reader.startApplyLoopAsync();
            break;
        }
        
        try {
            // Wait for the verification loop to complete.
            state.verificationFuture.get();
            
            // Verify the total number of lines read.
            int nx = LINE_COUNT;
            int na = state.stringsPosition;
            System.out.println(String.format("Line count: '%1$d' = '%2$d'", nx, na));
            Assert.assertEquals(nx, na);
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }

    private Object verifyApply(TestState state) {
        if (!state.reader.isEOF()) {
            verifyLine(state);
            
            if (!state.reader.getStringRingBuffer().isEOF()) {
                state.reader.applyAsync()
                                .thenApply(nl -> verifyApply(state));
            }
            else {
                state.verificationFuture.complete(null);
            }
        }
        
        return null;
    }
    
    private static void onAvailableToRead(TestState state) {
        while (state.reader.getStringRingBuffer().getAvailableToRead() > 0) {
            verifyLine(state);
        }
    }
    
    private static void verifyLine(TestState state) {
        String sx = state.lines[state.stringsPosition];
        String sa = state.reader.getStringRingBuffer().read();
        
        System.out.println(String.format("[%1$d] '%2$s' = '%3$s'", state.stringsPosition++, sx, sa));
        Assert.assertEquals(sx, sa);
    }

    private class TestState {
        String[] lines;
        TextStreamAsyncOptions options;
        AsyncTextStreamReader reader;        
        CompletableFuture<Void> verificationFuture;
        int stringsPosition;
    }
    
    private enum AsyncAgentMethod {
        APPLY_ASYNC,
        START_APPLY_LOOP_ASYNC
    }
    
}
