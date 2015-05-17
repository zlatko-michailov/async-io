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
 * Asynchronous text line writer over a plain old OutputStream.
 * <p>
 * The writer converts available lines and appends the chosen line breaks, encodes the chars
 * into a {@link ByteRingBuffer}. Then those bytes are written into an OutputStream.
 * <p>
 * Use {@link AsyncAgent#applyAsync} or {@link AsyncAgent#startApplyLoopAsync} to
 * write lines through {@link #getStringRingBuffer}.
 * <p>
 * This class is a convenience wrapper around the individual elements of the output stack. 
 * <p>
 * Note: The caller is responsible for opening and closing the stream as needed. 
 * 
 * @see     WhenReady
 * @see     AsyncAgent
 * @see     ByteRingBuffer
 * @see     CharRingBuffer
 * @see     AsyncLineJoiner
 * 
 * @author  Zlatko Michailov
 */
public class AsyncTextStreamWriter extends AsyncAgent {

    private final StringRingBuffer _stringRingBuffer;
    private final AsyncLineJoiner _asyncLineJoiner;
    private final AsyncCharEncoder _asyncCharEncoder;
    private final AsyncByteStreamWriter _asyncByteStreamWriter;
    private boolean _areLoopsStarted;
    private boolean _isEOF;

    /**
     * Constructs a new AsyncTextStreamWriter instance to write to the given OutputStream.
     * 
     * @param   outputStream                An OutputStream to write into.
     * @param   textStreamAsyncOptions      {@link TextStreamAsyncOptions} that will control all async operations on this instance. 
     */
    public AsyncTextStreamWriter(OutputStream outputStream, TextStreamAsyncOptions textStreamAsyncOptions) {
        this(outputStream, null, null, null, textStreamAsyncOptions);
    }
    
    /**
     * Constructs a new AsyncTextStreamWriter instance to write into the given OutputStream.
     * 
     * @param   outputStream                An OutputStream to write into.
     * @param   stringRingBuffer            A {@link StringRingBuffer} to read lines from. May be {@code null}. 
     *                                      In that case, an implicit {@link StringRingBuffer} is created. 
     * @param   charRingBuffer              A {@link CharRingBuffer} to join lines into. May be {@code null}. 
     *                                      In that case, an implicit {@link CharRingBuffer} is created. 
     * @param   byteRingBuffer              A {@link ByteRingBuffer} to encode chars to. May be {@code null}. 
     *                                      In that case, an implicit {@link ByteRingBuffer} is created. 
     * @param   textStreamAsyncOptions      {@link TextStreamAsyncOptions} that will control all async operations on this instance. 
     */
    public AsyncTextStreamWriter(OutputStream outputStream, StringRingBuffer stringRingBuffer, CharRingBuffer charRingBuffer, ByteRingBuffer byteRingBuffer, TextStreamAsyncOptions textStreamAsyncOptions) {
        super(textStreamAsyncOptions);
        
        Util.ensureArgumentNotNull("outputStream", outputStream);
        Util.ensureArgumentNotNull("textStreamAsyncOptions", textStreamAsyncOptions);

        if (stringRingBuffer == null) {
            stringRingBuffer = new StringRingBuffer(textStreamAsyncOptions.stringRingBufferCapacity);
        }
        
        if (charRingBuffer == null) {
            charRingBuffer = new CharRingBuffer(textStreamAsyncOptions.charRingBufferCapacity);
        }
        
        if (byteRingBuffer == null) {
            byteRingBuffer = new ByteRingBuffer(textStreamAsyncOptions.byteRingBufferCapacity);
        }
        
        _stringRingBuffer = stringRingBuffer;
        _asyncLineJoiner = new AsyncLineJoiner(stringRingBuffer, charRingBuffer, textStreamAsyncOptions);
        _asyncCharEncoder = new AsyncCharEncoder(charRingBuffer, byteRingBuffer, textStreamAsyncOptions);
        _asyncByteStreamWriter = new AsyncByteStreamWriter(outputStream, byteRingBuffer, textStreamAsyncOptions);
        _areLoopsStarted = false;
        _isEOF = false;
    }

    /**
     * Returns the underlying {@link StringRingBuffer} from where text lines are read. 
     * 
     * @return  The underlying {@link StringRingBuffer}.
     */
    public StringRingBuffer getStringRingBuffer() {
        return _stringRingBuffer;
    }
    
    /**
     * Checks whether this text stream writer has reached EOF.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        return _isEOF;
    }

    /**
     * "<i>ready</i>" predicate that returns true when lines can be written to the string ring buffer.   
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            if (!_areLoopsStarted) {
                _asyncLineJoiner.startApplyLoopAsync();
                _asyncCharEncoder.startApplyLoopAsync();
                _asyncByteStreamWriter.startApplyLoopAsync();
                _areLoopsStarted = true;
            }
            
            isReady = !isEOF() || !isEmpty();
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
            isDone = isEOF() || (_stringRingBuffer.isEOF() && isEmpty());
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
     * Checks whether all the underlying ring buffers are empty.
     * This condition is important to check when {@link AsyncAgent#applyAsync} is used because
     * that method would return as soon as a set of lines is sent down to the {@link AsyncLineJoiner},
     * but there may be chars and bytes still in the pipeline. 
     * If you want to continue execution when all chars and bytes have been flushed to the OutputStream,
     * you may use this method in conjunction with {@link WhenReady#completeAsync}.
     * 
     * @return  true iff all of the underlying ring buffers are empty.
     */
    public boolean isEmpty() {
        return  _stringRingBuffer.getAvailableToRead() == 0 
             && _asyncCharEncoder.getCharRingBuffer().getAvailableToRead() == 0
             && _asyncCharEncoder.getByteRingBuffer().getAvailableToRead() == 0;
    }
    
    /**
     * Sets the EOF status on this instance to true. 
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
