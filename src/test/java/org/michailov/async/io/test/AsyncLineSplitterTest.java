package org.michailov.async.io.test;

import java.util.concurrent.*;
import java.util.function.*;
import org.junit.*;
import org.michailov.async.*;
import org.michailov.async.io.*;

public class AsyncLineSplitterTest {

    @Test
    public void testArray() {
        final String[] LINES = {
                "",
                "one",
                "",
                "",
                "two",
                "",
                "three",
                "",
                "",
                "",
        };
        final String CONTENT;
        final int CHAR_RING_BUFFER_CAPACITY = 7;
        final int STRING_RING_BUFFER_CAPACITY = LINES.length;
        
        System.out.println("\ntestArray {");
        
        StringBuilder content = new StringBuilder(500);
        int lineBreaksLength = InputStreamSimulator.LINE_BREAKS.length;
        
        for (int i = 0; i < LINES.length; i++) {
            System.out.println(String.format("LINES[%1$d] = '%2$s'", i, LINES[i]));
            
            content.append(LINES[i]);
            content.append(InputStreamSimulator.LINE_BREAKS[i % lineBreaksLength]);
        }
        CONTENT = content.toString();
        System.out.println(String.format("Content length = %1$d", CONTENT.length()));
        
        TestState state = new TestState(); 
        state.lines = LINES;
        state.options = new LineAsyncOptions();
        state.charRingBuffer = new CharRingBuffer(CHAR_RING_BUFFER_CAPACITY);
        state.stringRingBuffer = new StringRingBuffer(STRING_RING_BUFFER_CAPACITY);
        state.splitter = new AsyncLineSplitter(state.charRingBuffer, state.stringRingBuffer, state.options);
        
        // Start input loop.
        Predicate<TestState> ready = st -> st.charsPosition < CONTENT.length() && st.charRingBuffer.getAvailableToWrite() > 0;
        Predicate<TestState> done = st -> st.charsPosition == CONTENT.length();
        Function<TestState, Void> action = st -> { st.charRingBuffer.write(CONTENT.charAt(st.charsPosition++)); return null; };
        state.inputFuture = WhenReady.startApplyLoopAsync(ready, done, action, state)
                                .whenCompleteAsync((nl, ex) -> state.charRingBuffer.setEOF());

        testState(state);
        
        System.out.println("} // testArray");
    }
    
    @Test
    public void testStreamReader() {
        final int STREAM_LENGTH = 101; // Make sure this matches the end of a line!
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        final int BYTE_RING_BUFFER_CAPACITY = 11;
        final int CHAR_RING_BUFFER_CAPACITY = 7;
        final int STRING_RING_BUFFER_CAPACITY = InputStreamSimulator.LINES.length;
        
        System.out.println("\ntestStreamReader {");
        
        TestState state = new TestState(); 
        state.lines = InputStreamSimulator.LINES;
        state.options = new LineAsyncOptions();
        state.options.charset = InputStreamSimulator.CHARSET;
        state.charRingBuffer = new CharRingBuffer(CHAR_RING_BUFFER_CAPACITY);
        state.stringRingBuffer = new StringRingBuffer(STRING_RING_BUFFER_CAPACITY);
        state.splitter = new AsyncLineSplitter(state.charRingBuffer, state.stringRingBuffer, state.options);
        
        // Start input loop.
        InputStreamSimulator simulator = new InputStreamSimulator(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        ByteRingBuffer byteRingBuffer = new ByteRingBuffer(BYTE_RING_BUFFER_CAPACITY);
        AsyncByteStreamReader reader = new AsyncByteStreamReader(simulator, byteRingBuffer, state.options);
        AsyncCharDecoder decoder = new AsyncCharDecoder(byteRingBuffer, state.charRingBuffer, state.options);
        reader.startApplyLoopAsync();
        state.inputFuture = decoder.startApplyLoopAsync();

        testState(state);
        
        System.out.println("} // testStreamReader");
    }
    
    private void testState(TestState state) {
        // Start splitting loop.
        state.splittingFuture = state.splitter.startApplyLoopAsync();
        
        // Start verification loop.
        Predicate<TestState> ready = st -> st.stringRingBuffer.getAvailableToRead() > 0;
        Predicate<TestState> done = st -> st.stringRingBuffer.isEOF() && st.stringRingBuffer.getAvailableToRead() == 0;
        Function<TestState, Void> action = st -> { 
            while(ready.test(st)) {
                String sx = st.lines[st.stringsPosition];
                String sa = st.stringRingBuffer.read();
                System.out.println(String.format("%1$d: '%2$s' = '%3$s'", st.stringsPosition, sx, sa));
                Assert.assertEquals(sx, sa);
                st.stringsPosition++;
            }
            return null; 
        };
        state.verificationFuture = WhenReady.startApplyLoopAsync(ready, done, action, state);
        
        try {
            state.inputFuture.get();
            state.splittingFuture.get();
            state.verificationFuture.get();
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }
    
    private class TestState {
        String[] lines;
        LineAsyncOptions options;
        CharRingBuffer charRingBuffer;
        StringRingBuffer stringRingBuffer;
        AsyncLineSplitter splitter;
        CompletableFuture<Void> inputFuture;
        CompletableFuture<Void> splittingFuture;
        CompletableFuture<Void> verificationFuture;
        int charsPosition;
        int stringsPosition;
    }
    
}
