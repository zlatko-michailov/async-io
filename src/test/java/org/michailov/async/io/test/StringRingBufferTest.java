package org.michailov.async.io.test;

import org.junit.*;
import org.michailov.async.io.*;

public class StringRingBufferTest {

    @Test
    public void testCombinations() {
        final int BUFF_LENGTH = 5;
        
        String[] buff = new String[BUFF_LENGTH];
        StringRingBuffer ring = new StringRingBuffer(buff);
        
        // Initial
        Assert.assertEquals(null, ring.read());
        Assert.assertEquals(0, ring.getReadPosition());
        Assert.assertEquals(0, ring.getWritePosition());
        Assert.assertEquals(0, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToWrite());
        Assert.assertEquals(0, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToWriteStraight());
        Assert.assertEquals(0, ring.getTotalReadCount());
        Assert.assertEquals(0, ring.getTotalWriteCount());
        
        // Write 2 items
        Assert.assertEquals("ndb6", ring.write("ndb6"));
        Assert.assertEquals("tmo[0", ring.write("tmo[0"));
        Assert.assertEquals(0, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(2, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWrite());
        Assert.assertEquals(2, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWriteStraight());
        Assert.assertEquals(0, ring.getTotalReadCount());
        Assert.assertEquals(2, ring.getTotalWriteCount());
        
        // Read 2 items
        Assert.assertEquals("ndb6", ring.read());
        Assert.assertEquals("tmo[0", ring.read());
        Assert.assertEquals(2, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(0, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToWrite());
        Assert.assertEquals(0, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWriteStraight());
        Assert.assertEquals(null, ring.read());
        Assert.assertEquals(2, ring.getTotalReadCount());
        Assert.assertEquals(2, ring.getTotalWriteCount());
        
        // Write 5 items
        Assert.assertEquals("auv4", ring.write("auv4"));
        Assert.assertEquals("eish5", ring.write("eish5"));
        Assert.assertEquals("ндб6", ring.write("ндб6"));
        Assert.assertEquals("тмош0", ring.write("тмош0"));
        Assert.assertEquals("еисх5", ring.write("еисх5"));
        Assert.assertEquals(2, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToRead());
        Assert.assertEquals(0, ring.getAvailableToWrite());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToReadStraight());
        Assert.assertEquals(0, ring.getAvailableToWriteStraight());
        Assert.assertEquals(null, ring.write("good luck!"));
        Assert.assertEquals(2, ring.getTotalReadCount());
        Assert.assertEquals(7, ring.getTotalWriteCount());
        
        // Read 5 items
        Assert.assertEquals("auv4", ring.read());
        Assert.assertEquals("eish5", ring.read());
        Assert.assertEquals("ндб6", ring.read());
        Assert.assertEquals("тмош0", ring.read());
        Assert.assertEquals("еисх5", ring.read());
        Assert.assertEquals(2, ring.getReadPosition());
        Assert.assertEquals(2, ring.getWritePosition());
        Assert.assertEquals(0, ring.getAvailableToRead());
        Assert.assertEquals(BUFF_LENGTH, ring.getAvailableToWrite());
        Assert.assertEquals(0, ring.getAvailableToReadStraight());
        Assert.assertEquals(BUFF_LENGTH - 2, ring.getAvailableToWriteStraight());
        Assert.assertEquals(null, ring.read());
        Assert.assertEquals(7, ring.getTotalReadCount());
        Assert.assertEquals(7, ring.getTotalWriteCount());
    }
}
