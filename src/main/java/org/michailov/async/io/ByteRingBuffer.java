package org.michailov.async.io;

public class ByteRingBuffer extends RingBuffer {

    private static final int NOT_AVAILABLE = -1;
    
    byte[] _buffer;
    
    public ByteRingBuffer(int capacity) {
        this(new byte[capacity]);
    }
    
    public ByteRingBuffer(byte[] buffer) {
        super(buffer.length);
        
        _buffer = buffer;
    }
    
    public byte[] getBuffer() {
        return _buffer;
    }
    
    public int peek(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("Argument delta may not be negative.");
        }
        
        if (getAvailableToRead() <= delta) {
            return NOT_AVAILABLE;
        }
        
        int i = (getReadPosition() + delta) % getBufferLength();
        int b = _buffer[i];
        return b;
    }
    
    public int read() {
        int p = peek(0);
        if (p == NOT_AVAILABLE) {
            return NOT_AVAILABLE;
        }
        
        advanceReadPosition(1);
        return p;
    }
    
    public int write(byte b) {
        if (getAvailableToWrite() <= 0) {
            return NOT_AVAILABLE;
        }
        
        int i = getWritePosition();
        _buffer[i] = b;

        advanceWritePosition(1);
        return b;
    }
}
