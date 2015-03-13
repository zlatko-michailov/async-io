package org.michailov.async.io;

import java.io.*;
import java.util.concurrent.*;
import org.michailov.async.*;

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
    
    private final InputStream _inputStream;
    private final ByteRingBuffer _byteRingBuffer;
    private final AsyncOptions _asyncOptions;
    private final CompletableFuture<Void> _eof;
    
    private Mode _mode;
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
        _mode = Mode.IDLE;
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

    public CompletableFuture<Void> readAsync() {
        ensureReadableState(Mode.ONCE);

        return WhenReady.applyAsync(reader -> reader.canRead(), reader -> reader.read(), this, _asyncOptions);
    }
    
    /**
     * Starts a loop that read bytes from the stream and writes them into the ring buffer.
     * 
     * @return  A future that completes when the loop is finished either due to reaching EOF or due to an exception.
     */
    public CompletableFuture<Void> startReadingLoopAsync() {
        ensureReadableState(Mode.LOOP);

        return WhenReady.startApplyLoopAsync(reader -> reader.canRead(), reader -> reader._eof.isDone(), reader -> reader.read(), this, _asyncOptions);
    }
    
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
                        if (_mode == Mode.ONCE) {
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
    
    private void setIdle() {
        _mode = Mode.IDLE;
    }

    private void completeEOF() {
        _eof.complete(null);
        _byteRingBuffer.setEOF();
        
        setIdle();
    }
    
    private void completeEOFExceptionallyAndThrow(Throwable ex) {
        _eof.completeExceptionally(ex);
        _byteRingBuffer.setEOF();
        
        setIdle();
        
        throw new AsyncException(ex);
    }
    
    /*private CompletableFuture<Void> startRead(boolean isLoop) {
        ensureReadableState1();
        
        // Cache the current read future locally because the member may get nulled out in parallel. 
        CompletableFuture<Void> future = new CompletableFuture<Void>();
        
        // Initialize the current read's context.
        _future = future;
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
                        _future.complete(null);
                        _future = null;
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
                            _future.complete(null);
                            _future = null;
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
            _future.completeExceptionally(ex);
            _future = null;
            
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
                _future.completeExceptionally(new TimeoutException());
                _future = null;
                
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
    
    private void ensureReadableState1() {
        // There should be no outstanding operation on this reader.
        if (_future != null) {
            throw new IllegalStateException("There is already a readAsync operation in progress. Await for the returned CompletableFuture to complete, and retry.");
        }
        
        // If EOF has already been reached, the caller shouldn't have continued.
        if (_eof.isDone()) {
            throw new IllegalStateException("Attempting to read past the end of stream.");
        }
    }*/

    /**
     * Ensures the state is good for starting a new read operation.
     */
    private void ensureReadableState(Mode mode) {
        // The reader must be in idle mode.
        if (_mode != Mode.IDLE) {
            throw new IllegalStateException("There is already an operation in progress. Await for the returned CompletableFuture to complete, and then retry.");
        }
        
        // If EOF has already been reached, the caller shouldn't have continued.
        if (_eof.isDone()) {
            throw new IllegalStateException("Attempting to read past the end of stream.");
        }
        
        _mode = mode;
    }

    /**
     * Helper that checks an argument for null.
     */
    private static void ensureArgumentNotNull(String argName, Object argValue) {
        if (argValue == null) {
            throw new IllegalArgumentException(String.format("Argument %1$s may not be null.", argName));
        }
    }
    
    private enum Mode {
        IDLE,
        ONCE,
        LOOP
    }
}
