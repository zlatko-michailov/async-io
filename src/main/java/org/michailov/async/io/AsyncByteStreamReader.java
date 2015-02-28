package org.michailov.async.io;

import java.io.*;
import java.util.concurrent.*;

/**
 * Asynchronous byte reader over a plain old InputStream.
 * The reader never blocks (as long as the stream correctly reports its available bytes.)
 * <p>
 * The reader reads available bytes from the stream if there are any. 
 * If there aren't, it schedules a future to do that.
 * <p>
 * The stream bytes are read into a ByteRingBuffer. 
 * <p>
 * Note: The caller is responsible for opening and closing the stream as needed. 
 * 
 * @see     java.io.InputStream
 * @see     java.util.concurrent.CompletableFuture
 * @see     ByteRingBuffer
 * 
 * @author  Zlatko Michailov
 */
public class AsyncByteStreamReader {
    
    private static final int EOF = -1;
    final boolean LOOP = true;
    final boolean NOT_LOOP = false;
    
    private final InputStream _inputStream;
    private final ByteRingBuffer _byteRingBuffer;
    private final AsyncOptions _asyncOptions;
    private final CompletableFuture<Void> _eof;
    
    private CompletableFuture<Void> _readFuture;
    private long _startTimeMillis;
    private long _timeoutMillis;
    
    /**
     * Constructs a new AsyncByteStreamReader instance to read from the given InputStream
     * into the given ByteRingBuffer.
     * 
     * @param   inputStream     An InputStream to read from.
     * @param   byteRingBuffer  A ByteRingBuffer to write to.
     * @param   asyncOptions    AsyncOptions to use for all async operations. 
     */
    public AsyncByteStreamReader(InputStream inputStream, ByteRingBuffer byteRingBuffer, AsyncOptions asyncOptions) {
        ensureArgumentNotNull("inputStream", inputStream);
        ensureArgumentNotNull("byteRingBuffer", byteRingBuffer);
        ensureArgumentNotNull("asyncOptions", asyncOptions);
        
        _inputStream = inputStream;
        _byteRingBuffer = byteRingBuffer;
        _asyncOptions = asyncOptions;
        _eof = new CompletableFuture<Void>();
        _readFuture = null;
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
     * Returns a future that completes when the reader reaches the end of the stream.
     * 
     * @return  A future that completes when the reader reaches the end of the stream..
     */
    public CompletableFuture<Void> getEOF() {
        return _eof;
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
     * Starts a loop that read bytes from the stream and writes them into the ring buffer.
     * 
     * @return  A future that completes when the loop is finished either due to reaching EOF or due to an exception.
     */
    public CompletableFuture<Void> startReadingLoopAsync() {
        return startRead(LOOP);
    }
    
    /**
     * Reads bytes from the stream into the ring buffer. There is no guarantee how many bytes will be read.
     * 
     * @return  A future that completes when either some bytes have been read from the stream, EOF has been reached, or an exception has occurred.
     */
    public CompletableFuture<Void> readAsync() {
        return startRead(NOT_LOOP);
    }
    
    /**
     * Starts a read operation - ensures correct state for the operation.
     */
    private CompletableFuture<Void> startRead(boolean isLoop) {
        ensureReadableState();
        
        // Cache the current read future locally because the member may get nulled out in parallel. 
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        
        // Initialize the current read's context.
        _readFuture = future;
        _startTimeMillis = System.currentTimeMillis();
        _timeoutMillis = _asyncOptions.timeout >= 0 ? _asyncOptions.timeUnit.toMillis(_asyncOptions.timeout) : AsyncOptions.TIMEOUT_INFINITE;

        // Read sync if possible or submit an operation.
        submitRead(isLoop);
        
        return future;
    }
    
    /**
     * Attempts to read sync or submits a read operation.
     */
    private void submitRead(boolean isLoop) {
        // Attempt to read sync.
        boolean isDone = readSync(isLoop);

        if (!isDone) {
            // Nothing could be read sync, nor was there an error.
            // Before submitting a new attempt, check timeout.
            isDone = hasTimedOut(isLoop);
        }
        
        if (!isDone) {
            // Nothing has came out from this attempt.
            // Submit another one.
            ForkJoinPool.commonPool().execute(() -> submitRead(isLoop));
        }
    }
    
    /**
     * Attempts to read sync if there are available bytes.
     * 
     * @return  true if the operation is complete; false if a new attempt should be queued up.
     */
    private boolean readSync(boolean isLoop) {
        try {
            // Check what's available from the stream.
            int availableByteCount = _inputStream.available();
            if (availableByteCount > 0) {
                // If something is available, try to read the minimum of that and what's available to write straight in the ring buffer.
                int targetByteCount = Math.min(availableByteCount, _byteRingBuffer.getAvailableToWriteStraight());
                int actualByteCount = _inputStream.read(_byteRingBuffer.getBuffer(), _byteRingBuffer.getWritePosition(), targetByteCount);
                if (actualByteCount != 0) {
                    
                    boolean isDone = false;

                    if (actualByteCount == EOF) {
                        // EOF was received from the stream -
                        // Complete everything - the current read future, the EOF, and the ring buffer.
                        // Done.
                        _eof.complete(null);
                        _byteRingBuffer.setEOF();
                        _readFuture.complete(null);
                        _readFuture = null;
                        isDone = true;
                    }
                    else {
                        // Bytes were read from the stream and written into the ring buffer.
                        // Advance the ring buffer's write position.
                        _byteRingBuffer.advanceWritePosition(actualByteCount);
                        
                        if (!isLoop) {
                            // Something (good or bad) was received from the stream. 
                            // Complete the current read future.
                            // Done.
                            _readFuture.complete(null);
                            _readFuture = null;
                            isDone = true;
                        }
                    }
                    
                    return isDone;
                }
            }
        }
        catch(IOException ex) {
            // Upon exception, complete the current read future, the EOF, and the ring buffer.
            // Done.
            _eof.completeExceptionally(ex);
            _byteRingBuffer.setEOF();
            _readFuture.completeExceptionally(ex);
            _readFuture = null;
            
            return true;
        }
        
        // Nothing interesting has happened. So not done yet.
        return false;
    }
    
    /**
     * Checks if the operation has timed out..
     * 
     * @return  true if the operation is complete; false if a new attempt should be queued up.
     */
    private boolean hasTimedOut(boolean isLoop) {
        if (_asyncOptions.timeout >= 0) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - _startTimeMillis > _timeoutMillis) {
                // Time's up!
                // Complete the current read future with a TimeoutException.
                _readFuture.completeExceptionally(new TimeoutException());
                _readFuture = null;
                
                if (isLoop) {
                    // Complete the EOF and the ring buffer.
                    _eof.completeExceptionally(new TimeoutException());
                    _byteRingBuffer.setEOF();
                }

                // Done.
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Ensures the state is good for starting a new read operation.
     */
    private void ensureReadableState() {
        // There should be no outstanding operation on this reader.
        if (_readFuture != null) {
            throw new IllegalStateException("There is already a readAsync operation in progress. Await for the returned CompletableFuture to complete, and retry.");
        }
        
        // If EOF has already been reached, the caller shouldn't have continued.
        if (_eof.isDone()) {
            throw new IllegalStateException("Attempting to read past the end of stream.");
        }
    }

    /**
     * Helper that check arguments for null.
     */
    private static void ensureArgumentNotNull(String argName, Object argValue) {
        if (argValue == null) {
            throw new IllegalArgumentException(String.format("Argument %1$s may not be null.", argName));
        }
    }
}
