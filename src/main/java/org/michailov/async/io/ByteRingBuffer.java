/**
 * Copyright (c) Zlatko Michailov
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.michailov.async.io;

/**
 * This is a concrete implementation of {@link RingBuffer} of a byte[].
 * 
 * @author Zlatko Michailov
 */
public class ByteRingBuffer extends RingBuffer {

    private final byte[] _buffer;
    
    /**
     * Constructs a ring buffer over an implicitly allocated byte[].
     *  
     * @param   capacity    Capacity of the implicit byte[].
     */
    public ByteRingBuffer(int capacity) {
        this(new byte[capacity]);
    }
    
    /**
     * Constructs a ring buffer over an existing byte[].
     * This overload is useful when the caller wants to reuse existing arrays
     * to avoid unnecessary heap allocations.
     *  
     * @param   buffer    An existing byte[].
     */
    public ByteRingBuffer(byte[] buffer) {
        super(buffer.length);
        
        _buffer = buffer;
    }
    
    /**
     * Gets the underlying byte[].
     * This may be passed to buffer copying APIs along with {@link RingBuffer#getAvailableToReadStraight}
     * or {@link RingBuffer#getAvailableToWriteStraight}. 
     * 
     * @return  The underlying byte[].
     */
    public byte[] getBuffer() {
        return _buffer;
    }
    
    /**
     * Gets the byte at a given relative position ahead of the current virtual read position.
     *  
     * @param   delta   A relative position ahead of the virtual read position to peek at.
     * @return          The value of the byte at the peek position or <b>-1</b> if <i>delta</i>
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
        int b = _buffer[i];
        return b;
    }
    
    /**
     * Reads the byte at the virtual read position, and advances the virtual read position by 1.
     * 
     * @return      The byte at the virtual read position or <b>-1</b> if nothing is available to read.
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
     * Writes a byte at the virtual write position, and advances the virtual write position by 1.
     * 
     * @param   b   Byte to be written.   
     * @return      The byte that was written or <b>-1</b> if no space was available to write.
     */
    public int write(byte b) {
        if (getAvailableToWrite() <= 0) {
            return Util.EOF;
        }
        
        int i = getWritePosition();
        _buffer[i] = b;

        advanceWritePosition(1);
        return b;
    }
}
