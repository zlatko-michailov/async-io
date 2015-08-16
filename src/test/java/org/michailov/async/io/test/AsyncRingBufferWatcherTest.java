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
import org.junit.*;
import org.michailov.async.*;
import org.michailov.async.io.*;

public class AsyncRingBufferWatcherTest {

    @Test
    public void teststartApplyLoopAsync() {
        System.out.println("\ntestStartApplyLoopAsync {");
        
        final int BUFF_LENGTH = 14;
        final int LINE_COUNT = BUFF_LENGTH;
        
        // Create a ring buffer and a watcher.
        TestState state = new TestState();
        state.linesPosition = -1;
        state.ring = new StringRingBuffer(BUFF_LENGTH);
        AsyncRingBufferWatcher watcher = new AsyncRingBufferWatcher(state.ring, r -> onAvailableToRead(state), AsyncOptions.DEFAULT);
        
        // Start the watcher. 
        // When it's done, the test is done.
        state.verificationFuture = watcher.startApplyLoopAsync();
        
        // Write items to the ring buffer, and complete it.
        for (int i = 0; i < BUFF_LENGTH; i++) {
            state.ring.write(InputStreamSimulator.LINES[i]);
        }
        state.ring.setEOF();
        
        try {
            // Wait until the watcher is done.
            state.verificationFuture.get();
            
            // Verify all the written lines have been read.
            System.out.println(String.format("%1$d = %2$d", state.linesPosition + 1, LINE_COUNT));
            Assert.assertEquals(state.linesPosition + 1, LINE_COUNT);
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected exception");
        }
        
        System.out.println("} // testStartApplyLoopAsync");
    }
    
    private void onAvailableToRead(TestState state) {
        while (state.ring.getAvailableToRead() > 0) {
            state.linesPosition++;
            String sa = state.ring.read();
            String sx = InputStreamSimulator.LINES[state.linesPosition]; 
            System.out.println(String.format("[%1$d] '%2$s' = '%3$s'", state.linesPosition, sx, sa));
            Assert.assertEquals(sx, sa);
        }
    }
    
    private class TestState {
        CompletableFuture<Void> verificationFuture;
        StringRingBuffer ring;
        int linesPosition;
    }

}
