package org.michailov.async.io;

import java.nio.charset.*;

import org.michailov.async.*;

public class CharsetAsyncOptions extends AsyncOptions {
    public static final CharsetAsyncOptions DEFAULT = new CharsetAsyncOptions();
    
    public Charset charset = StandardCharsets.US_ASCII;
}
