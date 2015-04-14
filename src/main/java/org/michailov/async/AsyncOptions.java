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

import java.util.concurrent.*;

/**
 * Common options that control the execution of async methods from this package.
 *  
 * @author Zlatko Michailov
 */
public class AsyncOptions {
    
    /**
     * Shared instance that represents the "default" set of options.
     * Use this field to avoid unnecessary heap allocations. 
     */
    public static final AsyncOptions DEFAULT = new AsyncOptions();
    
    /**
     * Sentinel that represents an infinite number of time units.
     */
    public static final long TIMEOUT_INFINITE = -1;
    
    /**
     * Number of time units before which a given async call should complete.
     * Once this time is up, the future returned from the given async call will be completed exceptionally.
     * Default is {@link #TIMEOUT_INFINITE}.
     */
    public long timeout = TIMEOUT_INFINITE;
    
    /**
     * Unit type of the timeout.
     * Default is TimeUnit.MILLISECONDS.
     */
    public TimeUnit timeUnit = TimeUnit.MILLISECONDS;
}
