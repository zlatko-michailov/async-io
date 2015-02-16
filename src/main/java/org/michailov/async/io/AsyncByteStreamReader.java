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
 * The caller chooses when the returned future completes - as soon as the first batch of available bytes is read,
 * or when the given buffer is full (or the end of stream is reached meanwhile.)
 * <p> 
 * If a positive number of bytes was successfully read, that number is returned even if end of stream was reached. 
 * -1 is returned once - upon the first attempt to read straight from the end of stream and there are 0 bytes to read.
 * Further attempts to read throw IllegalStateException.
 * <p> 
 * Note: The caller is responsible for opening and closing the stream as needed. 
 * 
 * @see     java.io.InputStream, java.util.concurrent.CompletableFuture
 * 
 * @author Zlatko Michailov <zlatko+async@michailov.org>
 */
public class AsyncByteStreamReader {
    
    private static final int EOF = -1;
    private static final long TIMEOUT_INFINITE = -1;
    
    private final InputStream _stream;
    private EOFState _eofState;
    private AsyncState _asyncState;
    
    /**
     * Creates a new AsyncByteStreamReader instance to read from the given InputStream.
     * 
     * @param   stream  An InputStream to read from.
     */
    public AsyncByteStreamReader(InputStream stream) {
        _stream = stream;
        _eofState = EOFState.NOT_REACHED;
    }
    
    /**
     * Returns the underlying InputStream.
     * 
     * @return  The underlying InputStream instance.
     */
    public InputStream getStream() {
        return _stream;
    }

    /**
     * Starts an asynchronous read operation with an infinite timeout that will complete 
     * as soon as the first batch of available bytes is read.
     *  
     * @param   buff    Buffer to read into.
     * @param   offset  Offset in the buffer to start placing bytes.
     * @param   length  Maximum number of bytes to be read. A smaller number may actually be read.
     * @return          A CompletableFuture<Integer> that will complete as soon as the first batch 
     *                  of available bytes is read. The result value is the number of bytes actually 
     *                  read, or -1 if an attempt to read from the end of stream was made. 
     */
    public CompletableFuture<Integer> readAsync(byte[] buff, int offset, int length) {
        return readAsync(buff, offset, length, CompleteWhen.AVAILABLE, TIMEOUT_INFINITE, TimeUnit.MILLISECONDS);
    }

    /**
     * Starts an asynchronous read operation with an infinite timeout.
     * 
     * @param   buff            Buffer to read into.
     * @param   offset          Offset in the buffer to start placing bytes.
     * @param   length          Maximum number of bytes to be read. A smaller number may actually be read.
     * @param   completeWhen    When AVAILABLE, the returned future completes as soon as the first batch of
     *                          available bytes is read. 
     *                          When FULL, the returned future completes when either the buffer is full or
     *                          when there are no more bytes to read.
     * @return                  A CompletableFuture<Integer>. The result value is the number of bytes actually 
     *                          read, or -1 if an attempt to read from the end of stream was made. 
     */
    public CompletableFuture<Integer> readAsync(byte[] buff, int offset, int length, CompleteWhen completeWhen) {
        return readAsync(buff, offset, length, completeWhen, TIMEOUT_INFINITE, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Starts an asynchronous read operation.
     * 
     * @param   buff            Buffer to read into.
     * @param   offset          Offset in the buffer to start placing bytes.
     * @param   length          Maximum number of bytes to be read. A smaller number may actually be read.
     * @param   completeWhen    When AVAILABLE, the returned future completes as soon as the first batch of
     *                          available bytes is read. 
     *                          When FULL, the returned future completes when either the buffer is full or
     *                          when there are no more bytes to read.
     * @param   timeout         The number of timeout units before the operation expires, or -1 for infinity.
     * @param   unit            The type of time units.
     * @return                  A CompletableFuture<Integer>. The result value is the number of bytes actually 
     *                          read, or -1 if an attempt to read from the end of stream was made. 
     */
    public CompletableFuture<Integer> readAsync(byte[] buff, int offset, int length, CompleteWhen completeWhen, long timeout, TimeUnit unit) {
        // There should be no outstanding operation on this reader.
        if (_asyncState != null) {
            throw new IllegalStateException("There is already a readAsync operation in progress. Await for the returned task to complete, and retry.");
        }
        
        // The buffer should be existent.
        if (buff == null) {
            throw new IllegalArgumentException("Argument buff may not be null.");
        }
        
        // The offset should be within the buffer's boundaries.
        if (offset < 0 || buff.length <= offset) {
            String message = String.format("Argument offset (%1$d) must be between 0 and %2$d, or the length of buff must be increased.", offset, buff.length - 1);
            throw new IllegalArgumentException(message);
        }
        
        // The combination of offset and length should be within the buffer's boundaries. 
        if (length < 0 || buff.length < offset + length) {
            String message = String.format("Argument length (%1$d) must be between 0 and %2$d, or the length of buff must be increased.", length, buff.length - 1 - offset);
            throw new IllegalArgumentException(message);
        }
        
        // If EOF has already been reported, the caller shouldn't have continued.
        if (_eofState == EOFState.REPORTED) {
            throw new IllegalStateException("Attempting to read past the end of stream.");
        }
        
        // If EOF was received from the stream, but hasn't been yet reported to the caller,
        // return a completed future with a result of EOF, and record that EOF has been reported to the caller.
        if (_eofState == EOFState.RECEIVED) {
            _eofState = EOFState.REPORTED;
            return CompletableFuture.completedFuture(Integer.valueOf(EOF));
        }
        
        // If the caller is requesting 0 bytes, return a completed future immediately. 
        if (length == 0) {
            return CompletableFuture.completedFuture(Integer.valueOf(0));
        }

        // Create a new operation state.
        // Cache everything that may be need through the end of this method because the state may get deleted before the return statement is reached. 
        _asyncState = new AsyncState(buff, offset, length, completeWhen, unit.toMillis(timeout));
        CompletableFuture<Integer> future = _asyncState.future;

        // Submit a new operation or complete synchronously.
        submitRead();
        
        // Return the cached future reference which may or may not be completed by now. 
        return future;
    }
    
    /**
     * Submits a new async operation or completes synchronously.
     */
    private void submitRead() {
        try {
            // Check what's available from the stream.
            int available = _stream.available();
            if (available > 0) {
                
                // If something is available, try to read the minimum of that and what's left from the request.
                int target = Math.min(available, _asyncState.remaining);
                int actual = _stream.read(_asyncState.buff, _asyncState.offset, target);
                if (actual != 0) {
                    
                    // If bytes were read, update the state. 
                    if (actual > 0) {
                        _asyncState.offset += actual;
                        _asyncState.remaining -= actual;
                        _asyncState.total += actual;
                    }
                    
                    // If EOF was received from the stream and some bytes have been read as part of this request,
                    // we must return that number. We'll set the EOF state to RECEIVED, so we return a EOF on the
                    // next request right away.
                    // If EOF was received from the stream an no bytes have been read as part of this request,
                    // we'll report EOF right now.
                    if (actual == EOF) {
                        if (_asyncState.total > 0) {
                            _eofState = EOFState.RECEIVED;
                        }
                        else {
                            _eofState = EOFState.REPORTED;
                        }
                    }
                    
                    // Complete the original future now if any of these is true:
                    //  - We just received EOF from the stream.
                    //  - The caller has requested to complete when AVAILABLE.
                    //  - The requested number of bytes has been read. 
                    if (actual == EOF || _asyncState.completeWhen == CompleteWhen.AVAILABLE || _asyncState.remaining == 0) {
                        
                        // Cache everything that is needed from the state, and delete the state.
                        CompletableFuture<Integer> future = _asyncState.future;
                        int total = _asyncState.total > 0 ? _asyncState.total : EOF;
                        _asyncState = null;
                        
                        // Complete the original future and return. 
                        future.complete(Integer.valueOf(total));
                        return;
                    }
                }
            }
        }
        catch(IOException ex) {
            
            // Upon exception, delete the state, and complete the original future.
            CompletableFuture<Integer> future = _asyncState.future;
            _asyncState = null;
            
            future.completeExceptionally(ex);
        }
        
        // There was nothing available from the stream.
        // First, check how long this request has taken so far.
        // If time is up, complete the original future with a TimeoutException, and return.
        if (_asyncState.timeoutMillis >= 0) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - _asyncState.startTimeMillis > _asyncState.timeoutMillis) {
                CompletableFuture<Integer> future = _asyncState.future;
                _asyncState = null;
                
                future.completeExceptionally(new TimeoutException());
                return;
            }
        }
        
        // Schedule yet another operation.
        ForkJoinPool.commonPool().execute(() -> submitRead());
    }

    /**
     * Internal EOF state.
     */
    private enum EOFState {
        /**
         * EOF has not been received from the stream yet.
         */
        NOT_REACHED,
        
        /**
         * EOF has been received from the stream, but hasn't been reported to the caller yet.
         */
        RECEIVED,
        
        /**
         * EOF has been reported to the caller.
         */
        REPORTED,
    }
    
    /**
     * Internal operation state. 
     */
    private class AsyncState {
        CompletableFuture<Integer> future;
        byte[] buff;
        int offset;
        int remaining;
        int total;
        CompleteWhen completeWhen;
        long startTimeMillis;
        long timeoutMillis;
        
        AsyncState(byte[] buff, int offset, int length, CompleteWhen completeWhen, long timeoutMillis) {
            this.future = new CompletableFuture<Integer>();
            this.buff = buff;
            this.offset = offset;
            this.remaining = length;
            this.total = 0;
            this.completeWhen = completeWhen;
            this.startTimeMillis = System.currentTimeMillis();
            this.timeoutMillis = timeoutMillis;
        }
    }
}
