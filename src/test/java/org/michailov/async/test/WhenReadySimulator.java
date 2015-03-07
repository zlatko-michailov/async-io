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
        return simulator._actionCount >= simulator._doneAfterCount;
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
