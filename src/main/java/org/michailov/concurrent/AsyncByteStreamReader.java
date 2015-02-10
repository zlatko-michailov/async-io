package org.michailov.concurrent;

import java.io.*;
import java.util.concurrent.*;

/**
 * @author Zlatko Michailov
 *
 */
public class AsyncByteStreamReader {
    private static final long INFINITE = -1;
    
    private final InputStream _stream;
    private State _state;
    
    public AsyncByteStreamReader(InputStream stream) {
        _stream = stream;
    }
    
    public InputStream getStream() {
        return _stream;
    }

    public CompletableFuture<Integer> readAsync(byte[] buff, int offset, int length) {
        return readAsync(buff, offset, length, CompleteWhen.AVAILABLE, INFINITE, TimeUnit.MILLISECONDS);
    }
    
    public CompletableFuture<Integer> readAsync(byte[] buff, int offset, int length, CompleteWhen completeWhen) {
        return readAsync(buff, offset, length, completeWhen, INFINITE, TimeUnit.MILLISECONDS);
    }
    
    public CompletableFuture<Integer> readAsync(byte[] buff, int offset, int length, CompleteWhen completeWhen, long timeout, TimeUnit unit) {
        if (_state != null) {
            throw new IllegalStateException("There is already a readAsync operation in progress. Await for the returned task to complete, and retry.");
        }
        
        if (buff == null) {
            throw new IllegalArgumentException("Argument buff may not be null.");
        }
        
        if (offset < 0 || buff.length <= offset) {
            String message = String.format("Argument offset (%1$d) must be between 0 and %2$d, or the length of buff must be increased.", offset, buff.length - 1);
            throw new IllegalArgumentException(message);
        }
        
        if (length < 0 || buff.length <= offset + length) {
            String message = String.format("Argument length (%1$d) must be between 0 and %2$d, or the length of buff must be increased.", offset, buff.length - 1 - offset);
            throw new IllegalArgumentException(message);
        }
        
        if (length == 0) {
            return CompletableFuture.completedFuture(Integer.valueOf(0));
        }

        _state = new State(buff, offset, length, completeWhen, unit.toMillis(timeout));
        submitReadChain();
        
        return _state.future;
    }
    
    private void submitReadChain() {
        try {
            int available = _stream.available();
            if (available > 0) {
                int target = Math.min(available, _state.remaining);
                int actual = _stream.read(_state.buff, _state.offset, target);
                if (actual > 0) {
                    _state.offset += actual;
                    _state.remaining -= actual;
                    _state.total += actual;
                    
                    if (_state.completeWhen == CompleteWhen.AVAILABLE || (_state.completeWhen == CompleteWhen.FULL && _state.remaining == 0)) {
                        CompletableFuture<Integer> future = _state.future;
                        int total = _state.total;
                        _state = null;
                        
                        future.complete(Integer.valueOf(total));
                    }
                }
            }
        }
        catch(IOException ex) {
            CompletableFuture<Integer> future = _state.future;
            _state = null;
            
            future.completeExceptionally(ex);
        }
        
        if (_state.timeoutMillis >= 0) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - _state.startTimeMillis > _state.timeoutMillis) {
                CompletableFuture<Integer> future = _state.future;
                _state = null;
                
                future.completeExceptionally(new TimeoutException());
            }
        }
        
        ForkJoinPool.commonPool().execute(() -> submitReadChain());
    }

    public enum CompleteWhen {
        AVAILABLE,
        FULL,
    }
    
    private class State {
        CompletableFuture<Integer> future;
        byte[] buff;
        int offset;
        int remaining;
        int total;
        CompleteWhen completeWhen;
        long startTimeMillis;
        long timeoutMillis;
        
        State(byte[] buff, int offset, int length, CompleteWhen completeWhen, long timeoutMillis) {
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
