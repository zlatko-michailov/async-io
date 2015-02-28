package org.michailov.async.io;

public class CharRingBuffer extends RingBuffer {

    private static final int NOT_AVAILABLE = -1;
    
    char[] _buffer;
    
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
        if (getAvailableToRead() < delta) {
            return NOT_AVAILABLE;
        }
        
        int i = (getReadPosition() + delta) % getBufferLength();
        int c = _buffer[i];
        return c;
    }
    
    public int read() {
        int p = peek(1);
        if (p == NOT_AVAILABLE) {
            return NOT_AVAILABLE;
        }
        
        advanceReadPosition(1);
        return p;
    }
    
    public int write(char c) {
        if (getAvailableToWrite() < 1) {
            return NOT_AVAILABLE;
        }
        
        int i = (getWritePosition() + 1) % getBufferLength();
        _buffer[i] = c;

        advanceWritePosition(1);
        return c;
    }
}
