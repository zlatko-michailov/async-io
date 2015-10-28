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
 * Asynchronous splitter of chars into lines.
 * 
 * @see     WhenReady
 * @see     AsyncAgent
 * @see     CharRingBuffer
 * @see     StringRingBuffer
 * 
 * @author  Zlatko Michailov
 */
public class AsyncLineSplitter extends AsyncAgent {

    private static final int CR = 0x000d;
    private static final int LF = 0x000a;
    private static final int[] LINE_BREAK_CHARS = { 0x000a, 0x000b, 0x000c, 0x000d, 0x0085, 0x2028, 0x2029 };
    private static final boolean EVEN_IF_EMPTY = true;
    
    private final CharRingBuffer _charRingBuffer;
    private final StringRingBuffer _stringRingBuffer;
    private final StringBuilder _currentLine;
    private int _lastChar;

    /**
     * Constructs an AsyncLineSpliter instance over the given ring buffers.
     * 
     * @param charRingBuffer        {@link CharRingBuffer} to read chars from.
     * @param stringRingBuffer      {@link StringRingBuffer} to write lines to.
     * @param lineAsyncOptions      {@link LineAsyncOptions} to use for all async operations.
     */
    public AsyncLineSplitter(CharRingBuffer charRingBuffer, StringRingBuffer stringRingBuffer, LineAsyncOptions lineAsyncOptions) {
        super(lineAsyncOptions);
        
        Util.ensureArgumentNotNull("charRingBuffer", charRingBuffer);
        Util.ensureArgumentNotNull("stringRingBuffer", stringRingBuffer);
        Util.ensureArgumentNotNull("asyncOptions", lineAsyncOptions);
        
        _charRingBuffer = charRingBuffer;
        _stringRingBuffer = stringRingBuffer;
        _currentLine = new StringBuilder(lineAsyncOptions.estimatedLineLength);
        _lastChar = 0;
    }

    /**
     * Returns the attached char ring buffer from where chars will be read from.
     * 
     * @return  The attached char ring buffer from where chars will be read from.
     */
    public CharRingBuffer getCharRingBuffer() {
        return _charRingBuffer;
    }
    
    /**
     * Returns the attached string ring buffer where lines will be written.
     * 
     * @return  The attached string ring buffer where lines will be written.
     */
    public StringRingBuffer getStringRingBuffer() {
        return _stringRingBuffer;
    }
    
    /**
     * Checks whether this line splitter has reached EOF of the input char ring buffer.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        return _stringRingBuffer.isEOF();
    }

    /**
     * "<i>ready</i>" predicate that returns true when chars can be read from the input
     * char ring buffer and lines can be written to the output string ring buffer without blocking.   
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            isReady = !isEOF() && _charRingBuffer.getAvailableToRead() > 0 && _stringRingBuffer.getAvailableToWrite() > 0;
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
            isDone = isEOF() || (_charRingBuffer.isEOF() && _charRingBuffer.getAvailableToRead() == 0);
            if (isDone) {
                flushCurrentLine(!EVEN_IF_EMPTY);
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
            int ch = _charRingBuffer.read();
            if (_lastChar == CR && ch == LF) {
                // Ignore this LF.
            }
            else if (isLineBreakChar(ch)) {
                // Flush the StringBuilder.
                flushCurrentLine(EVEN_IF_EMPTY);
            }
            else {
                // Append this char to the current line.
                _currentLine.append((char)ch);
            }
            
            _lastChar = ch;
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
    }
    
    private boolean isLineBreakChar(int ch) {
        for (int i = 0; i < LINE_BREAK_CHARS.length; i++) {
            if (LINE_BREAK_CHARS[i] == ch) {
                return true;
            }
        }
        
        return false;
    }
    
    private void flushCurrentLine(boolean evenIfEmpty) {
        if (evenIfEmpty || _currentLine.length() > 0) {
            _stringRingBuffer.write(_currentLine.toString());
            _currentLine.delete(0, _currentLine.capacity());
        }
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying output string ring buffer to true. 
     * Marks this instance as 'idle'.
     */
    private void setEOF() {
        String logMessage = String.format("future=unknown , class=%1$s , event=EOF", getClass().getName());
        Logger.getLogger().info(logMessage);
        
        _stringRingBuffer.setEOF();
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
        String logMessage = String.format("future=unknown , class=%1$s , event=THROW", getClass().getName());
        Logger.getLogger().info(logMessage);
        
        _stringRingBuffer.setEOF();
        setIdleAndThrow(ex);
    }
    
}
