package org.michailov.async.io;

/**
 * This class defines an abstract ring buffer functionality where a virtual read position 
 * and a virtual write position chase each other around a buffer length.
 * The type of the items in the buffer is irrelevant to this functionality.
 * <p>
 * The virtual write position is always "ahead" of the virtual read position. 
 * That is because something has to be written first before it could be read.
 * <p>
 * <u>Thread safety</u>: This functionality is safe to be used by one "reader" thread
 * and one "writer" thread concurrently as long as the reader thread only updates
 * the virtual read position (it's OK to get the virtual write position) and the "writer"
 * thread only updates the virtual write position (it's OK to get the virtual read position.)
 * 
 * @author Zlatko Michailov
 */
public abstract class RingBuffer {

    private final long _bufferLength;
    private volatile long _virtualReadPosition;
    private volatile long _virtualWritePosition;
    private volatile boolean _eof;
    
    /**
     * Constructor to be called from concrete implementations.
     * 
     * @param bufferLength      Length of the underlying buffer.
     */
    protected RingBuffer(int bufferLength) {
        _bufferLength = bufferLength;
        _virtualReadPosition = 0;
        _virtualWritePosition = 0;
        _eof = false;
    }
    
    /**
     * Gets the EOF state of this ring buffer.
     * The EOF state is explicitly set by a writing client. 
     * 
     * @return  ture iff no more bytes will be written to this ring buffer.
     */
    public boolean getEOF() {
        return _eof;
    }
    
    /**
     * Sets the EOF state of this ring buffer.
     * The EOF state is explicitly set by a writing client. 
     */
    public void setEOF() {
        _eof = true;
    }
    
    /**
     * Gets the length of the underlying buffer.
     * 
     * @return  The length of the underlying buffer.
     */
    public int getBufferLength() {
        return (int)_bufferLength;
    }
    
    /**
     * Gets the current virtual read position.
     * 
     * @return  The current virtual read position.
     */
    public int getReadPosition() {
        return (int)(_virtualReadPosition % _bufferLength);
    }
    
    /**
     * Gets the current virtual write position.
     * 
     * @return  The current virtual write position.
     */
    public int getWritePosition() {
        return (int)(_virtualWritePosition % _bufferLength);
    }
    
    /**
     * Gets the total count of read items.
     * 
     * @return  The total count of read items.
     */
    public long getTotalReadCount() {
        return _virtualReadPosition;
    }
    
    /**
     * Gets the total count of written items.
     * 
     * @return  The total count of written items.
     */
    public long getTotalWriteCount() {
        return _virtualWritePosition;
    }
    
    /**
     * Gets the count of items that are available to be read.
     * Reading these items may include a flip over, i.e. these items can be read
     * one by one, but not necessarily all at once.
     * To get the items that can be read at once, see {@link #getAvailableToReadStraight}.
     * 
     * @return  The count of items available to be read one by one.
     */
    public int getAvailableToRead() {
        return (int)(_virtualWritePosition - _virtualReadPosition);
    }
    
    /**
     * Gets the count of items that are available to be read at once. 
     * This method is useful to be used with APIs that do buffer copying.
     * This count is less than or equal to the count returned from {@link #getAvailableToRead}.
     * 
     * @return  The count of items available to be read at once.
     */
    public int getAvailableToReadStraight() {
        return Math.min(getAvailableToRead(), (int)(_bufferLength - (_virtualReadPosition % _bufferLength)));
    }
    
    /**
     * Gets the count of items that are available to be written.
     * Writing these items may include a flip over, i.e. these items can be written
     * one by one, but not necessarily all at once.
     * To get the items that can be written at once, see {@link #getAvailableToWriteStraight}.
     * 
     * @return  The count of items available to be written one by one.
     */
    public int getAvailableToWrite() {
        return (int)(_bufferLength - (_virtualWritePosition - _virtualReadPosition));
    }
    
    /**
     * Gets the count of items that are available to be written at once. 
     * This method is useful to be used with APIs that do buffer copying.
     * This count is less than or equal to the count returned from {@link #getAvailableToWrite}.
     * 
     * @return  The count of items available to be written at once.
     */
    public int getAvailableToWriteStraight() {
        return Math.min(getAvailableToWrite(), (int)(_bufferLength - (_virtualWritePosition % _bufferLength)));
    }
    
    /**
     * This method must be called to indicate a successful read operation.
     * If multiple items are read one by one, it is not necessary to advance the read position
     * after each item. That could be done once for all read items.
     * <p> 
     * An attempt to advance the read position beyond the value returned by {@link #getAvailableToRead}
     * limits the attempt to the value of {@link #getAvailableToRead}.
     * That is not an exceptional case.
     * 
     * @param   delta   The count of virtual notches by which to increment the virtual read position. 
     * @return          The minimum of the requested <i>delta</i> and the value of {@link #getAvailableToRead}. 
     */
    public int advanceReadPosition(int delta) {
        int availableDelta = getAvailableDeltaToAdvance(delta, getAvailableToRead());
        _virtualReadPosition += availableDelta;
        return availableDelta;
    }
    
    /**
     * This method must be called to indicate a successful write operation.
     * If multiple items are written one by one, it is not necessary to advance the write position
     * after each item. That could be done once for all written items.
     * <p> 
     * An attempt to advance the write position beyond the value returned by {@link #getAvailableToWrite}
     * limits the attempt to the value of {@link #getAvailableToWrite}.
     * That is not an exceptional case.
     * 
     * @param   delta   The count of virtual notches by which to increment the virtual write position. 
     * @return          The minimum of the requested <i>delta</i> and the value of {@link #getAvailableToWrite}. 
     */
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
