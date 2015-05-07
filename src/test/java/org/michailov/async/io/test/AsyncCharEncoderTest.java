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

import java.nio.charset.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.junit.*;
import org.michailov.async.*;
import org.michailov.async.io.*;
import org.michailov.async.io.test.AsyncCharDecoderTest.TestState;

public class AsyncCharEncoderTest {

    @Test
    public void testArray() {
        final Charset CHARSET = StandardCharsets.UTF_16LE;
        final String CONTENT = "БаДаГе";
        final byte[] BYTES = CONTENT.getBytes(CHARSET);
        final int CHAR_RING_BUFFER_CAPACITY = BYTES.length;
        final int BYTE_RING_BUFFER_CAPACITY = 3;
        
        System.out.println("\ntestArray {");
        
        for (int i = 0; i < BYTES.length; i++) {
            System.out.println(String.format("BYTES[%1$d] = %2$d", i, BYTES[i]));
        }
        
        TestState state = new TestState(); 
        state.contentBytes = BYTES;
        state.charRingBuffer = new CharRingBuffer(CHAR_RING_BUFFER_CAPACITY);
        state.byteRingBuffer = new ByteRingBuffer(BYTE_RING_BUFFER_CAPACITY);
        state.options = new CharsetAsyncOptions();
        state.options.charset = CHARSET;
        state.encoder = new AsyncCharEncoder(state.charRingBuffer, state.byteRingBuffer, state.options);
        
        // Start input loop.
        Predicate<TestState> ready = st -> st.charsPosition < CONTENT.length() && st.charRingBuffer.getAvailableToWrite() > 0;
        Predicate<TestState> done = st -> st.charsPosition == CONTENT.length();
        Function<TestState, Void> action = st -> { st.charRingBuffer.write(CONTENT.charAt(st.charsPosition++)); return null; };
        state.inputFuture = WhenReady.startApplyLoopAsync(ready, done, action, state)
                                .whenCompleteAsync((nl, ex) -> state.charRingBuffer.setEOF());

        testState(state);
        
        System.out.println("} // testArray");
    }

    private void testState(TestState state) {
        // Start encoding loop.
        state.encodingFuture = state.encoder.startApplyLoopAsync();
        
        // Start verification loop.
        Predicate<TestState> ready = st -> st.byteRingBuffer.getAvailableToRead() > 0;
        Predicate<TestState> done = st -> st.byteRingBuffer.isEOF() && st.byteRingBuffer.getAvailableToRead() == 0;
        Function<TestState, Void> action = st -> { 
            while(ready.test(st)) {
                byte bx = st.contentBytes[st.bytesPosition];
                byte ba = (byte)st.byteRingBuffer.read();
                System.out.println(String.format("%1$d: %2$d = %3$d", st.bytesPosition, bx, ba));
                Assert.assertEquals(bx, ba);
                st.bytesPosition++;
            }
            return null; 
        };
        state.verificationFuture = WhenReady.startApplyLoopAsync(ready, done, action, state);
        
        try {
            state.inputFuture.get();
            state.encodingFuture.get();
            state.verificationFuture.get();
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }
    
    private class TestState {
        byte[] contentBytes;
        CharsetAsyncOptions options;
        CharRingBuffer charRingBuffer;
        ByteRingBuffer byteRingBuffer;
        AsyncCharEncoder encoder;
        CompletableFuture<Void> inputFuture;
        CompletableFuture<Void> encodingFuture;
        CompletableFuture<Void> verificationFuture;
        int charsPosition;
        int bytesPosition;
    }
    
}
