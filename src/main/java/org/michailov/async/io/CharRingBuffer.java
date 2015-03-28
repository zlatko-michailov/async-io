package org.michailov.async.io;

public class CharRingBuffer extends RingBuffer {

    private final char[] _buffer;
    
    public CharRingBuffer(int capacity) {
        this(new char[capacity]);
    }
    
    public CharRingBuffer(char[] buffer) {
        super(buffer.length);
        
        _buffer = buffer;
    }
    
    public char[] getBuffer() {
        return _buffer;
    }
    
    public int peek(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("Argument delta may not be negative.");
        }
        
        if (getAvailableToRead() <= delta) {
            return Util.EOF;
        }
        
        int i = (getReadPosition() + delta) % getBufferLength();
        int c = _buffer[i];
        return c;
    }
    
    public int read() {
        int p = peek(0);
        if (p == Util.EOF) {
            return Util.EOF;
        }
        
        advanceReadPosition(1);
        return p;
    }
    
    public int write(char c) {
        if (getAvailableToWrite() <= 0) {
            return Util.EOF;
        }
        
        int i = getWritePosition();
        _buffer[i] = c;

        advanceWritePosition(1);
        return c;
    }
}
