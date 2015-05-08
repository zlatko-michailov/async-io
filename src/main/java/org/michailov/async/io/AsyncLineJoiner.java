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

import org.michailov.async.*;

/**
 * Asynchronous joiner of discrete lines into a sequence of chars.
 * 
 * @see     WhenReady
 * @see     AsyncAgent
 * @see     CharRingBuffer
 * @see     StringRingBuffer
 * 
 * @author  Zlatko Michailov
 */
public class AsyncLineJoiner  extends AsyncAgent {

    private static final int NEGATIVE = -1;
    
    private final StringRingBuffer _stringRingBuffer;
    private final CharRingBuffer _charRingBuffer;
    private final String _lineBreak; 
    private String _currentLine;
    private int _lineCharIndex;
    private int _breakCharIndex;

    /**
     * Constructs an AsyncLineJoiner instance over the given ring buffers.
     * 
     * @param stringRingBuffer      {@link StringRingBuffer} to read lines from.
     * @param charRingBuffer        {@link CharRingBuffer} to write chars to.
     * @param lineAsyncOptions      {@link LineAsyncOptions} to use for all async operations.
     */
    public AsyncLineJoiner(StringRingBuffer stringRingBuffer, CharRingBuffer charRingBuffer, LineAsyncOptions lineAsyncOptions) {
        super(lineAsyncOptions);
        
        Util.ensureArgumentNotNull("stringRingBuffer", stringRingBuffer);
        Util.ensureArgumentNotNull("charRingBuffer", charRingBuffer);
        Util.ensureArgumentNotNull("asyncOptions", lineAsyncOptions);
        
        _stringRingBuffer = stringRingBuffer;
        _charRingBuffer = charRingBuffer;
        _lineBreak = lineAsyncOptions.lineBreak;
        _currentLine = null;
        _lineCharIndex = NEGATIVE;
        _breakCharIndex = NEGATIVE;
    }

    /**
     * Returns the attached string ring buffer where lines will be read from.
     * 
     * @return  The attached string ring buffer where lines will be read from.
     */
    public StringRingBuffer getStringRingBuffer() {
        return _stringRingBuffer;
    }
    
    /**
     * Returns the attached char ring buffer from where chars will be written.
     * 
     * @return  The attached char ring buffer from where chars will be written.
     */
    public CharRingBuffer getCharRingBuffer() {
        return _charRingBuffer;
    }
    
    /**
     * Checks whether this line joiner has reached EOF of the input string ring buffer.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        return _charRingBuffer.isEOF();
    }

    /**
     * "<i>ready</i>" predicate that returns true when lines can be read from the input
     * string ring buffer and chars can be written to the output char ring buffer without blocking.   
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            isReady = !isEOF() && (isLineInProgress() || _stringRingBuffer.getAvailableToRead() > 0) && _charRingBuffer.getAvailableToWrite() > 0;
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
            isDone = isEOF() || (!isLineInProgress() && _stringRingBuffer.isEOF() && _stringRingBuffer.getAvailableToRead() == 0);
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
     * "<i>action</i>" function that reads chars from the input char ring buffer and writes lines to the output string ring buffer.   
     */
    @Override
    protected void action() {
        try {
            if (isLineInProgress()) {
                writeCurrentLineChars();
            }
            else {
                readNextLine();
            }
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
    }
    
    /**
     * Checks whether a current line (including the trailing EOL mark) is in progress.
     */
    private boolean isLineInProgress() {
        return _lineCharIndex > NEGATIVE || _breakCharIndex > NEGATIVE;
    }

    /**
     * Reads and caches the next line from the input string ring buffer.
     */
    private void readNextLine() {
        _currentLine = _stringRingBuffer.read();
        
        // Prepare both indexes now to avoid an extra check later.
        _lineCharIndex = 0;
        _breakCharIndex = 0;
    }
    
    /**
     * Copies the chars of the current line plus a trailing EOL mark to the output char ring buffer.
     */
    private void writeCurrentLineChars() {
        while (_charRingBuffer.getAvailableToWrite() > 0) {
            if (_lineCharIndex > NEGATIVE && _lineCharIndex < _currentLine.length()) {
                char ch = _currentLine.charAt(_lineCharIndex++);
                _charRingBuffer.write(ch);
            }
            else if (_breakCharIndex < _lineBreak.length()) { 
                char ch = _lineBreak.charAt(_breakCharIndex++);
                _charRingBuffer.write(ch);
            }
            else {
                // No line in progress.
                _lineCharIndex = NEGATIVE;
                _breakCharIndex = NEGATIVE;
                break;
            }
        }
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying output string ring buffer to true. 
     * Marks this instance as 'idle'.
     */
    private void setEOF() {
        _charRingBuffer.setEOF();
        setIdle();
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying output string ring buffer to true. 
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this instance as 'idle'.
     * 
     * @param   ex  An exception to complete with.
     */
    private void setEOFAndThrow(Throwable ex) {
        _charRingBuffer.setEOF();
        setIdleAndThrow(ex);
    }
    
}
