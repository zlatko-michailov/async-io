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

import java.util.function.*;
import org.michailov.async.*;

/**
 * Asynchronous watcher of a {@link RingBuffer}.
 * Fires the given callback when there are items available for reading.
 * 
 * @see     WhenReady
 * @see     AsyncAgent
 * @see     RingBuffer
 * 
 * @author  Zlatko Michailov
 */
public class AsyncRingBufferWatcher<TRingBuffer extends RingBuffer> extends AsyncAgent {

    private final TRingBuffer _ringBuffer;
    private final Consumer<TRingBuffer> _onAvailableToRead;
    private boolean _isEOF;
    
    /**
     * Constructs an AsyncCharDecoder instance over the given ring buffers.
     * 
     * @param ringBuffer            {@link RingBuffer} to watch.
     * @param onAvailableToRead     A callback to invoke when there are items available to read from the ring buffer.
     * @param asyncOptions          {@link AsyncOptions} to use for all async operations.
     */
    public AsyncRingBufferWatcher(TRingBuffer ringBuffer, Consumer<TRingBuffer> onAvailableToRead, AsyncOptions asyncOptions) {
        super(asyncOptions);
        
        Util.ensureArgumentNotNull("ringBuffer", ringBuffer);
        Util.ensureArgumentNotNull("onAvailableToRead", onAvailableToRead);
        Util.ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        _ringBuffer = ringBuffer;
        _onAvailableToRead = onAvailableToRead;
        _isEOF = false;
    }

    /**
     * Returns the watched ring buffer.
     * 
     * @return  The watched ring buffer.
     */
    public TRingBuffer getRingBuffer() {
        return _ringBuffer;
    }
    
    /**
     * Checks whether this watcher has reached EOF of the watched ring buffer.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        return _isEOF;
    }

    /**
     * "<i>ready</i>" predicate that returns true when items can be read from the watched ring buffer.
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            isReady = !isEOF() && _ringBuffer.getAvailableToRead() > 0;
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
            isDone = isEOF() || (_ringBuffer.isEOF() && _ringBuffer.getAvailableToRead() == 0);
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
     * "<i>action</i>" function that invokes the onAvailableToRead callback synchronously.   
     */
    @Override
    protected void action() {
        try {
            _onAvailableToRead.accept(_ringBuffer);
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
    }
    
    /**
     * Sets the EOF status on this async agent to true. 
     * Marks this instance as 'idle'.
     */
    private void setEOF() {
        _isEOF = true;
        setIdle();
    }
    
    /**
     * Sets the EOF status on this async agent to true. 
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
