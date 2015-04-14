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
