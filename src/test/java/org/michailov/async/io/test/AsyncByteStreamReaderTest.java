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

public class AsyncByteStreamReaderTest {

    @Test
    public void testReadAsync() throws Throwable {
        final boolean NOT_LOOP = false;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BUFF_LENGTH = 19;
        final long TIMEOUT_MILLIS = 10000;
        
        System.out.println("\ntestReadAsync {");
        
        testReadAsync(NOT_LOOP, STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, TIMEOUT_MILLIS);
        
        System.out.println("} // testReadAsync");
    }
    
    @Test (expected = TimeoutException.class)
    public void testReadAsyncTimeout() throws Throwable {
        final boolean NOT_LOOP = false;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 200;
        final int BUFF_LENGTH = 19;
        final long TIMEOUT_MILLIS = 100;
        
        System.out.println("\ntestReadAsyncTimeout {");
        
        testReadAsync(NOT_LOOP, STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, TIMEOUT_MILLIS);
        
        System.out.println("} // testReadAsyncTimeout");
    }
    
    @Test
    public void testStartReadingLoopAsync() throws Throwable {
        final boolean LOOP = true;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BUFF_LENGTH = 19;
        final long TIMEOUT_MILLIS = 10000;
        
        System.out.println("\ntestStartReadingLoopAsync {");
        
        testReadAsync(LOOP, STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, TIMEOUT_MILLIS);
        
        System.out.println("} // testStartReadingLoopAsync");
    }

    @Test (expected = TimeoutException.class)
    public void testStartReadingLoopAsyncTimeout() throws Throwable {
        final boolean LOOP = true;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 200;
        final int BUFF_LENGTH = 19;
        final long TIMEOUT_MILLIS = 100;
        
        System.out.println("\ntestStartReadingLoopAsyncTimeout {");
        
        testReadAsync(LOOP, STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, TIMEOUT_MILLIS);
        
        System.out.println("} // testStartReadingLoopAsyncTimeout");
    }

    private void testReadAsync(boolean isLoop, int streamLength, int chunkLength, int chunkDelayMillis, int buffLength, long timeoutMillis) throws Throwable {
        TestState state = new TestState();
        state.options = new AsyncOptions();
        state.options.timeout = timeoutMillis;
        state.ringBuffer = new ByteRingBuffer(buffLength);
        state.simulator = new InputStreamSimulator(streamLength, chunkLength, chunkDelayMillis, TimeUnit.MILLISECONDS);
        state.reader = new AsyncByteStreamReader(state.simulator, state.ringBuffer, state.options);
        state.streamLength = streamLength;
        state.streamPosition = 0;
        state.testFuture = new CompletableFuture<Void>();
        state.isLoop = isLoop;
        
        if (isLoop) {
            // Start a read loop.
            readAndVerifyLoopAsync(state);
        }
        else {
            // Use a sequence of read + verify.
            readAndVerifyAsync(state);
        }
        
        try {
            // Await completion
            state.testFuture.get();
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            
            // If there is a cause, re-throw it.
            Throwable cause = ex.getCause();
            if (cause != null) {
                throw cause;
            }
        }
    }
    private static void readAndVerifyLoopAsync(TestState state) {
        state.operationFuture = state.reader.startApplyLoopAsync();
        state.operationFuture.whenCompleteAsync((result, ex) -> {
            if (ex != null) {
                // On exception - fail the test future.
                state.testFuture.completeExceptionally(ex);
            }
            
            // On success - it will be all over. We'll start a separate async loop to verify the content. 
        });
        
        // Start a verification loop.
        Predicate<TestState> ready = st -> st.ringBuffer.getAvailableToRead() > 0;
        Predicate<TestState> done = st -> st.operationFuture.isDone() && st.ringBuffer.getAvailableToRead() == 0;
        Function<TestState, Void> action = st -> { verifyRingBuffer(st); return null; };
        WhenReady.startApplyLoopAsync(ready, done, action, state);
    }
    
    private static void readAndVerifyAsync(TestState state) {
        state.operationFuture = state.reader.applyAsync();
        state.operationFuture.whenCompleteAsync((result, ex) -> {
            if (ex != null) {
                // On exception - fail the test future.
                state.testFuture.completeExceptionally(ex);
            }
            else {
                // On success - verify ring buffer, and continue reading and verifying.
                boolean isDone = verifyRingBuffer(state);
                if (!isDone) {
                    readAndVerifyAsync(state);
                }
            }
        });
    }
    
    private static boolean verifyRingBuffer(TestState state) {
        // Read and verify any available content from the ring buffer.
        while (state.ringBuffer.getAvailableToRead() > 0) {
            int expectedByte = InputStreamSimulator.CONTENT_BYTES[state.streamPosition % InputStreamSimulator.CONTENT_BYTES_LENGTH];
            int actualByte = state.ringBuffer.read();
            
            System.out.println(String.format("Byte[%1$d]: %2$d = %3$d", state.streamPosition, expectedByte, actualByte));
            if (expectedByte != actualByte) {
                state.testFuture.completeExceptionally(new Exception("Wrong byte!"));
                return true;
            }
            
            state.streamPosition++;
        }

        // Check the operation for failure.
        if (state.operationFuture.isCompletedExceptionally()) {
            // Pass the exception to testFuture (without trying to get the result which would throw here.)
            state.operationFuture.whenComplete((res, th) -> {
                    state.testFuture.completeExceptionally(th);
                });
            return true;
        }
        
        // Check the operation for completion.
        else if ((state.isLoop && state.operationFuture.isDone()) || state.reader.isEOF()) {
            // Verify the stream length.
            System.out.println(String.format("Stream length: %1$d = %2$d", state.streamPosition, state.streamLength));
            if (state.streamPosition != state.streamLength) {
                state.testFuture.completeExceptionally(new Exception("Wrong stream length!"));
                return true;
            }
            
            // Complete the test.
            state.testFuture.complete(null);
            return true;
        }
        
        return false;
    }
    
    private class TestState {
        AsyncOptions options;
        ByteRingBuffer ringBuffer;
        InputStreamSimulator simulator;
        AsyncByteStreamReader reader;
        int streamLength;
        int streamPosition;
        CompletableFuture<Void> testFuture;
        CompletableFuture<Void> operationFuture;
        boolean isLoop;
    }

}
