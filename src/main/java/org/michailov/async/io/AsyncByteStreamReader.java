package org.michailov.async.io;

import java.io.*;
import org.michailov.async.*;

/**
 * Asynchronous byte reader over a plain old InputStream.
 * The reader doesn't block as long as the stream correctly reports its available bytes.
 * <p>
 * The reader reads available bytes from the stream if there are any. 
 * If there aren't, it schedules a future to do that.
 * <p>
 * The stream bytes are read into a {@link ByteRingBuffer}. 
 * <p>
 * Note: The caller is responsible for opening and closing the stream as needed. 
 * 
 * @see     ByteRingBuffer
 * @see     WhenReady
 * 
 * @author  Zlatko Michailov
 */
public class AsyncByteStreamReader extends AsyncAgent {
    
    protected static final int EOF = -1;
    
    private final InputStream _inputStream;
    private final ByteRingBuffer _byteRingBuffer;
    
    /**
     * Constructs a new AsyncByteStreamReader instance to read from the given InputStream
     * into the given {@link ByteRingBuffer}.
     * 
     * @param   inputStream     An InputStream to read from.
     * @param   byteRingBuffer  A {@link ByteRingBuffer} to write to.
     * @param   asyncOptions    {@link AsyncOptions} that will control all async operations on this instance. 
     */
    public AsyncByteStreamReader(InputStream inputStream, ByteRingBuffer byteRingBuffer, AsyncOptions asyncOptions) {
        super(asyncOptions);
        
        Util.ensureArgumentNotNull("inputStream", inputStream);
        Util.ensureArgumentNotNull("byteRingBuffer", byteRingBuffer);
        
        _inputStream = inputStream;
        _byteRingBuffer = byteRingBuffer;
    }
    
    /**
     * Returns the underlying InputStream.
     * 
     * @return  The underlying InputStream.
     */
    public InputStream getInputStream() {
        return _inputStream;
    }
    
    /**
     * Returns the attached ring buffer where bytes will be written.
     * 
     * @return  The attached ring buffer where bytes will be written.
     */
    public ByteRingBuffer getByteRingBuffer() {
        return _byteRingBuffer;
    }
    
    /**
     * Checks whether this reader has reached EOF of the stream.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        return _byteRingBuffer.isEOF();
    }

    /**
     * "<i>ready</i>" predicate that returns true when bytes can be read from the stream and written to the ring buffer without blocking.   
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            isReady = !isEOF() && _inputStream.available() > 0 && _byteRingBuffer.getAvailableToWriteStraight() > 0;
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
     * "<i>action</i>" function that reads bytes from the stream and writes them to the ring buffer.   
     */
    @Override
    protected void action() {
        try {
            int availableByteCount = _inputStream.available();
            if (availableByteCount > 0) {
                // If something is available, try to read the minimum of that and what's available to write straight in the ring buffer.
                int targetByteCount = Math.min(availableByteCount, _byteRingBuffer.getAvailableToWriteStraight());
                int actualByteCount = _inputStream.read(_byteRingBuffer.getBuffer(), _byteRingBuffer.getWritePosition(), targetByteCount);
                if (actualByteCount != 0) {
                    if (actualByteCount == EOF) {
                        setEOF();
                    }
                    else {
                        // Bytes were read from the stream and written into the ring buffer.
                        // Advance the ring buffer's write position.
                        _byteRingBuffer.advanceWritePosition(actualByteCount);
                    }
                }
            }
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying ring buffer to true. 
     * Marks this instance as 'idle'.
     */
    protected void setEOF() {
        _byteRingBuffer.setEOF();
        setIdle();
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying ring buffer to true. 
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this instance as 'idle'.
     * 
     * @param   ex  An exception to complete with.
     */
    protected void setEOFAndThrow(Throwable ex) {
        _byteRingBuffer.setEOF();
        setIdleAndThrow(ex);
    }
    
}
