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

package org.michailov.async;

import java.io.*;
import java.util.logging.*;

/**
 * Logger customized for the needs of this project.
 * <p>
 * This class is intended for internal purposes only.
 * 
 * @author Zlatko Michailov
 */
public class Logger extends java.util.logging.Logger {
    private static final String LOGGER_NAME = Logger.class.getPackage().getName();
    private static final String FILE_PATH = String.format("%%t/%1$s-%2$tY%2$tm%2$td-%2$tH%2$tM.csv", LOGGER_NAME, System.currentTimeMillis());
    private static final Object SYNC = new Object();
    
    private static boolean _isInitialized = false;
    private static Logger _logger;
    
    /**
     * Gets the shared instance.
     *
     *  @return         The shared instance.
     */
    public static Logger getLogger() {
        ensureInitialized();
        
        return _logger;
    }
    
    /**
     * This class cannot be instantiated directly.
     */
    private Logger() {
        super(LOGGER_NAME, null);
    }
    
    /**
     * Ensures the instance/class is initialized.
     * If it is already initialized, does nothing. 
     */
    private static void ensureInitialized() {
        // Quick check.
        if (!_isInitialized) {
            synchronized (SYNC) {
                // Check for sure.
                if (!_isInitialized) {
                    initialize();
                }
            }
        }
    }

    /**
     * Unconditionally initializes this class.
     * This method is not thread-safe. SYNC must be acquired before calling this method.
     */
    private static void initialize() {
        try {
            CustomFormatter formatter = new CustomFormatter();

            FileHandler handler = new FileHandler(FILE_PATH);
            handler.setFormatter(formatter);
            
            Logger logger = new Logger();
            logger.addHandler(handler);
            logger.setLevel(Level.FINEST);
            
            _logger = logger;
            _isInitialized = true;
            
            String message = String.format("%2$s logs are located at: %1$s%2$s-*.csv", System.getProperty("java.io.tmpdir"), LOGGER_NAME);
            Logger.getGlobal().info(message);
        }
        catch (IOException ex) {
            String message = String.format("Couldn't create logger %1$s. Exception caught: %2$s\n", LOGGER_NAME, ex.toString());
            Logger.getGlobal().severe(message);
        }
    }
}


/**
 * Custom simple formatter.
 */
class CustomFormatter extends SimpleFormatter {
    private static final String FORMAT = "%1$tF %1$tT , %2$s , %3$s.%4$s , %5$s %n";

    public String format(LogRecord record) {
        return String.format(FORMAT, record.getMillis(), record.getLevel(), record.getSourceClassName(), record.getSourceMethodName(), record.getMessage());
    }
}

