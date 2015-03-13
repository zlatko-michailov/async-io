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
        ReadAsyncState state = new ReadAsyncState();
        state.asyncOptions = new AsyncOptions();
        state.asyncOptions.timeout = timeoutMillis;
        state.ringBuffer = new ByteRingBuffer(buffLength);
        state.simulator = new InputStreamSimulator(streamLength, chunkLength, chunkDelayMillis, TimeUnit.MILLISECONDS);
        state.reader = new AsyncByteStreamReader(state.simulator, state.ringBuffer, state.asyncOptions);
        state.streamLength = streamLength;
        state.streamIndex = 0;
        state.testFuture = new CompletableFuture<Void>();
        
        if (isLoop) {
            // Start a read loop.
            CompletableFuture<Void> future = state.reader.startReadingLoopAsync();
            future.whenCompleteAsync((result, ex) -> {
                if (ex != null) {
                    // On exception - fail the test future.
                    state.testFuture.completeExceptionally(ex);
                }
            });
            
            // Start a verification loop.
            Predicate<ReadAsyncState> ready = st -> st.ringBuffer.getAvailableToRead() > 0;
            Predicate<ReadAsyncState> done = st -> st.reader.getEOF().isDone() && st.ringBuffer.getAvailableToRead() == 0;
            Function<ReadAsyncState, Void> action = st -> { verifyRingBuffer(st); return null; };
            WhenReady.startApplyLoopAsync(ready, done, action, state);
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
    
    private static void readAndVerifyAsync(ReadAsyncState state) {
        CompletableFuture<Void> future = state.reader.readAsync();
        future.whenCompleteAsync((result, ex) -> {
            if (ex == null) {
                // On success - verify ring buffer, and continue reading and verifying.
                boolean isDone = verifyRingBuffer(state);
                if (!isDone) {
                    readAndVerifyAsync(state);
                }
            }
            else {
                // On exception - fail the test future.
                state.testFuture.completeExceptionally(ex);
            }
        });
    }
    
    private static boolean verifyRingBuffer(ReadAsyncState state) {
        while (state.ringBuffer.getAvailableToRead() > 0) {
            int bx = InputStreamSimulator.CONTENT_BYTES[state.streamIndex % InputStreamSimulator.CONTENT_BYTES_LENGTH];
            int ba = state.ringBuffer.read();
            
            System.out.println(String.format("Byte[%1$d]: %2$d = %3$d", state.streamIndex, bx, ba));
            if (bx != ba) {
                state.testFuture.completeExceptionally(new Exception("Wrong byte!"));
                return true;
            }
            
            state.streamIndex++;
        }

        CompletableFuture<Void> eof = state.reader.getEOF();
        if (eof.isCompletedExceptionally()) {
            // An exception has occurred. 
            // Pass it to testFuture.
            eof.whenComplete((res, th) -> {
                    state.testFuture.completeExceptionally(th);
                });
            return true;
        }
        else if (eof.isDone()) {
            // EOF
            // Verify length.
            System.out.println(String.format("Stream length: %1$d = %2$d", state.streamIndex, state.streamLength));
            if (state.streamIndex != state.streamLength) {
                state.testFuture.completeExceptionally(new Exception("Wrong stream length!"));
                return true;
            }
            
            // Complete test.
            state.testFuture.complete(null);
            return true;
        }
        
        return false;
    }
    
    private class ReadAsyncState {
        AsyncOptions asyncOptions;
        ByteRingBuffer ringBuffer;
        InputStreamSimulator simulator;
        AsyncByteStreamReader reader;
        int streamLength;
        int streamIndex;
        CompletableFuture<Void> testFuture;
    }

}
