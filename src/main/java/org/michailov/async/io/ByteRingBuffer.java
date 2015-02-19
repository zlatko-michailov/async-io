package org.michailov.async.io;

public class ByteRingBuffer extends RingBuffer {

    private static final int NOT_AVAILABLE = -1;
    
    byte[] _buffer;
    
    public ByteRingBuffer(byte[] buffer) {
        super(buffer.length);
    }
    
    public byte[] getBuffer() {
        return _buffer;
    }
    
    
    public int peek(int delta) {
        if (getAvailableToRead() < delta) {
            return NOT_AVAILABLE;
        }
        
        int i = (getReadPosition() + delta) % getBufferLength();
        int b = _buffer[i];
        return b;
    }
    
    public int read() {
        int p = peek(1);
        if (p == NOT_AVAILABLE) {
            return NOT_AVAILABLE;
        }
        
        advanceReadPosition(1);
        return p;
    }
    
    public int write(byte b) {
        if (getAvailableToWrite() < 1) {
            return NOT_AVAILABLE;
        }
        
        int i = (getWritePosition() + 1) % getBufferLength();
        _buffer[i] = b;
        return b;
    }
}
