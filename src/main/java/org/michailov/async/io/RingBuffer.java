package org.michailov.async.io;

public abstract class RingBuffer {

    private final long _bufferLength;
    private volatile long _virtualReadPosition;
    private volatile long _virtualWritePosition;
    private volatile boolean _eof;
    
    protected RingBuffer(int bufferLength) {
        _bufferLength = bufferLength;
        _virtualReadPosition = 0;
        _virtualWritePosition = 0;
        _eof = false;
    }
    
    public boolean getEOF() {
        return _eof;
    }
    
    public void setEOF() {
        _eof = true;
    }
    
    public int getBufferLength() {
        return (int)_bufferLength;
    }
    
    public int getReadPosition() {
        return (int)(_virtualReadPosition % _bufferLength);
    }
    
    public int getWritePosition() {
        return (int)(_virtualWritePosition % _bufferLength);
    }
    
    public long getTotalReadCount() {
        return _virtualReadPosition;
    }
    
    public long getTotalWriteCount() {
        return _virtualWritePosition;
    }
    
    public int getAvailableToRead() {
        return (int)(_virtualWritePosition - _virtualReadPosition);
    }
    
    public int getAvailableToReadStraight() {
        return Math.min(getAvailableToRead(), (int)(_bufferLength - (_virtualReadPosition % _bufferLength)));
    }
    
    public int getAvailableToWrite() {
        return (int)(_bufferLength - (_virtualWritePosition - _virtualReadPosition));
    }
    
    public int getAvailableToWriteStraight() {
        return Math.min(getAvailableToWrite(), (int)(_bufferLength - (_virtualWritePosition % _bufferLength)));
    }
    
    public int advanceReadPosition(int delta) {
        int availableDelta = getAvailableDeltaToAdvance(delta, getAvailableToRead());
        _virtualReadPosition += availableDelta;
        return availableDelta;
    }
    
    public int advanceWritePosition(int delta) {
        int availableDelta = getAvailableDeltaToAdvance(delta, getAvailableToWrite());
        _virtualWritePosition += availableDelta;
        return availableDelta;
    }
    
    private int getAvailableDeltaToAdvance(int delta, int available) {
        if (delta < 0) {
            throw new IllegalArgumentException("Parameter delta may not be negative.");
        }
        
        return Math.min(delta, available);
    }
    
}
