package org.michailov.async.io.test;

import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.junit.*;
import org.michailov.async.*;
import org.michailov.async.io.*;

public class AsyncCharDecoderTest {

    @Test
    public void testUTF8() {
        final Charset CHARSET = StandardCharsets.UTF_8;
        final String TEXT = "БаДаГе";
        final byte[] BYTES = TEXT.getBytes(CHARSET);
        final int BYTE_RING_BUFFER_CAPACITY = 3;
        final int CHAR_RING_BUFFER_CAPACITY = TEXT.length();
        
        for (int i = 0; i < BYTES.length; i++) {
            System.out.println(String.format("BYTES[%1$d] = %2$d", i, BYTES[i]));
        }
        
        TestState state = new TestState(); 
        state.byteRingBuffer = new ByteRingBuffer(BYTE_RING_BUFFER_CAPACITY);
        state.charRingBuffer = new CharRingBuffer(CHAR_RING_BUFFER_CAPACITY);
        state.options = new CharsetAsyncOptions();
        state.options.charset = CHARSET;
        state.decoder = new AsyncCharDecoder(state.byteRingBuffer, state.charRingBuffer, state.options);
        
        Predicate<TestState> ready = st -> st.byteRingBuffer.getAvailableToWrite() > 0;
        Predicate<TestState> done = st -> st.bytesPosition == BYTES.length;
        Function<TestState, Void> action = st -> { st.byteRingBuffer.write(BYTES[st.bytesPosition++]); return null; };
        CompletableFuture<Void> inputFuture = WhenReady.startApplyLoopAsync(ready, done, action, state)
                                                    .whenCompleteAsync((nl, th) -> state.byteRingBuffer.setEOF());
        
        CompletableFuture<Void> outputFuture = state.decoder.startApplyLoopAsync();
        
        try {
            inputFuture.get();
            outputFuture.get();
            
            for (int i = 0; i < TEXT.length(); i++) {
                int cx = TEXT.charAt(i);
                int ca = state.charRingBuffer.read();
                System.out.println(String.format("%1$d: '%2$c' (%2$d) = '%3$c' (%3$d)", i, cx, ca));
                Assert.assertEquals(cx, ca);
            }
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }

}

class TestState {
    ByteRingBuffer byteRingBuffer;
    CharRingBuffer charRingBuffer;
    CharsetAsyncOptions options;
    AsyncCharDecoder decoder;
    int bytesPosition;
}
