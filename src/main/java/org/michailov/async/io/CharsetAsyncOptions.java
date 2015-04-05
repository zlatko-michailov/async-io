package org.michailov.async.io;

import java.nio.charset.*;
import org.michailov.async.*;

/**
 * Options that control the execution of async char encoding and decoding.
 *  
 * @author Zlatko Michailov
 */
public class CharsetAsyncOptions extends AsyncOptions {
    /**
     * Shared instance that represents the "default" set of options.
     * Use this field to avoid unnecessary heap allocations. 
     */
    public static final CharsetAsyncOptions DEFAULT = new CharsetAsyncOptions();
    
    /**
     * Charset for the encoded/decoded bytes.
     * The default is ASCII.
     */
    public Charset charset = StandardCharsets.US_ASCII;
}
