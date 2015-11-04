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

package org.michailov.async.io;

import java.io.*;
import java.util.function.*;

/**
 * An InputStream that can explicitly report EOF. 
 * <p>
 * The built-in InputStream imposes a problem for clients that don't want to block.
 * A client doesn't know what to do when available == 0 - does it mean "fetching more data" or "EOF".
 * It is possible for an InputStream to support this scenario - the InputStream can "lie" and report
 * 1 available byte and when the client comes to read it, it will get -1 right away.
 * However, neither FileInputStream nor the InputStream from Process.getInputStream() do that.
 * <p>
 * This class enables clients to use their own information on whether EOF of the InputStream has been reached.
 * <p>
 * Optionally, clients may modify the available behavior.
 * <p>
 * This class offers static factory methods for creating EOFInputStream instances from File and from Process.
 * 
 * @author  Zlatko Michailov
 */
public class EOFInputStream extends InputStream {

    private InputStream _inputStream;
    private Predicate<AsyncByteStreamReader> _eof;
    private ToIntFunction<AsyncByteStreamReader> _available;
    private AsyncByteStreamReader _reader;

    /**
     * Constructs a new EOFInputStream. 
     *  
     * @param   inputStream     InputStream to override.
     * @param   eof             EOF predicate. Takes a {@link AsyncByteStreamReader} as a parameter.
     *                          It could be used to get hold of the related {@link ByteRingBuffer} to get the 
     *                          total count of read bytes or to obtain other related objects.
     */
    public EOFInputStream(InputStream inputStream, Predicate<AsyncByteStreamReader> eof) {
        this(inputStream, eof, null);
    }
    
    /**
     * Constructs a new EOFInputStream.
     * 
     * @param   inputStream     InputStream to override.
     * @param   eof             EOF predicate. Takes a {@link AsyncByteStreamReader} as a parameter.
     *                          It could be used to get hold of the related {@link ByteRingBuffer} to get the 
     *                          total count of read bytes or to obtain other related objects.
     * @param   available       available function. Takes a {@link AsyncByteStreamReader} as a parameter.
     *                          It could be used to get hold of the related {@link ByteRingBuffer} to get the 
     *                          total count of read bytes or to obtain other related objects.
     */
    public EOFInputStream(InputStream inputStream, Predicate<AsyncByteStreamReader> eof, ToIntFunction<AsyncByteStreamReader> available) {
        Util.ensureArgumentNotNull("inputStream", inputStream);
        Util.ensureArgumentNotNull("eof", eof);
        
        _inputStream = inputStream;
        _eof = eof;
        _available = available;
    }
    
    /**
     * Implicit constructor for derived classes.
     */
    protected EOFInputStream() {
    }
    
    /**
     * An entry point for the {@link AsyncByteStreamReader} to register itself with this EOFInputStream.
     * 
     * @param   reader          The {@link AsyncByteStreamReader} that is consuming this EOFInputStream.
     *                          This object is passed to the eof() predicate and to the available() function.
     *                          It could be used to get hold of the related {@link ByteRingBuffer} to get the 
     *                          total count of read bytes or to obtain other related objects.
     */
    void setReader(AsyncByteStreamReader reader) {
        Util.ensureArgumentNotNull("reader", reader);
        
        _reader = reader;
    }
    
    /**
     * Used by the consuming {@link AsyncByteStreamReader} to check whether EOF has been reached.
     * Invokes the eof() predicate passing the consuming {@link AsyncByteStreamReader}.
     *  
     * @return  The value of the eof() predicate which should be true iff EOF has been reached.
     */
    public boolean eof() {
        return _eof.test(_reader);
    }
    
    /**
     * @see InputStream
     */
    @Override
    public int available() throws IOException {
        if (_available != null) {
            return _available.applyAsInt(_reader);
        }
        else {
            return _inputStream.available();
        }
    }
    
    /**
     * @see InputStream
     */
    @Override
    public int read() throws IOException {
        return _inputStream.read();
    }
    
    /**
     * @see InputStream
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return _inputStream.read(b, off, len);
    }
    
    /**
     * Factory method for Process.getInputStream().
     * <p>
     * eof() returns true iff Process.isAlive() returns false.
     * 
     * @param   process     The Process whose InputStream will be read.
     * @return              A new EOFInputStream instance.
     */
    public static EOFInputStream fromProcess(Process process) {
        Util.ensureArgumentNotNull("process", process);
        
        return new EOFInputStream(process.getInputStream(), reader -> eofProcess(reader, process));
    }

    /**
     * Factory method for Process.getInputStream().
     * <p>
     * eof() returns true iff File.length bytes have been read by the consuming {@link AsyncByteStreamReader}.
     * 
     * @param   file                    The File whose FileInputStream will be read.
     * @return                          A new EOFInputStream instance.
     * @throws FileNotFoundException    @see FileInputStream. 
     */
    public static EOFInputStream fromFile(File file) throws FileNotFoundException {
        Util.ensureArgumentNotNull("file", file);
        
        return new EOFInputStream(new FileInputStream(file), reader -> eofFile(reader, file));
    }
    
    /**
     * Returns true iff Process.isAlive() returns false.
     */
    private static boolean eofProcess(AsyncByteStreamReader byteReader, Process process) {
        try {
            return byteReader.getInputStream().available() == 0 && !process.isAlive();
        }
        catch (Throwable ex) {
            return true;
        }
    }

    /**
     * Returns true iff File.length bytes have been read by the consuming {@link AsyncByteStreamReader}.
     */
    private static boolean eofFile(AsyncByteStreamReader byteReader, File file) {
        try {
            return byteReader.getInputStream().available() == 0 && byteReader.getByteRingBuffer().getTotalReadCount() == file.length();
        }
        catch (Throwable ex) {
            return true;
        }
    }

}
