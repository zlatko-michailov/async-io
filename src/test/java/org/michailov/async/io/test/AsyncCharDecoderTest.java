package org.michailov.async.io.test;

import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.junit.*;
import org.michailov.async.*;
import org.michailov.async.io.*;

public class AsyncCharDecoderTest {

    @Test
    public void testArray() {
        final Charset CHARSET = StandardCharsets.UTF_16;
        final String CONTENT = "БаДаГе";
        final byte[] BYTES = CONTENT.getBytes(CHARSET);
        final int BYTE_RING_BUFFER_CAPACITY = 3;
        final int CHAR_RING_BUFFER_CAPACITY = CONTENT.length();
        
        System.out.println("\ntestArray {");
        
        for (int i = 0; i < BYTES.length; i++) {
            System.out.println(String.format("BYTES[%1$d] = %2$d", i, BYTES[i]));
        }
        
        TestState state = new TestState(); 
        state.content = CONTENT;
        state.byteRingBuffer = new ByteRingBuffer(BYTE_RING_BUFFER_CAPACITY);
        state.charRingBuffer = new CharRingBuffer(CHAR_RING_BUFFER_CAPACITY);
        state.options = new CharsetAsyncOptions();
        state.options.charset = CHARSET;
        state.decoder = new AsyncCharDecoder(state.byteRingBuffer, state.charRingBuffer, state.options);
        
        // Start input loop.
        Predicate<TestState> ready = st -> st.bytesPosition < BYTES.length && st.byteRingBuffer.getAvailableToWrite() > 0;
        Predicate<TestState> done = st -> st.bytesPosition == BYTES.length;
        Function<TestState, Void> action = st -> { st.byteRingBuffer.write(BYTES[st.bytesPosition++]); return null; };
        state.inputFuture = WhenReady.startApplyLoopAsync(ready, done, action, state)
                                .whenCompleteAsync((nl, ex) -> state.byteRingBuffer.setEOF());

        testState(state);
        
        System.out.println("} // testArray");
    }

    @Test
    public void testStreamReader() {
        final int BYTE_RING_BUFFER_CAPACITY = 11;
        final int CHAR_RING_BUFFER_CAPACITY = 13;
        final int STREAM_LENGTH = 100;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        
        System.out.println("\ntestStreamReader {");
        
        TestState state = new TestState(); 
        state.content = InputStreamSimulator.CONTENT;
        state.byteRingBuffer = new ByteRingBuffer(BYTE_RING_BUFFER_CAPACITY);
        state.charRingBuffer = new CharRingBuffer(CHAR_RING_BUFFER_CAPACITY);
        state.options = new CharsetAsyncOptions();
        state.options.charset = InputStreamSimulator.CHARSET;
        state.decoder = new AsyncCharDecoder(state.byteRingBuffer, state.charRingBuffer, state.options);
        
        // Start input loop.
        InputStreamSimulator simulator = new InputStreamSimulator(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        AsyncByteStreamReader reader = new AsyncByteStreamReader(simulator, state.byteRingBuffer, state.options);
        state.inputFuture = reader.startApplyLoopAsync();
        
        testState(state);

        System.out.println("} // testStreamReader");
    }
    
    private void testState(TestState state) {
        // Start decoding loop.
        state.decodingFuture = state.decoder.startApplyLoopAsync();
        
        // Start verification loop.
        Predicate<TestState> ready = st -> st.charRingBuffer.getAvailableToRead() > 0;
        Predicate<TestState> done = st -> st.charRingBuffer.isEOF() && st.charRingBuffer.getAvailableToRead() == 0;
        Function<TestState, Void> action = st -> { 
            while(ready.test(st)) {
                int cx = st.content.charAt(st.charsPosition);
                int ca = st.charRingBuffer.read();
                System.out.println(String.format("%1$d: '%2$c' (%2$d) = '%3$c' (%3$d)", st.charsPosition, cx, ca));
                Assert.assertEquals(cx, ca);
                st.charsPosition++;
            }
            return null; 
        };
        state.verificationFuture = WhenReady.startApplyLoopAsync(ready, done, action, state);
        
        try {
            state.inputFuture.get();
            state.decodingFuture.get();
            state.verificationFuture.get();
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }
    
    private class TestState {
        String content;
        CharsetAsyncOptions options;
        ByteRingBuffer byteRingBuffer;
        CharRingBuffer charRingBuffer;
        AsyncCharDecoder decoder;
        CompletableFuture<Void> inputFuture;
        CompletableFuture<Void> decodingFuture;
        CompletableFuture<Void> verificationFuture;
        int bytesPosition;
        int charsPosition;
    }
    
}


