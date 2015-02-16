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
            int r = simulator.read(buff, 0, BUFF_LENGTH);
            
            while (r != -1) {
                int ax;
                totalRead += r;

                // Reading less than BUFF_LENGTH means we've reached EOF.
                // We'll be lied to to get the EOF.
                if (r < BUFF_LENGTH) {
                    ax = 1;
                }
                else {
                    ax = (totalRead % CHUNK_LENGTH) == 0 ? 0 : CHUNK_LENGTH - (totalRead % CHUNK_LENGTH); 
                }
                
                // Verify available.
                a = simulator.available();
                System.out.println(String.format("Available: %1$d = %2$d", ax, a));
                Assert.assertEquals(ax, a);
                
                // Read forward.
                r = simulator.read(buff, 0, BUFF_LENGTH);
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
        
        System.out.println("} // testSimulatorReadBulk");
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
            
            int i = 0;
            int r = simulator.read();
            long currentTimeMillis = System.currentTimeMillis();
            long lastTimeMillis;

            while (r != -1) {
                // Verify each read byte.
                System.out.println(String.format("[%1$d] Byte[%2$d]: %3$d = %4$d", System.currentTimeMillis() % 100000, i, InputStreamSimulator.CONTENT_BYTES[i % InputStreamSimulator.CONTENT_BYTES_LENGTH], r));
                Assert.assertEquals(InputStreamSimulator.CONTENT_BYTES[i % InputStreamSimulator.CONTENT_BYTES_LENGTH], r);
                
                // Verify available.
                a = simulator.available();
                int ax = ((i + 1) % CHUNK_LENGTH) == 0 ? 0 : (CHUNK_LENGTH - ((i + 1) % CHUNK_LENGTH));
                System.out.println(String.format("Available: %1$d = %2$d", ax, a));
                Assert.assertEquals(ax, a);

                // Read forward.
                i++;
                r = simulator.read();
                lastTimeMillis = currentTimeMillis;
                currentTimeMillis = System.currentTimeMillis();
                
                // Verify delay if on chunk boundary.
                if (i % CHUNK_LENGTH == 0) {
                    System.out.println(String.format("Delay: %1$d >= %2$d", currentTimeMillis - lastTimeMillis, CHUNK_DELAY_MILLIS));
                    Assert.assertTrue(currentTimeMillis - lastTimeMillis >= CHUNK_DELAY_MILLIS);
                }
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
        
        System.out.println("} // testSimulatorRead1");
    }
}
