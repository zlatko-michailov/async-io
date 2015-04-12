package org.michailov.async.io;

/**
 * Options that control the execution of async text stream operations.
 *  
 * @author Zlatko Michailov
 */
public class TextStreamAsyncOptions extends LineAsyncOptions {
    
    /**
     * Default value for {@link #byteRingBufferCapacity} - {@value #DEFAULT_BYTE_RING_BUFFER_CAPACITY} bytes.
     */
    public static final int DEFAULT_BYTE_RING_BUFFER_CAPACITY = 2048;
    
    /**
     * Default value for {@link #charRingBufferCapacity} - {@value #DEFAULT_CHAR_RING_BUFFER_CAPACITY} chars.
     */
    public static final int DEFAULT_CHAR_RING_BUFFER_CAPACITY = 1024;
    
    /**
     * Default value for {@link #stringRingBufferCapacity} - {@value #DEFAULT_STRING_RING_BUFFER_CAPACITY} strings.
     */
    public static final int DEFAULT_STRING_RING_BUFFER_CAPACITY = 64;
    
    /**
     * Capacity of the implicit {@link ByteRingBuffer} used for copying bytes 
     * to/from the target byte stream. 
     * Defaults to {@link #DEFAULT_BYTE_RING_BUFFER_CAPACITY}.
     */
    public int byteRingBufferCapacity = DEFAULT_BYTE_RING_BUFFER_CAPACITY;
    
    /**
     * Capacity of the implicit {@link CharRingBuffer} used for encoding/decoding
     * of char to/from bytes. 
     * Defaults to {@link #DEFAULT_CHAR_RING_BUFFER_CAPACITY}.
     */
    public int charRingBufferCapacity = DEFAULT_CHAR_RING_BUFFER_CAPACITY;
    
    /**
     * Capacity of the implicit {@link StringRingBuffer} used for storing lines.
     * Defaults to {@link #DEFAULT_STRING_RING_BUFFER_CAPACITY}.
     */
    public int stringRingBufferCapacity = DEFAULT_STRING_RING_BUFFER_CAPACITY;
}
