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

package org.michailov.async.test;

class WhenReadySimulator {

    private final int _readyAfterCount;
    private final int _doneAfterCount;
    private final long _actionMillis;
    
    private int _readyCount;
    private int _actionCount;
    
    WhenReadySimulator(int readyAfterCount, int doneAfterCount, long actionMillis) {
        _readyAfterCount = readyAfterCount;
        _doneAfterCount = doneAfterCount;
        _actionMillis = actionMillis;
        
        _readyCount = 0;
        _actionCount = 0;
    }
    
    int getReadyCount() {
        return _readyCount;
    }
    
    int getActionCount() {
        return _actionCount;
    }
    
    static boolean ready(WhenReadySimulator simulator) {
        sleep(simulator._actionMillis);
        return ++simulator._readyCount >= simulator._readyAfterCount;
    }
    
    static boolean done(WhenReadySimulator simulator) {
        return simulator._doneAfterCount > 0 && simulator._actionCount >= simulator._doneAfterCount;
    }
    
    static int action(WhenReadySimulator simulator) {
        sleep(simulator._actionMillis);
        return ++simulator._actionCount;
    }
    
    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (Exception ex) {
        }
    }
}
