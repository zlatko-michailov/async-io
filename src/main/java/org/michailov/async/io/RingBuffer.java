package org.michailov.async.io;

abstract class RingBuffer {

    private final int _bufferLength;
    private volatile int _readPosition;
    private volatile int _writePosition;
    
    protected RingBuffer(int bufferLength) {
        _bufferLength = bufferLength;
        _readPosition = 0;
        _writePosition = 0;
    }
    
    public int getBufferLength() {
        return _bufferLength;
    }
    
    public int getReadPosition() {
        return _readPosition;
    }
    
    public int getWritePosition() {
        return _writePosition;
    }
    
    public int getAvailableToRead() {
        return getCalcWritePosition() - _readPosition;
    }
    
    public int getAvailableToWrite() {
        return getCalcReadPosition() - _writePosition;
    }
    
    public int advanceReadPosition(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("Parameter delta may not be negative.");
        }
        
        int availableDelta = Math.min(delta, getAvailableToRead());
        _readPosition = (_readPosition + availableDelta) % _bufferLength;
        return availableDelta;
    }
    
    public int advanceWritePosition(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("Parameter delta may not be negative.");
        }
        
        int availableDelta = Math.min(delta, getAvailableToWrite());
        _writePosition = (_writePosition + availableDelta) % _bufferLength;
        return availableDelta;
    }
    
    private int getCalcReadPosition() {
        return _writePosition <= _readPosition ? _readPosition : _bufferLength + _readPosition;
    }
    
    private int getCalcWritePosition() {
        return _readPosition <= _writePosition ? _writePosition : _bufferLength + _writePosition;
    }
}
