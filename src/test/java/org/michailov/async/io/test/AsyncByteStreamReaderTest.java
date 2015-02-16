package org.michailov.async.io.test;

import java.util.concurrent.*;
import org.junit.*;
import org.michailov.async.io.*;

public class AsyncByteStreamReaderTest {

    @Test
    public void testReadAsyncAvailable() throws Throwable {
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BUFF_LENGTH = 19;
        final CompleteWhen COMPLETE_WHEN = CompleteWhen.AVAILABLE;
        final long TIMEOUT_MILLIS = 10000;
        
        System.out.println("\n testReadAsyncAvailable {");
        
        testReadAsync(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, COMPLETE_WHEN, TIMEOUT_MILLIS);
        
        System.out.println("} // testReadAsyncAvailable");
    }
    
    @Test
    public void testReadAsyncFull() throws Throwable {
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BUFF_LENGTH = 19;
        final CompleteWhen COMPLETE_WHEN = CompleteWhen.FULL;
        final long TIMEOUT_MILLIS = 10000;
        
        System.out.println("\n testReadAsyncFull {");
        
        testReadAsync(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, COMPLETE_WHEN, TIMEOUT_MILLIS);
        
        System.out.println("} // testReadAsyncFull");
    }

    @Test (expected = TimeoutException.class)
    public void testReadAsyncTimeout() throws Throwable {
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BUFF_LENGTH = 19;
        final CompleteWhen COMPLETE_WHEN = CompleteWhen.FULL;
        final long TIMEOUT_MILLIS = 100;
        
        System.out.println("\n testReadAsyncTimeout {");
        
        testReadAsync(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, BUFF_LENGTH, COMPLETE_WHEN, TIMEOUT_MILLIS);
        
        System.out.println("} // testReadAsyncTimeout");
    }
    
    private void testReadAsync(int streamLength, int chunkLength, int chunkDelayMillis, int buffLength, CompleteWhen completeWhen, long timeoutMillis) throws Throwable {
        ReadAsyncState state = new ReadAsyncState();
        state.simulator = new InputStreamSimulator(streamLength, chunkLength, chunkDelayMillis, TimeUnit.MILLISECONDS);
        state.reader = new AsyncByteStreamReader(state.simulator);
        state.streamLength = streamLength;
        state.streamIndex = 0;
        state.buff = new byte[buffLength];
        state.completeWhen = completeWhen;
        state.testFuture = new CompletableFuture<Void>();
        
        // Read async
        CompletableFuture<Integer> future = state.reader.readAsync(state.buff, 0, state.buff.length, completeWhen, timeoutMillis, TimeUnit.MILLISECONDS);
        future.whenCompleteAsync((res, th) -> continueReadAsync(res, th, state));
        
        // Await completion
        try {
            state.testFuture.get();
        }
        catch (ExecutionException | InterruptedException ex) {
            ex.printStackTrace();
            
            // If there is a cause, re-throw it.
            Throwable cause = ex.getCause();
            if (cause != null) {
                throw cause;
            }
        }
    }

    private static void continueReadAsync(Integer result, Throwable throwable, ReadAsyncState state) {
        if (result != null) {
            int n = result.intValue();
            if (n != -1) {
                // Verify result and buffer.
                for (int i = 0; i < n; i++) {
                    int bx = InputStreamSimulator.CONTENT_BYTES[(state.streamIndex + i) % InputStreamSimulator.CONTENT_BYTES_LENGTH];
                    System.out.println(String.format("Byte[%1$d]: %2$d = %3$d", state.streamIndex + i, bx, state.buff[i]));
                    if (bx != state.buff[i]) {
                        state.testFuture.completeExceptionally(new Exception("Wrong byte!"));
                        return;
                    }
                }
                
                // Continue reading async.
                state.streamIndex += n;
                CompletableFuture<Integer> future = state.reader.readAsync(state.buff, 0, state.buff.length, state.completeWhen);
                future.whenCompleteAsync((res, th) -> continueReadAsync(res, th, state));
            }
            else {
                // EOF
                // Verify length.
                System.out.println(String.format("Stream length: %1$d = %2$d", state.streamIndex, state.streamLength));
                if (state.streamIndex != state.streamLength) {
                    state.testFuture.completeExceptionally(new Exception("Wrong stream length!"));
                    return;
                }
                
                // Complete test.
                state.testFuture.complete(null);
            }
        }
        else {
            // Exception - abort the test.
            throwable.printStackTrace(System.out);
            state.testFuture.completeExceptionally(throwable);
        }
    };
    
    private class ReadAsyncState {
        InputStreamSimulator simulator;
        AsyncByteStreamReader reader;
        int streamLength;
        int streamIndex;
        byte[] buff;
        CompleteWhen completeWhen;
        CompletableFuture<Void> testFuture;
    }

}
