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
import org.michailov.async.io.*;

public class ByteRingBufferTest {

    @Test
    public void testCombinations() {
        final int BUFF_LENGTH = 5;
        final int EOF = -1;
        
        byte[] buff = new byte[BUFF_LENGTH];
        ByteRingBuffer ring = new ByteRingBuffer(buff);
        
        // Initial
        Assert.assertEquals(EOF, ring.read());
        Assert.assertEquals(0, ring.getReadPosition());
        Assert.assertEquals(0, ring.getWritePosition());
        Assert.assertEquals(0, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToWrite());
        Assert.assertEquals(0, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToWriteStraight());
        Assert.assertEquals(0, ring.getTotalReadCount());
        Assert.assertEquals(0, ring.getTotalWriteCount());
        
        // Write 2 items
        Assert.assertEquals((byte)42, ring.write((byte)42));
        Assert.assertEquals((byte)43, ring.write((byte)43));
        Assert.assertEquals(0, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(2, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWrite());
        Assert.assertEquals(2, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWriteStraight());
        Assert.assertEquals(0, ring.getTotalReadCount());
        Assert.assertEquals(2, ring.getTotalWriteCount());
        
        // Read 2 items
        Assert.assertEquals((byte)42, ring.read());
        Assert.assertEquals((byte)43, ring.read());
        Assert.assertEquals(2, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(0, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToWrite());
        Assert.assertEquals(0, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWriteStraight());
        Assert.assertEquals(EOF, ring.read());
        Assert.assertEquals(2, ring.getTotalReadCount());
        Assert.assertEquals(2, ring.getTotalWriteCount());
        
        // Write 5 items
        Assert.assertEquals((byte)62, ring.write((byte)62));
        Assert.assertEquals((byte)63, ring.write((byte)63));
        Assert.assertEquals((byte)64, ring.write((byte)64));
        Assert.assertEquals((byte)65, ring.write((byte)65));
        Assert.assertEquals((byte)66, ring.write((byte)66));
        Assert.assertEquals(2, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToRead());
        Assert.assertEquals(0, ring.getAvailableToWrite());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToReadStraight());
        Assert.assertEquals(0, ring.getAvailableToWriteStraight());
        Assert.assertEquals(EOF, ring.write((byte)67));
        Assert.assertEquals(2, ring.getTotalReadCount());
        Assert.assertEquals(7, ring.getTotalWriteCount());
        
        // Read 5 items
        Assert.assertEquals((byte)62, ring.read());
        Assert.assertEquals((byte)63, ring.read());
        Assert.assertEquals((byte)64, ring.read());
        Assert.assertEquals((byte)65, ring.read());
        Assert.assertEquals((byte)66, ring.read());
        Assert.assertEquals(2, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(0, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToWrite());
        Assert.assertEquals(0, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWriteStraight());
        Assert.assertEquals(EOF, ring.read());
        Assert.assertEquals(7, ring.getTotalReadCount());
        Assert.assertEquals(7, ring.getTotalWriteCount());
    }

}
