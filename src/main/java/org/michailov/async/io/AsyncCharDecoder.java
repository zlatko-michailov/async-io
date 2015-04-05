package org.michailov.async.io;

import java.nio.*;
import java.nio.charset.*;
import org.michailov.async.*;

/**
 * Asynchronous decoder of bytes into chars.
 * 
 * @see     WhenReady
 * @see     AsyncAgent
 * @see     ByteRingBuffer
 * @see     CharRingBuffer
 * 
 * @author  Zlatko Michailov
 */
public class AsyncCharDecoder extends AsyncAgent {

    private static final int MAX_CHAR_BYTES = 8; 
    private static final boolean DECODE_AS_EOF = true;
    
    private final ByteRingBuffer _byteRingBuffer;
    private final CharRingBuffer _charRingBuffer;
    private final Charset _charset;
    private final CharsetDecoder _charsetDecoder;
    private final ByteBuffer _mainByteBuffer;
    private final ByteBuffer _tempByteBuffer;
    private final CharBuffer _mainCharBuffer;
    private volatile boolean _isTempBufferDirty;
    
    /**
     * Constructs an AsyncCharDecoder instance over the given ring buffers.
     * 
     * @param byteRingBuffer        {@link ByteRingBuffer} to read bytes from.
     * @param charRingBuffer        {@link CharRingBuffer} to write char to.
     * @param charsetAsyncOptions   {@link CharsetAsyncOptions} to use for all async operations.
     */
    public AsyncCharDecoder(ByteRingBuffer byteRingBuffer, CharRingBuffer charRingBuffer, CharsetAsyncOptions charsetAsyncOptions) {
        super(charsetAsyncOptions);
        
        Util.ensureArgumentNotNull("byteRingBuffer", byteRingBuffer);
        Util.ensureArgumentNotNull("charRingBuffer", charRingBuffer);
        Util.ensureArgumentNotNull("charsetAsyncOptions", charsetAsyncOptions);
        Util.ensureArgumentNotNull("charsetAsyncOptions.charset", charsetAsyncOptions.charset);
        
        _byteRingBuffer = byteRingBuffer;
        _charRingBuffer = charRingBuffer;
        _charset = charsetAsyncOptions.charset;
        _charsetDecoder = _charset.newDecoder();
        _mainByteBuffer = ByteBuffer.wrap(_byteRingBuffer.getBuffer());
        _tempByteBuffer = ByteBuffer.allocate(MAX_CHAR_BYTES);
        _mainCharBuffer = CharBuffer.wrap(_charRingBuffer.getBuffer());
        _isTempBufferDirty = false;
    }

    /**
     * Returns the attached byte ring buffer from where bytes will be read.
     * 
     * @return  The attached byte ring buffer from where bytes will be read.
     */
    public ByteRingBuffer getByteRingBuffer() {
        return _byteRingBuffer;
    }
    
    /**
     * Returns the attached char ring buffer where bytes will be written.
     * 
     * @return  The attached char ring buffer where bytes will be written.
     */
    public CharRingBuffer getCharRingBuffer() {
        return _charRingBuffer;
    }
    
    /**
     * Checks whether this decoder has reached EOF of the input byte ring buffer.
     * 
     * @return  true iff EOF has been reached or an exception has been encountered.
     */
    public boolean isEOF() {
        return _charRingBuffer.isEOF();
    }

    /**
     * "<i>ready</i>" predicate that returns true when bytes can be read from the input
     * byte ring buffer and chars can be written to the output char ring buffer without blocking.   
     */
    @Override
    protected boolean ready() {
        boolean isReady = false;
        
        try {
            isReady = !isEOF() && _byteRingBuffer.getAvailableToRead() > 0 && _charRingBuffer.getAvailableToWrite() > 0;
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
        
        return isReady;
    }
    
    /**
     * "<i>done</i>" predicate that returns true when this async agent wants to quit the current async loop.   
     */
    @Override
    protected boolean done() {
        boolean isDone = true;
        
        try {
            isDone = isEOF() || (_byteRingBuffer.isEOF() && _byteRingBuffer.getAvailableToRead() == 0);
            if (isDone) {
                setEOF();
            }
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
        
        return isDone;
    }
    
    /**
     * "<i>action</i>" function that reads bytes from the input byte ring buffer and writes chars to the output char ring buffer.   
     */
    @Override
    protected void action() {
        try {
            if (!_isTempBufferDirty) {
                decodeFromMainBuffer();
            }
            else {
                decodeFromTempBuffer();
            }
        }
        catch (Throwable ex) {
            setEOFAndThrow(ex);
        }
    }
    
    private void decodeFromMainBuffer() {
        // Take a snapshot of the real buffers.
        int readPosition = _byteRingBuffer.getReadPosition();
        int readStraight = _byteRingBuffer.getAvailableToReadStraight();
        int writePosition = _charRingBuffer.getWritePosition();
        int writeStraight = _charRingBuffer.getAvailableToWriteStraight();

        // Map the virtual decoder buffers over the native buffers. 
        _mainByteBuffer.position(readPosition);
        _mainByteBuffer.limit(readPosition + readStraight);
        _mainCharBuffer.position(writePosition);
        _mainCharBuffer.limit(writePosition + writeStraight);
        
        // Attempt to decode.
        _charsetDecoder.reset();
        CoderResult coderResult = _charsetDecoder.decode(_mainByteBuffer, _mainCharBuffer, DECODE_AS_EOF);
        
        // Advance positions of physical buffers based on the state of the virtual buffers.
        int newReadPosition = _mainByteBuffer.position();
        int readDelta = newReadPosition - readPosition;
        if (readDelta > 0) {
            _byteRingBuffer.advanceReadPosition(readDelta);
        }
        
        int newWritePosition = _mainCharBuffer.position();
        int writeDelta = newWritePosition - writePosition;
        if (writeDelta > 0) {
            _charRingBuffer.advanceWritePosition(writeDelta);
        }
        
        // If input was malformed AND this is the physical end of the main buffer,
        // decoding must transition to the temp buffer.
        if (coderResult.isMalformed() && (_mainByteBuffer.limit() == _mainByteBuffer.capacity())) {
            // The temp buffer must be initialized exactly once.
            // Therefore, it can't be done in the loop. It must be done here.

            // Copy the undecoded remainder to the temp buffer.
            int remainder = _mainByteBuffer.limit() - newReadPosition;
            _tempByteBuffer.position(0);
            _tempByteBuffer.limit(_tempByteBuffer.capacity());
            _tempByteBuffer.put(_mainByteBuffer.array(), newReadPosition, remainder);
            
            // Advance the real byte ring buffer's read position because 
            // the remainder wasn't counted towards the read delta. 
            _byteRingBuffer.advanceReadPosition(remainder);
            
            // Reset the temp buffer positions.
            _tempByteBuffer.position(0);
            _tempByteBuffer.limit(remainder);

            // Transition to temp buffer.
            _isTempBufferDirty = true;
        }
        // If it is hopeless to make progress due to a stale remainder, complete. 
        else if (readDelta == 0 && _byteRingBuffer.isEOF() && _byteRingBuffer.getAvailableToReadStraight() == readStraight) {
            setEOF();
        }
    }
    
    private void decodeFromTempBuffer() {
        // Take a snapshot of the real char buffer.
        // (The real byte buffer is not needed now.)
        int writePosition = _charRingBuffer.getWritePosition();
        int writeStraight = _charRingBuffer.getAvailableToWriteStraight();

        // Map the virtual decoder char buffer over the native char buffer. 
        _mainCharBuffer.position(writePosition);
        _mainCharBuffer.limit(writePosition + writeStraight);

        // Copy one more byte from the real byte buffer to the temp buffer.
        // RingBuffer.read() automatically adjusts the read position.
        int b = _byteRingBuffer.read();
        int oldLimit = _tempByteBuffer.limit();
        _tempByteBuffer.limit(oldLimit + 1);
        _tempByteBuffer.put(oldLimit, (byte)b);
        _tempByteBuffer.position(0);
        
        // Attempt to decode.
        _charsetDecoder.reset();
        CoderResult coderResult = _charsetDecoder.decode(_tempByteBuffer, _mainCharBuffer, DECODE_AS_EOF);

        // Adjust the position on the physical char buffer. 
        int newWritePosition = _mainCharBuffer.position();
        int writeDelta = newWritePosition - writePosition;
        if (writeDelta > 0) {
            _charRingBuffer.advanceWritePosition(writeDelta);
        }
            
        // If decoding succeeded, it must continue from the main buffer.
        if (!coderResult.isError()) {
            _isTempBufferDirty = false;
        }
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying output char ring buffer to true. 
     * Marks this instance as 'idle'.
     */
    private void setEOF() {
        _charRingBuffer.setEOF();
        setIdle();
    }
    
    /**
     * Sets the EOF status on this instance as well as on the underlying output char ring buffer to true. 
     * Throws an {@link AsyncException} to notify the {@link WhenReady} framework that something has gone wrong.  
     * Marks this instance as 'idle'.
     * 
     * @param   ex  An exception to complete with.
     */
    private void setEOFAndThrow(Throwable ex) {
        _charRingBuffer.setEOF();
        setIdleAndThrow(ex);
    }
    
}
