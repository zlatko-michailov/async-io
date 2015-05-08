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

public class AsyncLineJoinerTest {

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
        final String LINE_BREAK = LineAsyncOptions.CRLF;
        final int STRING_RING_BUFFER_CAPACITY = LINES.length;
        final int CHAR_RING_BUFFER_CAPACITY = 7;
        
        System.out.println("\ntestArray {");
        
        StringBuilder content = new StringBuilder(500);
        for (int i = 0; i < LINES.length; i++) {
            System.out.println(String.format("LINES[%1$d] = '%2$s'", i, LINES[i]));
            
            content.append(LINES[i]);
            content.append(LINE_BREAK);
        }
        
        TestState state = new TestState();
        state.lines = LINES;
        state.content = content.toString();
        state.options = new LineAsyncOptions();
        state.options.lineBreak = LINE_BREAK;
        state.stringRingBuffer = new StringRingBuffer(STRING_RING_BUFFER_CAPACITY);
        state.charRingBuffer = new CharRingBuffer(CHAR_RING_BUFFER_CAPACITY);
        state.joiner = new AsyncLineJoiner(state.stringRingBuffer, state.charRingBuffer, state.options);
        
        System.out.println(String.format("Content length = %1$d", state.content.length()));
        
        // Start input loop.
        Predicate<TestState> ready = st -> st.stringsPosition < st.lines.length && st.stringRingBuffer.getAvailableToWrite() > 0;
        Predicate<TestState> done = st -> st.stringsPosition == st.lines.length;
        Function<TestState, Void> action = st -> { st.stringRingBuffer.write(st.lines[st.stringsPosition++]); return null; };
        state.inputFuture = WhenReady.startApplyLoopAsync(ready, done, action, state)
                                .whenCompleteAsync((nl, ex) -> state.stringRingBuffer.setEOF());

        testState(state);
        
        System.out.println("} // testArray");
    }
    
    private void testState(TestState state) {
        // Start splitting loop.
        state.joiningFuture = state.joiner.startApplyLoopAsync();
        
        // Start verification loop.
        Predicate<TestState> ready = st -> st.charRingBuffer.getAvailableToRead() > 0;
        Predicate<TestState> done = st -> st.charRingBuffer.isEOF() && st.charRingBuffer.getAvailableToRead() == 0;
        Function<TestState, Void> action = st -> { 
            while(ready.test(st)) {
                int cx = st.content.charAt(st.charsPosition);
                int ca = st.charRingBuffer.read();
                System.out.println(String.format("%1$d: '%2$d' = '%3$d'", st.charsPosition, cx, ca));
                Assert.assertEquals(cx, ca);
                st.charsPosition++;
            }
            return null; 
        };
        state.verificationFuture = WhenReady.startApplyLoopAsync(ready, done, action, state);
        
        try {
            state.inputFuture.get();
            state.joiningFuture.get();
            state.verificationFuture.get();
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
    }
    
    private class TestState {
        String[] lines;
        String content;
        LineAsyncOptions options;
        StringRingBuffer stringRingBuffer;
        CharRingBuffer charRingBuffer;
        AsyncLineJoiner joiner;
        CompletableFuture<Void> inputFuture;
        CompletableFuture<Void> joiningFuture;
        CompletableFuture<Void> verificationFuture;
        int stringsPosition;
        int charsPosition;
    }
    
}
