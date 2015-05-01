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

public class AsyncByteStreamWriterTest {

    @Test
    public void testWriteAsync() throws Throwable {
        final boolean NOT_LOOP = false;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BUFF_LENGTH = 19;
        final long TIMEOUT_MILLIS = 10000;
        
        System.out.println("\ntestWriteAsync {");
        
        testWriteAsync(NOT_LOOP, STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, TIMEOUT_MILLIS);
        
        System.out.println("} // testWriteAsync");
    }
    
    @Test (expected = TimeoutException.class)
    public void testWriteAsyncTimeout() throws Throwable {
        final boolean NOT_LOOP = false;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 200;
        final int BUFF_LENGTH = 19;
        final long TIMEOUT_MILLIS = 100;
        
        System.out.println("\ntestWriteAsyncTimeout {");
        
        testWriteAsync(NOT_LOOP, STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, TIMEOUT_MILLIS);
        
        System.out.println("} // testWriteAsyncTimeout");
    }
    
    @Test
    public void testStartWritingLoopAsync() throws Throwable {
        final boolean LOOP = true;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BUFF_LENGTH = 19;
        final long TIMEOUT_MILLIS = 10000;
        
        System.out.println("\ntestStartWritingLoopAsync {");
        
        testWriteAsync(LOOP, STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, TIMEOUT_MILLIS);
        
        System.out.println("} // testStartWritingLoopAsync");
    }

    @Test (expected = TimeoutException.class)
    public void testStartWritingLoopAsyncTimeout() throws Throwable {
        final boolean LOOP = true;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 200;
        final int BUFF_LENGTH = 19;
        final long TIMEOUT_MILLIS = 100;
        
        System.out.println("\ntestStartWritingLoopAsyncTimeout {");
        
        testWriteAsync(LOOP, STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, TIMEOUT_MILLIS);
        
        System.out.println("} // testStartWritingLoopAsyncTimeout");
    }

    private void testWriteAsync(boolean isLoop, int streamLength, int chunkLength, int chunkDelayMillis, int buffLength, long timeoutMillis) throws Throwable {
        TestState state = new TestState();
        state.options = new AsyncOptions();
        state.options.timeout = timeoutMillis;
        state.ringBuffer = new ByteRingBuffer(buffLength);
        state.simulator = new OutputStreamSimulator(streamLength, chunkLength, chunkDelayMillis, TimeUnit.MILLISECONDS);
        state.writer = new AsyncByteStreamWriter(state.simulator, state.ringBuffer, state.options);
        state.streamLength = streamLength;
        state.streamPosition = 0;
        state.testFuture = new CompletableFuture<Void>();
        state.isLoop = isLoop;
        
        startFeedingLoopAsync(state);
        
        if (isLoop) {
            // Start a write loop. Verify at the end.
            writeAndVerifyLoopAsync(state);
        }
        else {
            // Use a sequence of write + verify.
            writeAndVerifyAsync(state);
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

    private static void startFeedingLoopAsync(TestState state) {
        Predicate<TestState> ready = st -> {
                return st.ringBuffer.getAvailableToWrite() > 0; 
            };
            
        Predicate<TestState> done = st -> {
                boolean isContentDone = st.streamPosition >= st.streamLength;
                boolean isDone = (st.operationFuture != null && st.operationFuture.isCompletedExceptionally()) || isContentDone;
                if (isContentDone) {
                    st.ringBuffer.setEOF();
                }
                return isDone;
            };
            
        Function<TestState, Void> action = st -> {
                st.ringBuffer.write(InputStreamSimulator.CONTENT_BYTES[st.streamPosition++]); 
                return null; 
            };
            
        WhenReady.startApplyLoopAsync(ready, done, action, state);
    }
    
    private static void writeAndVerifyLoopAsync(TestState state) {
        state.operationFuture = state.writer.startApplyLoopAsync();
        state.operationFuture.whenCompleteAsync((result, ex) -> {
            if (ex != null) {
                // On exception - fail the test future.
                state.testFuture.completeExceptionally(ex);
            }
            
            // On success - synchronously verify the content.
            System.out.println(String.format("Content = %1$d, Sent = %2$d, Received = %3$d", state.streamLength, state.streamPosition, state.simulator.getNextStreamIndex()));
            Assert.assertEquals(state.streamLength, state.streamPosition);
            Assert.assertEquals(state.streamLength, state.simulator.getNextStreamIndex());
            verifyContent(state);
        });
    }
    
    private static void writeAndVerifyAsync(TestState state) {
        state.operationFuture = state.writer.applyAsync();
        state.operationFuture.whenCompleteAsync((result, ex) -> {
            if (ex != null) {
                // On exception - fail the test future.
                state.testFuture.completeExceptionally(ex);
            }
            else {
                // On success - verify ring buffer, and continue reading and verifying.
                boolean isDone = verifyContent(state);
                if (!isDone) {
                    writeAndVerifyAsync(state);
                }
            }
        });
    }
    
    private static boolean verifyContent(TestState state) {
        int streamPosition = state.simulator.getNextStreamIndex();
        for (int i = state.verifiedPosition; i < streamPosition; i++) {
            byte bx = InputStreamSimulator.CONTENT_BYTES[i];
            byte ba = state.simulator.getContentBytes()[i];
            System.out.println(String.format("Byte [%1$d]: Expected = %2$d, Actual = %3$d", i, bx, ba));
            Assert.assertEquals(bx, ba);
        }
        
        state.verifiedPosition = streamPosition;
        
        // Check the operation for failure.
        if (state.operationFuture.isCompletedExceptionally()) {
            // Pass the exception to testFuture (without trying to get the result which would throw here.)
            state.operationFuture.whenComplete((res, th) -> {
                    state.testFuture.completeExceptionally(th);
                });
            return true;
        }
        
        // Check the operation for completion.
        else if ((state.isLoop && state.operationFuture.isDone()) || state.writer.isEOF()) {
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
        OutputStreamSimulator simulator;
        AsyncByteStreamWriter writer;
        int streamLength;
        int streamPosition;
        int verifiedPosition;
        CompletableFuture<Void> testFuture;
        CompletableFuture<Void> operationFuture;
        boolean isLoop;
    }

}
