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
 * This is a concrete implementation of {@link RingBuffer} of a String[].
 * 
 * @author Zlatko Michailov
 */
public class StringRingBuffer extends RingBuffer {
    
    private final String[] _buffer;
    
    /**
     * Constructs a ring buffer over an implicitly allocated String[].
     *  
     * @param   capacity    Capacity of the implicit String[].
     */
    public StringRingBuffer(int capacity) {
        this(new String[capacity]);
    }
    
    /**
     * Constructs a ring buffer over an existing String[].
     * This overload is useful when the caller wants to reuse existing arrays
     * to avoid unnecessary heap allocations.
     *  
     * @param   buffer    An existing String[].
     */
    public StringRingBuffer(String[] buffer) {
        super(buffer.length);
        
        _buffer = buffer;
    }
    
    /**
     * Gets the underlying String[].
     * This may be passed to buffer copying APIs along with {@link RingBuffer#getAvailableToReadStraight}
     * or {@link RingBuffer#getAvailableToWriteStraight}. 
     * 
     * @return  The underlying String[].
     */
    public String[] getBuffer() {
        return _buffer;
    }
    
    /**
     * Gets the String at a given relative position ahead of the current virtual read position.
     *  
     * @param   delta   A relative position ahead of the virtual read position to peek at.
     * @return          The String at the peek position or <b>null</b> if <i>delta</i>
     *                  is greater than {@link RingBuffer#getAvailableToRead}. 
     */
    public String peek(int delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("Argument delta may not be negative.");
        }
        
        if (getAvailableToRead() <= delta) {
            return null;
        }
        
        int i = (getReadPosition() + delta) % getBufferLength();
        String s = _buffer[i];
        return s;
    }
    
    /**
     * Reads the String at the virtual read position, and advances the virtual read position by 1.
     * 
     * @return      The String at the virtual read position or <b>null</b> if nothing is available to read.
     */
    public String read() {
        String p = peek(0);
        if (p == null) {
            return null;
        }
        
        advanceReadPosition(1);
        return p;
    }
    
    /**
     * Writes a String at the virtual write position, and advances the virtual write position by 1.
     * 
     * @param   s   String to be written.   
     * @return      The String that was written or <b>null</b> if no space was available to write.
     */
    public String write(String s) {
        if (getAvailableToWrite() <= 0) {
            return null;
        }
        
        int i = getWritePosition();
        _buffer[i] = s;

        advanceWritePosition(1);
        return s;
    }
}
