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

import org.junit.*;

public class DelaySimulatorTest {

    @Test
    public void testChunks() {
        final int STREAM_LENGTH = 25;
        final int CHUNK_LENGTH = 7;
        final int CHUNK_DELAY_MILLIS = 100;
        
        System.out.println("\ntestChunks {");
        
        DelaySimulator simulator = new DelaySimulator(CHUNK_LENGTH, CHUNK_DELAY_MILLIS);
        for (int i = 0; i < STREAM_LENGTH; i++) {
            int ax = i != 0 && i % CHUNK_LENGTH == 0 ? 0 : CHUNK_LENGTH - (i % CHUNK_LENGTH);
            int aa = simulator.getAvailable();
            System.out.println(String.format("%1$d: Available: expected=%2$d, actual=%3$d", i, ax, aa));
            Assert.assertEquals(ax, aa);
            
            long beforeTimeMillis = System.currentTimeMillis();
            simulator.advance();
            long afterTimeMillis = System.currentTimeMillis();
            long delayMillis = afterTimeMillis - beforeTimeMillis; 
            
            if (i != 0 && i % CHUNK_LENGTH == 0) {
                System.out.println(String.format("%1$d: Chunk boundary.", i));
                Assert.assertTrue(delayMillis > CHUNK_DELAY_MILLIS);
            }
            else {
                System.out.println(String.format("%1$d: Fast forward.", i));
                Assert.assertTrue(delayMillis < 50);
            }
        }
        
        System.out.println("} // testChunks");
    }
}
