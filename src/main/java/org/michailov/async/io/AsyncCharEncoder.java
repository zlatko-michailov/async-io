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

import java.nio.*;
import java.nio.charset.*;

import org.michailov.async.*;

/**
 * Asynchronous encoder of chars into bytes.
 * 
 * @see     WhenReady
 * @see     AsyncAgent
 * @see     ByteRingBuffer
 * @see     CharRingBuffer
 * 
 * @author  Zlatko Michailov
 */
public class AsyncCharEncoder extends AsyncAgent {

    private static final int MAX_CHAR_BYTES = 8; 
    private static final boolean ENCODE_AS_EOF = true;
    
    private final CharRingBuffer _charRingBuffer;
    private final ByteRingBuffer _byteRingBuffer;
    private final Charset _charset;
    private final CharsetEncoder _charsetEncoder;
    private final CharBuffer _mainCharBuffer;
    private final ByteBuffer _mainByteBuffer;
    private final ByteBuffer _tempByteBuffer;
    private boolean _isTempBufferDirty;
    
    /**
     * Constructs an AsyncCharEncoder instance over the given ring buffers.
     * 
     * @param charRingBuffer        {@link CharRingBuffer} to read chars from.
     * @param byteRingBuffer        {@link ByteRingBuffer} to write bytes to.
     * @param charsetAsyncOptions   {@link CharsetAsyncOptions} to use for all async operations.
     */
    public AsyncCharEncoder(CharRingBuffer charRingBuffer, ByteRingBuffer byteRingBuffer, CharsetAsyncOptions charsetAsyncOptions) {
        super(charsetAsyncOptions);
        
        Util.ensureArgumentNotNull("charRingBuffer", charRingBuffer);
        Util.ensureArgumentNotNull("byteRingBuffer", byteRingBuffer);
        Util.ensureArgumentNotNull("charsetAsyncOptions", charsetAsyncOptions);
        Util.ensureArgumentNotNull("charsetAsyncOptions.charset", charsetAsyncOptions.charset);
        
        _charRingBuffer = charRingBuffer;
        _byteRingBuffer = byteRingBuffer;
        _charset = charsetAsyncOptions.charset;
        _charsetEncoder = _charset.newEncoder();
        _mainCharBuffer = CharBuffer.wrap(_charRingBuffer.getBuffer());
        _mainByteBuffer = ByteBuffer.wrap(_byteRingBuffer.getBuffer());
        _tempByteBuffer = ByteBuffer.allocate(MAX_CHAR_BYTES);
        _isTempBufferDirty = false;
    }

    /**
     * Returns the attached char ring buffer from where chars will be read.
     * 
     * @return  The attached char ring buffer from where chars will be read.
     */
    public CharRingBuffer getCharRingBuffer() {
        return _charRingBuffer;
    }
    
    /**
     * Returns the attached byte ring buffer where bytes will be written.
     * 
     * @return  The attached byte ring buffer where bytes will be written.
     */
    public ByteRingBuffer getByteRingBuffer() {
        return _byteRingBuffer;
    }
    
    /**
     * Checks whether this decoder has reached EOF of the input char ring buffer.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        // We record the EOF on the output ring buffer.
        return _byteRingBuffer.isEOF();
    }

    /**
     * "<i>ready</i>" predicate that returns true when chars can be read from the input
     * char ring buffer and bytes can be written to the output byte ring buffer without blocking.   
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            isReady = !isEOF() && (_charRingBuffer.getAvailableToRead() > 0 || _isTempBufferDirty) && _byteRingBuffer.getAvailableToWrite() > 0;
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
        
        return isReady;
    }
    
    /**
     * "<i>done</i>" predicate that returns true when this async agent wants to quit the current async loop.   
     */
    @Override
    protected boolean done() {
        boolean isDone = true;
        
        try {
            isDone = isEOF() || (_charRingBuffer.isEOF() && _charRingBuffer.getAvailableToRead() == 0 && !_isTempBufferDirty);
            if (isDone) {
                setEOF();
            }
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
        
        return isDone;
    }
    
    /**
     * "<i>action</i>" function that reads chars from the input char ring buffer and writes bytes to the output byte ring buffer.   
     */
    @Override
    protected void action() {
        try {
            if (_isTempBufferDirty) {
                flushTempBuffer();
            }
            else {
                encodeToMainBuffer();
            }
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
    }
    
    /**
     * Copies as many bytes as possible from the temp buffer to the output byte buffer. 
     */
    private void flushTempBuffer() {
        // Write bytes one by one. 
        // We may bail out due to like of space in the output byte buffer before the entire temp buffer has been flushed.   
        while (_byteRingBuffer.getAvailableToWrite() > 0) {
            if (_tempByteBuffer.position() < _tempByteBuffer.limit()) {
                byte b = _tempByteBuffer.get();
                _byteRingBuffer.write(b);
            }
            else {
                // The temp buffer has been flushed.
                _isTempBufferDirty = false;
                break;
            }
        }
    }
    
    /**
     * Attempts to encode chars to the main byte buffer.
     */
    private void encodeToMainBuffer() {
        // Take a snapshot of the real buffers.
        int readPosition = _charRingBuffer.getReadPosition();
        int readStraight = _charRingBuffer.getAvailableToReadStraight();
        int writePosition = _byteRingBuffer.getWritePosition();
        int writeStraight = _byteRingBuffer.getAvailableToWriteStraight();

        // Map the virtual encoder buffers over the native buffers. 
        _mainCharBuffer.position(readPosition);
        _mainCharBuffer.limit(readPosition + readStraight);
        _mainByteBuffer.position(writePosition);
        _mainByteBuffer.limit(writePosition + writeStraight);
        
        // Attempt to encode.
        _charsetEncoder.reset();
        CoderResult coderResult = _charsetEncoder.encode(_mainCharBuffer, _mainByteBuffer, !ENCODE_AS_EOF);
        
        // Advance positions of physical buffers based on the state of the virtual buffers.
        int newReadPosition = _mainCharBuffer.position();
        int readDelta = newReadPosition - readPosition;
        if (readDelta > 0) {
            _charRingBuffer.advanceReadPosition(readDelta);
        }
        
        int newWritePosition = _mainByteBuffer.position();
        int writeDelta = newWritePosition - writePosition;
        if (writeDelta > 0) {
            _byteRingBuffer.advanceWritePosition(writeDelta);
        }
        
        // There is at least 1 char that couldn't get encoded due to insufficient space in the output byte buffer.
        // If the byte buffer is at its exact end, writing can continue from its beginning.
        // But if there is some remaining (but insufficient) space at the end of the byte buffer,
        // we must encode 1 char to the temp byte buffer, and then flush it, and then the process will continue.
        if (coderResult.isOverflow() && _mainByteBuffer.position() < _mainByteBuffer.capacity()) {
            encodeToTempBuffer();
        }
    }
    
    /**
     * Encodes 1 char to the temp byte buffer.
     */
    private void encodeToTempBuffer() {
        // Take a snapshot of the real char buffer.
        // (The real byte buffer is not needed now.)
        int readPosition = _charRingBuffer.getReadPosition();

        // Map the virtual decoder char buffer over the native char buffer. 
        _mainCharBuffer.position(readPosition);
        _mainCharBuffer.limit(readPosition + 1);
        
        // Reset the virtual temp buffer.
        _tempByteBuffer.clear();
        
        // Attempt to decode.
        _charsetEncoder.reset();
        CoderResult coderResult = _charsetEncoder.encode(_mainCharBuffer, _tempByteBuffer, ENCODE_AS_EOF);
        
        if (!coderResult.isError()) {
            // Adjust the position on the physical char buffer. 
            _charRingBuffer.advanceReadPosition(1);
            
            // Flip the temp buffer and mark it as dirty.
            _tempByteBuffer.flip();
            _isTempBufferDirty = true;
        }
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying output byte ring buffer to true. 
     * Marks this instance as 'idle'.
     */
    private void setEOF() {
        _byteRingBuffer.setEOF();
        setIdle();
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying output byte ring buffer to true. 
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this instance as 'idle'.
     * 
     * @param   ex  An exception to complete with.
     */
    private void setEOFAndThrow(Throwable ex) {
        _byteRingBuffer.setEOF();
        setIdleAndThrow(ex);
    }

}
