package org.michailov.async.io.test;

import java.io.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.junit.*;
import org.michailov.async.io.*;
import org.michailov.async.io.AsyncByteStreamReader.CompleteWhen;

public class AsyncByteStreamReaderTest {

    @Test
    public void testReadAsyncAvailable() {
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BUFF_LENGTH = 19;
        final CompleteWhen COMPLETE_WHEN = CompleteWhen.AVAILABLE;
        
        System.out.println("\n testReadAsyncAvailable {");
        ReadAsyncState state = new ReadAsyncState();
        state.simulator = new InputStreamSimulator(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        state.reader = new AsyncByteStreamReader(state.simulator);
        state.streamLength = STREAM_LENGTH;
        state.streamIndex = 0;
        state.buff = new byte[BUFF_LENGTH];
        state.completeWhen = COMPLETE_WHEN;
        state.testFuture = new CompletableFuture();
        
        CompletableFuture<Integer> future = state.reader.readAsync(state.buff, 0, state.buff.length, COMPLETE_WHEN);
        future.whenCompleteAsync((res, th) -> continueReadAsync(res, th, state));
        
        try {
            state.testFuture.get();
        }
        catch (ExecutionException | InterruptedException ex) {
            ex.printStackTrace(System.out);
            Assert.fail();
        }
        
        System.out.println("} // testReadAsyncAvailable");
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
    
    @Test
    public void testReadAsyncFull() {
    }

    @Test
    public void testReadAsyncTimeout() {
    }
    
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
