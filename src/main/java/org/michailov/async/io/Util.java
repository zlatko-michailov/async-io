package org.michailov.async.io;

class Util {

    static final int EOF = -1;
    
    static void ensureArgumentNotNull(String argName, Object argValue) {
        if (argValue == null) {
            throw new IllegalArgumentException(String.format("Argument %1$s may not be null.", argName));
        }
    }
    
}
