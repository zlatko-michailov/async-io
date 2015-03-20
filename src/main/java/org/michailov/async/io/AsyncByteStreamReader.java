package org.michailov.async.io;

import java.io.*;
import java.util.concurrent.*;

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
public class AsyncByteStreamReader extends Worker {
    
    private final InputStream _inputStream;
    private final ByteRingBuffer _byteRingBuffer;
    private final AsyncOptions _asyncOptions;
    
    /**
     * Constructs a new AsyncByteStreamReader instance to read from the given InputStream
     * into the given {@link ByteRingBuffer}.
     * 
     * @param   inputStream     An InputStream to read from.
     * @param   byteRingBuffer  A {@link ByteRingBuffer} to write to.
     * @param   asyncOptions    {@link AsyncOptions} that will control all async operations on this instance. 
     */
    public AsyncByteStreamReader(InputStream inputStream, ByteRingBuffer byteRingBuffer, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("inputStream", inputStream);
        ensureArgumentNotNull("byteRingBuffer", byteRingBuffer);
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        _inputStream = inputStream;
        _byteRingBuffer = byteRingBuffer;
        _asyncOptions = asyncOptions;
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
     * Reads bytes from the stream and writes them into the ring buffer.
     * 
     * @return  A future that completes when either some bytes have been read or an exception has occurred.
     */
    public CompletableFuture<Void> readAsync() {
        ensureReadableState(WorkerMode.ONCE);

        return WhenReady.applyAsync(reader -> reader.canRead(), reader -> reader.read(), this, _asyncOptions);
    }
    
    /**
     * Starts a loop that read bytes from the stream and writes them into the ring buffer.
     * 
     * @return  A future that completes when the loop is finished either due to reaching EOF or due to an exception.
     */
    public CompletableFuture<Void> startReadingLoopAsync() {
        ensureReadableState(WorkerMode.LOOP);

        return WhenReady.startApplyLoopAsync(reader -> reader.canRead(), reader -> reader._eof.isDone(), reader -> reader.read(), this, _asyncOptions);
    }
    
    /**
     * 'ready' predicate that returns true iff bytes can be read from the stream and written to the ring buffer without blocking.   
     */
    private boolean canRead() {
        boolean isReady = false;
        
        try {
            isReady = !_eof.isDone() && _inputStream.available() > 0 && _byteRingBuffer.getAvailableToWriteStraight() > 0;
        }
        catch (Throwable ex) {
            completeEOFExceptionallyAndThrow(ex);
        }
        
        return isReady;
    }
    
    /**
     * 'action' function that reads bytes from the stream and writes them to the ring buffer.   
     */
    private Void read() {
        try {
            int availableByteCount = _inputStream.available();
            if (availableByteCount > 0) {
                // If something is available, try to read the minimum of that and what's available to write straight in the ring buffer.
                int targetByteCount = Math.min(availableByteCount, _byteRingBuffer.getAvailableToWriteStraight());
                int actualByteCount = _inputStream.read(_byteRingBuffer.getBuffer(), _byteRingBuffer.getWritePosition(), targetByteCount);
                if (actualByteCount != 0) {
                    if (actualByteCount == EOF) {
                        completeEOF();
                    }
                    else {
                        // Bytes were read from the stream and written into the ring buffer.
                        // Advance the ring buffer's write position.
                        _byteRingBuffer.advanceWritePosition(actualByteCount);
                        
                        // If this was a one-time read, become idle.
                        if (_mode == WorkerMode.ONCE) {
                            setIdle();
                        }
                    }
                }
            }
        }
        catch (Throwable ex) {
            completeEOFExceptionallyAndThrow(ex);
        }
        
        return null;
    }
    
    /**
     * Completes the EOF future on this instance as well as on the underlying ring buffer normally. 
     * Marks this instance as "idle".
     */
    protected void completeEOF() {
        _byteRingBuffer.setEOF();
        super.completeEOF();
    }
    
    /**
     * Completes the EOF futures on this instance as well as on the underlying ring buffer exceptionally.
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this instance as "idle".
     */
    protected void completeEOFExceptionallyAndThrow(Throwable ex) {
        _byteRingBuffer.setEOF();
        super.completeEOFExceptionallyAndThrow(ex);
    }
    
}
