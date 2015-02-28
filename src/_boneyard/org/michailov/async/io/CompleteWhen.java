// TODO: Remove the whole file.

package org.michailov.async.io;

/**
 * Allows the caller to request when an asynchronous read operation should complete. 
 * 
 * @author Zlatko Michilov <zlatko+async@michailov.org>
 */
public enum CompleteWhen {
    /**
     * The operation should complete as soon as the first batch of available bytes is read from the stream.
     */
    AVAILABLE,
    
    /**
     * The operation should complete when the buffer is full (or end of stream is reached meanwhile.)
     */
    FULL,
}

