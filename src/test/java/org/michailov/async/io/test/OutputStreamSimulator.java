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
import java.util.concurrent.*;

class OutputStreamSimulator extends OutputStream {

    private final DelaySimulator _delaySimulator;
    private final byte[] _contentBytes;
    private final int _streamLength;
    private int _nextStreamIndex;

    OutputStreamSimulator(int streamLength, int chunkLength, int delay, TimeUnit unit) {
        _delaySimulator = new DelaySimulator(chunkLength, (int)unit.toMillis(delay));
        _contentBytes = new byte[streamLength];
        _streamLength = streamLength;
        _nextStreamIndex = 0;
    }
    
    byte[] getContentBytes() {
        return _contentBytes;
    }
    
    int getStreamLength() {
        return _streamLength;
    }
    
    int getNextStreamIndex() {
        return _nextStreamIndex;
    }

    @Override
    public void write(int b) throws IOException {
        if (_nextStreamIndex >= _streamLength) {
            throw new IOException("Test error - writing more bytes than declared.");
        }
        
        // Store the byte.
        _contentBytes[_nextStreamIndex++] = (byte)b;
        
        // Advance the delay simulator.
        _delaySimulator.advance();
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }

}
