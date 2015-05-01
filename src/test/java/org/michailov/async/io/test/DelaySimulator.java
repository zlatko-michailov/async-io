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

class DelaySimulator {
    private final int _chunkLength;
    private final int _delayMillis;
    private int _nextChunkIndex;
    private long _lastTimeMillis;
    private boolean _isStarted;

    DelaySimulator(int chunkLength, int delayMillis) {
        _chunkLength = chunkLength;
        _delayMillis = delayMillis;
        _nextChunkIndex = 0;
        _lastTimeMillis = 0;
        _isStarted = false;
    }

    void advance() {
        // Block if necessary.
        long blockingMillis = getBlockingMillis();
        if (blockingMillis > 0) {
            try {
                Thread.sleep(blockingMillis);
            }
            catch (InterruptedException ex) {
            }
        }
        
        // We have started.
        _isStarted = true;
        
        // Advance the chunk index.
        if (++_nextChunkIndex == _chunkLength) {
            _nextChunkIndex = 0;
        }
        
        // Take the current advancement time.
        _lastTimeMillis = System.currentTimeMillis();
    }
    
    boolean isChunkingEnabled() {
        return _chunkLength > 0;
    }
    
    int getAvailable() {
        // If it's time to block, nothing is available.
        // Otherwise, the rest of the chunk is available.
        return getBlockingMillis() > 0 ? 0 : _chunkLength - _nextChunkIndex;
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
        
        // If we haven't started yet, we won't block now.
        if (!_isStarted) {
            return 0;
        }
        
        // We are at chunk boundary.
        // The blocking time is the time left from the last advancement up to a chunk delay.
        long currentTimeMillis = System.currentTimeMillis();
        long currentDelayMillis = currentTimeMillis - _lastTimeMillis;
        
        // If more time than the desired delay has already passed, we won't block.
        if (currentDelayMillis >= _delayMillis) {
            return 0;
        }
        
        // We'll block for the remainder.
        return _delayMillis - currentDelayMillis;
    }
    
}
