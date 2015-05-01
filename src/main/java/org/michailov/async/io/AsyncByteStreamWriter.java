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

import java.io.*;
import org.michailov.async.*;

/**
 * Asynchronous byte writer over a plain old OutputStream.
 * <p>
 * The stream bytes are read from a {@link ByteRingBuffer}. 
 * <p>
 * Note: The caller is responsible for opening and closing the stream as needed. 
 * 
 * @see     WhenReady
 * @see     AsyncAgent
 * @see     ByteRingBuffer
 * 
 * @author  Zlatko Michailov
 */
public class AsyncByteStreamWriter extends AsyncAgent {
    
    private final OutputStream _outputStream;
    private final ByteRingBuffer _byteRingBuffer;
    private boolean _isEOF;
    
    /**
     * Constructs a new AsyncByteStreamWriter instance to write to the given OutputStream
     * from the given {@link ByteRingBuffer}.
     * 
     * @param   outputStream    An OutputStream to write to.
     * @param   byteRingBuffer  A {@link ByteRingBuffer} to read from.
     * @param   asyncOptions    {@link AsyncOptions} that will control all async operations on this instance. 
     */
    public AsyncByteStreamWriter(OutputStream outputStream, ByteRingBuffer byteRingBuffer, AsyncOptions asyncOptions) {
        super(asyncOptions);
        
        Util.ensureArgumentNotNull("outputStream", outputStream);
        Util.ensureArgumentNotNull("byteRingBuffer", byteRingBuffer);
        
        _outputStream = outputStream;
        _byteRingBuffer = byteRingBuffer;
        _isEOF = false;
    }
    
    /**
     * Returns the underlying OutputStream.
     * 
     * @return  The underlying OutputStream.
     */
    public OutputStream getOutputStream() {
        return _outputStream;
    }
    
    /**
     * Returns the attached ring buffer where bytes will be read from.
     * 
     * @return  The attached ring buffer where bytes will be read from.
     */
    public ByteRingBuffer getByteRingBuffer() {
        return _byteRingBuffer;
    }
    
    /**
     * Checks whether this writer has reached EOF of the byte ring buffer.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        return _isEOF || (_byteRingBuffer.isEOF() && _byteRingBuffer.getAvailableToRead() == 0);
    }

    /**
     * "<i>ready</i>" predicate that returns true when bytes can be read from the ring buffer without blocking.
     * <p>
     * Notice that OutputStream, unlike InputStream, has no way to report how many bytes can be written without blocking.   
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            isReady = !_isEOF && _byteRingBuffer.getAvailableToRead() > 0;
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
        
        return isReady;
    }
    
    @Override
    /**
     * "<i>done</i>" predicate that returns true when this async agent wants to quit the current async loop.   
     */
    protected boolean done() {
        return isEOF();
    }
    
    /**
     * "<i>action</i>" function that reads bytes from the ring buffer and writes them to the stream.   
     */
    @Override
    protected void action() {
        try {
            int availableByteCount = _byteRingBuffer.getAvailableToReadStraight();
            _outputStream.write(_byteRingBuffer.getBuffer(), _byteRingBuffer.getReadPosition(), availableByteCount);
            
            // Bytes were read from the ring buffer and written to the stream.
            // Advance the ring buffer's read position.
            _byteRingBuffer.advanceReadPosition(availableByteCount);
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
    }
    
    /**
     * Sets the EOF status on this instance to true. 
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this instance as 'idle'.
     * 
     * @param   ex  An exception to complete with.
     */
    private void setEOFAndThrow(Throwable ex) {
        _isEOF = true;
        setIdleAndThrow(ex);
    }
    
}
