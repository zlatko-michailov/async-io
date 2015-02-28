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
 * @see     java.io.InputStream, java.util.concurrent.CompletableFuture
 * 
 * @author Zlatko Michailov <zlatko+async@michailov.org>
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
     * @param   asyncOptions    AsyncOptions to use for the async calls. 
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
    
    public CompletableFuture<Void> getEOF() {
        return _eof;
    }
    
    /**
     * Returns the attached ByteRingBuffer.
     * 
     * @return  The attached ByteRingBUffer.
     */
    public ByteRingBuffer getByteRingBuffer() {
        return _byteRingBuffer;
    }

    public CompletableFuture<Void> startReadingLoopAsync() {
        return startRead(LOOP);
    }
    
    public CompletableFuture<Void> readAsync() {
        return startRead(NOT_LOOP);
    }
    
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

    private static void ensureArgumentNotNull(String argName, Object argValue) {
        if (argValue == null) {
            throw new IllegalArgumentException(String.format("Argument %1$s may not be null.", argName));
        }
    }
}
