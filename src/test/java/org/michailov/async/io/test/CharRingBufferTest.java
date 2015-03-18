package org.michailov.async.io.test;

import org.junit.*;
import org.michailov.async.io.*;

public class CharRingBufferTest {

    @Test
    public void testCombinations() {
        final int BUFF_LENGTH = 5;
        final int EOF = -1;
        
        char[] buff = new char[BUFF_LENGTH];
        CharRingBuffer ring = new CharRingBuffer(buff);
        
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
        Assert.assertEquals('q', ring.write('q'));
        Assert.assertEquals('R', ring.write('R'));
        Assert.assertEquals(0, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(2, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWrite());
        Assert.assertEquals(2, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWriteStraight());
        Assert.assertEquals(0, ring.getTotalReadCount());
        Assert.assertEquals(2, ring.getTotalWriteCount());
        
        // Read 2 items
        Assert.assertEquals('q', ring.read());
        Assert.assertEquals('R', ring.read());
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
        Assert.assertEquals('s', ring.write('s'));
        Assert.assertEquals('T', ring.write('T'));
        Assert.assertEquals('u', ring.write('u'));
        Assert.assertEquals('V', ring.write('V'));
        Assert.assertEquals('w', ring.write('w'));
        Assert.assertEquals(2, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToRead());
        Assert.assertEquals(0, ring.getAvailableToWrite());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToReadStraight());
        Assert.assertEquals(0, ring.getAvailableToWriteStraight());
        Assert.assertEquals(EOF, ring.write('X'));
        Assert.assertEquals(2, ring.getTotalReadCount());
        Assert.assertEquals(7, ring.getTotalWriteCount());
        
        // Read 5 items
        Assert.assertEquals('s', ring.read());
        Assert.assertEquals('T', ring.read());
        Assert.assertEquals('u', ring.read());
        Assert.assertEquals('V', ring.read());
        Assert.assertEquals('w', ring.read());
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
