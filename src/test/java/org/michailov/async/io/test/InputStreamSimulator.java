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

package org.michailov.async.io.test;

import java.io.*;
import java.nio.charset.*;
import java.util.concurrent.*;
import org.michailov.async.io.*;

class InputStreamSimulator extends InputStream {
    
    static final String[] LINES = {
        "",
        "",
        "",
        "Добре дошли",
        "Welcome",
        "",
        "в",
        "to",
        "очарователния",
        "the exciting",
        "",
        "",
        "свят",
        "world",
        "на",
        "of",
        "асинхронното",
        "asynchronous",
        "",
        "програмиране",
        "programming",
        "!",
        "",
        "",
    };
    static final String[] LINE_BREAKS = {
        LineAsyncOptions.CRLF,
        LineAsyncOptions.LF, // Keep LF in front of CR to avoid missing some empty lines!
        LineAsyncOptions.CR,
    };
    static final Charset CHARSET = StandardCharsets.UTF_8;
    static final String CONTENT;
    static final byte[] CONTENT_BYTES;
    static final int CONTENT_BYTES_LENGTH;
    
    private static final int EOF = -1;
    
    static {
        StringBuilder content = new StringBuilder(500);
        int lineBreaksLength = LINE_BREAKS.length;
        
        for (int i = 0; i < LINES.length; i++) {
            content.append(LINES[i]);
            content.append(LINE_BREAKS[i % lineBreaksLength]);
        }
        
        CONTENT = content.toString();
        CONTENT_BYTES = CONTENT.getBytes(CHARSET);
        CONTENT_BYTES_LENGTH = CONTENT_BYTES.length;
    }
    
    private final int _streamLength;
    private final int _chunkLength;
    private final int _delayMillis;
    private int _nextStreamIndex;
    private int _nextContentIndex;
    private int _nextChunkIndex;
    private long _lastReadTimeMillis;
    private boolean _isEOF;

    InputStreamSimulator(int streamLength, int chunkLength, int delay, TimeUnit unit) {
        _streamLength = streamLength;
        _chunkLength = chunkLength;
        _delayMillis = (int)unit.toMillis(delay);
        _nextStreamIndex = 0;
        _nextContentIndex = 0;
        _nextChunkIndex = 0;
        _lastReadTimeMillis = 0;
        _isEOF = false;
    }

    @Override
    public int available() throws IOException {
        // If we've already returned EOF, nothing is available.
        if (_isEOF) {
            return 0;
        }
        
        // If we've reached the end of the stream, there are no more bytes available.
        // However, the caller hasn't gotten an EOF yet. 
        // Lie to him to come and get the EOF.
        if (_nextStreamIndex == _streamLength) {
            return 1;
        }
        
        // If chunking is not enabled, the rest of the entire stream is available.
        if (_chunkLength <= 0) {
            return _streamLength - _nextStreamIndex;
        }
        
        // If we are supposed to block now, then nothing is available.
        long blockingMillis = getBlockingMillis();
        if (blockingMillis > 0) {
            return 0;
        }
        
        // The rest of the chunk is available.
        return _chunkLength - _nextChunkIndex;
    }
    
    @Override
    public int read() throws IOException {
        int r = readByte();
        
        // If we publicly return EOF, set the flag. 
        if (r == EOF) {
            _isEOF = true;
        }
        
        return r;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            int r = readByte();
            
            if (r == EOF) {
                if (i == 0) {
                    // We are publicly returning EOF. Set the flag.
                    _isEOF = true;
                    return EOF;
                }
                else {
                    // If we reach EOF as part of a read, we'll return a smaller number than requested,
                    // but we still haven't told the caller anything about EOF.
                    return i;
                }                    
            }
            
            b[off + i] = (byte)r;
        }
        
        return len;
    }

    private int readByte() throws IOException {
        // If we've reached the end of the stream, return EOF.
        if (_nextStreamIndex >= _streamLength) {
            return EOF;
        }
        
        // If chunking is enabled and we are at a chunk boundary (except at the beginning of the stream), we may have to block.
        if (_chunkLength > 0 && _nextChunkIndex == 0 && _nextStreamIndex > 0) {
            long blockingMillis = getBlockingMillis();
            if (blockingMillis > 0) {
                try {
                    Thread.sleep(blockingMillis);
                }
                catch (InterruptedException ex) {
                }
            }
        }
        
        // Cache the content byte we are supposed to return.
        byte r = CONTENT_BYTES[_nextContentIndex];
        
        // Move indexes.
        if (++_nextContentIndex == CONTENT_BYTES_LENGTH) {
            _nextContentIndex = 0;
        }
        if (++_nextChunkIndex == _chunkLength) {
            _nextChunkIndex = 0;
        }
        ++_nextStreamIndex;
        _lastReadTimeMillis = System.currentTimeMillis();

        // Return the cached byte.
        return r;
    }

    private long getBlockingMillis() {
        // If chunking is not enabled, we'll never block.
        if (_chunkLength <= 0) {
            return 0;
        }
        
        // If we are not at a chunk boundary, we won't block now.
        if (_nextChunkIndex != 0) {
            return 0;
        }
        
        // If we are at the beginning of the stream, we won't block now.
        if (_nextStreamIndex == 0) {
            return 0;
        }
        
        // We are at chunk boundary.
        // The blocking time is the time left from the last read up to a chunk delay.
        long currentTimeMillis = System.currentTimeMillis();
        long millisSinceLastRead = currentTimeMillis - _lastReadTimeMillis;
        
        // If more time than the desired delay has already passed, we won't block.
        if (millisSinceLastRead >= _delayMillis) {
            return 0;
        }
        
        // We'll block for the remainder.
        return _delayMillis - millisSinceLastRead;
    }
    
}
