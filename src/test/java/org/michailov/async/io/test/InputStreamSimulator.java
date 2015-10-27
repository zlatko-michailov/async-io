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

class InputStreamSimulator extends EOFInputStream {
    
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
    
    private final DelaySimulator _delaySimulator;
    private final int _streamLength;
    private int _nextStreamIndex;
    private int _nextContentIndex;
    private boolean _isEOF;

    InputStreamSimulator(int streamLength, int chunkLength, int delay, TimeUnit unit) {
        super();
        
        _delaySimulator = new DelaySimulator(chunkLength, (int)unit.toMillis(delay));
        _streamLength = streamLength;
        _nextStreamIndex = 0;
        _nextContentIndex = 0;
        _isEOF = false;
    }
    
    @Override
    public boolean eof() {
        if (_nextStreamIndex >= _streamLength) {
            _isEOF = true;
        }
        
        return _isEOF;
    }

    @Override
    public int available() throws IOException {
        // If we've reached EOF, nothing is available.
        if (eof()) {
            return 0;
        }
        
        // If chunking is not enabled, the rest of the entire stream is available.
        if (!_delaySimulator.isChunkingEnabled()) {
            return _streamLength - _nextStreamIndex;
        }
        
        // Ask the delay simulator what is available.
        return _delaySimulator.getAvailable();
    }
    
    @Override
    public int read() throws IOException {
        int r = readByte();
        
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
            _isEOF = true;
            return EOF;
        }
        
        // Cache the content byte we are supposed to return.
        byte r = CONTENT_BYTES[_nextContentIndex];

        // BLock if needed.
        _delaySimulator.advance();
        
        // Advance indexes.
        ++_nextStreamIndex;
        if (++_nextContentIndex == CONTENT_BYTES_LENGTH) {
            _nextContentIndex = 0;
        }

        // Return the cached byte.
        return r;
    }
    
}
