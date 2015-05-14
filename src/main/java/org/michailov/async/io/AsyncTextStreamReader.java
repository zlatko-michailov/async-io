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
 * Asynchronous text line reader over a plain old InputStream.
 * The reader doesn't block as long as the stream correctly reports its available bytes.
 * <p>
 * The reader reads available bytes from the stream if there are any,
 * decodes the chars, splits lines, and places complete lines in a {@link StringRingBuffer}
 * that is accessible through {@link #getStringRingBuffer}.
 * <p>
 * Use {@link AsyncAgent#applyAsync} or {@link AsyncAgent#startApplyLoopAsync} to
 * read lines through {@link #getStringRingBuffer}.
 * <p>
 * This class is a convenience wrapper around the individual elements of the input stack. 
 * <p>
 * Note: The caller is responsible for opening and closing the stream as needed. 
 * 
 * @see     WhenReady
 * @see     AsyncAgent
 * @see     ByteRingBuffer
 * @see     CharRingBuffer
 * @see     AsyncLineSplitter
 * 
 * @author  Zlatko Michailov
 */
public class AsyncTextStreamReader  extends AsyncAgent {
    
    private final AsyncByteStreamReader _asyncByteStreamReader;
    private final AsyncCharDecoder _asyncCharDecoder;
    private final AsyncLineSplitter _asyncLineSplitter;
    private final StringRingBuffer _stringRingBuffer;
    private boolean _isEOF;
    private boolean _areLoopsStarted;

    /**
     * Constructs a new AsyncTextStreamReader instance to read from the given InputStream.
     * 
     * @param   inputStream                 An InputStream to read from.
     * @param   textStreamAsyncOptions      {@link TextStreamAsyncOptions} that will control all async operations on this instance. 
     */
    public AsyncTextStreamReader(InputStream inputStream, TextStreamAsyncOptions textStreamAsyncOptions) {
        this(inputStream, null, null, null, textStreamAsyncOptions);
    }
    
    /**
     * Constructs a new AsyncTextStreamReader instance to read from the given InputStream.
     * 
     * @param   inputStream                 An InputStream to read from.
     * @param   byteRingBuffer              A {@link ByteRingBuffer} to write bytes to. May be {@code null}. 
     *                                      In that case, an implicit {@link ByteRingBuffer} is created. 
     * @param   charRingBuffer              A {@link CharRingBuffer} to decode chars into. May be {@code null}. 
     *                                      In that case, an implicit {@link CharRingBuffer} is created. 
     * @param   stringRingBuffer            A {@link StringRingBuffer} to place lines into. May be {@code null}. 
     *                                      In that case, an implicit {@link StringRingBuffer} is created. 
     * @param   textStreamAsyncOptions      {@link TextStreamAsyncOptions} that will control all async operations on this instance. 
     */
    public AsyncTextStreamReader(InputStream inputStream, ByteRingBuffer byteRingBuffer, CharRingBuffer charRingBuffer, StringRingBuffer stringRingBuffer, TextStreamAsyncOptions textStreamAsyncOptions) {
        super(textStreamAsyncOptions);
        
        Util.ensureArgumentNotNull("inputStream", inputStream);
        Util.ensureArgumentNotNull("textStreamAsyncOptions", textStreamAsyncOptions);

        if (byteRingBuffer == null) {
            byteRingBuffer = new ByteRingBuffer(textStreamAsyncOptions.byteRingBufferCapacity);
        }
        
        if (charRingBuffer == null) {
            charRingBuffer = new CharRingBuffer(textStreamAsyncOptions.charRingBufferCapacity);
        }
        
        if (stringRingBuffer == null) {
            stringRingBuffer = new StringRingBuffer(textStreamAsyncOptions.stringRingBufferCapacity);
        }
        
        _asyncByteStreamReader = new AsyncByteStreamReader(inputStream, byteRingBuffer, textStreamAsyncOptions);
        _asyncCharDecoder = new AsyncCharDecoder(byteRingBuffer, charRingBuffer, textStreamAsyncOptions);
        _asyncLineSplitter = new AsyncLineSplitter(charRingBuffer, stringRingBuffer, textStreamAsyncOptions);
        _stringRingBuffer = stringRingBuffer;
        _isEOF = false;
        _areLoopsStarted = false;
    }

    /**
     * Returns the underlying {@link StringRingBuffer} where text lines are placed. 
     * 
     * @return  The underlying {@link StringRingBuffer}.
     */
    public StringRingBuffer getStringRingBuffer() {
        return _stringRingBuffer;
    }
    
    /**
     * Checks whether this text stream reader has reached EOF.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        return _isEOF;
    }

    /**
     * "<i>ready</i>" predicate that returns true when lines can be read from the string ring buffer.   
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            if (!_areLoopsStarted) {
                _asyncByteStreamReader.startApplyLoopAsync();
                _asyncCharDecoder.startApplyLoopAsync();
                _asyncLineSplitter.startApplyLoopAsync();
                _areLoopsStarted = true;
            }
            
            isReady = !isEOF() && _stringRingBuffer.getAvailableToRead() > 0;
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
            isDone = isEOF() || (_stringRingBuffer.isEOF() && _stringRingBuffer.getAvailableToRead() == 0);
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
     * "<i>action</i>" function that gets executed as part of the apply loop.   
     */
    @Override
    protected void action() {
        // Nothing to do.
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying string ring buffer to true. 
     * Marks this instance as 'idle'.
     */
    private void setEOF() {
        _isEOF = true;
        setIdle();
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying string ring buffer to true. 
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
