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

import java.io.*;
import java.util.concurrent.*;
import org.junit.*;

public class InputStreamSimulatorTest {
    
    @Test
    public void testSimulatorReadBulk() {
        final int STREAM_LENGTH = 50;
        final int CHUNK_LENGTH = 6;
        final int CHUNK_DELAY_MILLIS = 200;
        final int BUFF_LENGTH = 7;
        
        System.out.println("\ntestSimulatorReadBulk {");
        InputStreamSimulator simulator = new InputStreamSimulator(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        try {
            // Verify available.
            int a = simulator.available();
            System.out.println(String.format("Available: %1$d = %2$d", CHUNK_LENGTH, a));
            Assert.assertEquals(CHUNK_LENGTH, a);
            
            byte[] buff = new byte[BUFF_LENGTH];
            int totalRead = 0;
            
            while (!simulator.eof()) {
                int r = simulator.read(buff, 0, BUFF_LENGTH);
                totalRead += r;
                
                // Reading less than BUFF_LENGTH means we've reached EOF.
                // We'll be lied to to get the EOF.
                boolean ex;
                int ax;
                if (r < BUFF_LENGTH) {
                    ex = true;
                    ax = 0;
                }
                else {
                    ex = false;
                    ax = (totalRead % CHUNK_LENGTH) == 0 ? 0 : CHUNK_LENGTH - (totalRead % CHUNK_LENGTH); 
                }
                
                // Verify eof
                boolean e = simulator.eof(); 
                System.out.println(String.format("EOF: %1$b = %2$b", ex, e));
                Assert.assertEquals(ex, e);
                        
                // Verify available.
                a = simulator.available();
                System.out.println(String.format("Available: %1$d = %2$d", ax, a));
                Assert.assertEquals(ax, a);
            }
            
            // Verify available
            a = simulator.available();
            System.out.println(String.format("Available: 0 = %1$d", a));
            Assert.assertEquals(0, a);

            // Verify the number of bytes read.
            System.out.println(String.format("Stream length: %1$d = %2$d", STREAM_LENGTH, totalRead));
            Assert.assertEquals(STREAM_LENGTH, totalRead);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        finally {
            System.out.println("} // testSimulatorReadBulk");
        }
    }

    @Test
    public void testSimulatorRead1() {
        final int STREAM_LENGTH = 20;
        final int CHUNK_LENGTH = 3;
        final int CHUNK_DELAY_MILLIS = 200;
        
        System.out.println("\ntestSimulatorRead1 {");
        InputStreamSimulator simulator = new InputStreamSimulator(STREAM_LENGTH, CHUNK_LENGTH, CHUNK_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        try {
            // Verify available.
            int a = simulator.available();
            System.out.println(String.format("Available: %1$d = %2$d", CHUNK_LENGTH, a));
            Assert.assertEquals(CHUNK_LENGTH, a);

            long currentTimeMillis = System.currentTimeMillis();
            int i = 0;
            while (!simulator.eof()) {
                // Read.
                long lastTimeMillis = currentTimeMillis;
                int r = simulator.read();
                currentTimeMillis = System.currentTimeMillis();

                // Verify delay if on chunk boundary.
                if (i != 0 && (i % CHUNK_LENGTH == 0)) {
                    System.out.println(String.format("Delay: %1$d >= %2$d", currentTimeMillis - lastTimeMillis, CHUNK_DELAY_MILLIS));
                    Assert.assertTrue(currentTimeMillis - lastTimeMillis >= CHUNK_DELAY_MILLIS);
                }
                
                // Verify te read byte.
                System.out.println(String.format("[%1$d] Byte[%2$d]: %3$d = %4$d", System.currentTimeMillis() % 100000, i, InputStreamSimulator.CONTENT_BYTES[i % InputStreamSimulator.CONTENT_BYTES_LENGTH], r));
                Assert.assertEquals(InputStreamSimulator.CONTENT_BYTES[i % InputStreamSimulator.CONTENT_BYTES_LENGTH], r);
                
                // Verify available.
                a = simulator.available();
                int ax = simulator.eof() || ((i + 1) % CHUNK_LENGTH) == 0 ? 0 : (CHUNK_LENGTH - ((i + 1) % CHUNK_LENGTH));
                System.out.println(String.format("Available: %1$d = %2$d", ax, a));
                Assert.assertEquals(ax, a);

                // Count the read byte.
                i++;
            }
            
            // Verify available
            a = simulator.available();
            System.out.println(String.format("Available: 0 = %1$d", a));
            Assert.assertEquals(0, a);

            // Verify the number of bytes read.
            System.out.println(String.format("Stream length: %1$d = %2$d", STREAM_LENGTH, i));
            Assert.assertEquals(STREAM_LENGTH, i);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
        finally {
            System.out.println("} // testSimulatorRead1");
        }
    }
}
