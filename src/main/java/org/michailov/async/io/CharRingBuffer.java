package org.michailov.async.io;

/**
 * This is a concrete implementation of {@link RingBuffer} of a char[].
 * 
 * @author Zlatko Michailov
 */
public class CharRingBuffer extends RingBuffer {

    private final char[] _buffer;
    
    /**
     * Constructs a ring buffer over an implicitly allocated char[].
     *  
     * @param   capacity    Capacity of the implicit char[].
     */
    public CharRingBuffer(int capacity) {
        this(new char[capacity]);
    }
    
    /**
     * Constructs a ring buffer over an existing char[].
     * This overload is useful when the caller wants to reuse existing arrays
     * to avoid unnecessary heap allocations.
     *  
     * @param   buffer    An existing char[].
     */
    public CharRingBuffer(char[] buffer) {
        super(buffer.length);
        
        _buffer = buffer;
    }
    
    /**
     * Gets the underlying char[].
     * This may be passed to buffer copying APIs along with {@link RingBuffer#getAvailableToReadStraight}
     * or {@link RingBuffer#getAvailableToWriteStraight}. 
     * 
     * @return  The underlying char[].
     */
    public char[] getBuffer() {
        return _buffer;
    }
    
    /**
     * Gets the char at a given relative position ahead of the current virtual read position.
     *  
     * @param   delta   A relative position ahead of the virtual read position to peek at.
     * @return          The value of the char at the peek position or <b>-1</b> if <i>delta</i>
     *                  is greater than {@link RingBuffer#getAvailableToRead}. 
     */
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
    
    /**
     * Reads the char at the virtual read position, and advances the virtual read position by 1.
     * 
     * @return      The char at the virtual read position or <b>-1</b> if nothing is available to read.
     */
    public int read() {
        int p = peek(0);
        if (p == Util.EOF) {
            return Util.EOF;
        }
        
        advanceReadPosition(1);
        return p;
    }
    
    /**
     * Writes a char at the virtual write position, and advances the virtual write position by 1.
     * 
     * @param   c   Char to be written.   
     * @return      The char that was written or <b>-1</b> if no space was available to write.
     */
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
