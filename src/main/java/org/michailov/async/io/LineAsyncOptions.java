package org.michailov.async.io;

/**
 * Options that control the execution of async line splitting and ending.
 *  
 * @author Zlatko Michailov
 */
public class LineAsyncOptions extends CharsetAsyncOptions {

    /**
     * The CR char (\u000d).
     */
    public static final String CR = "\r";
    
    /**
     * The LF char (\u000a).
     */
    public static final String LF = "\n";
    
    /**
     * The CR and LF chars (\u000d\u000a).
     */
    public static final String CRLF = "\r\n";
    
    /**
     * The system-specific value. See the "<i>line.separator</i>" system property.  
     */
    public static final String SYSTEM = System.getProperty("line.separator");
    
    /**
     * Default estimated line length in chars - {@value #DEFAULT_ESTIMATED_LINE_LENGTH} chars.
     */
    public static final int DEFAULT_ESTIMATED_LINE_LENGTH = 1024;

    /**
     * Sequence of chars that gets appended to each line. 
     * The default is {@link #SYSTEM}.
     */
    public String lineBreak = SYSTEM;
    
    /**
     * Estimated line length in chars.
     * This value is used to initialize the input StringBuilder.
     * A line may exceed this value. No content will be lost. Only a re-allocation will occur.
     * The default is {@link #DEFAULT_ESTIMATED_LINE_LENGTH}.
     */
    public int estimatedLineLength = DEFAULT_ESTIMATED_LINE_LENGTH;
}
