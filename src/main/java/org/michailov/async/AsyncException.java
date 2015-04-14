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

/**
 * This unchecked exception type should be used by predicates and functions to report checked exceptions to the
 * {@link WhenReady} framework is not possible from lambdas.
 * This exception type doesn't add any new properties. It should be used as wrapper around the actual exception.
 * See {@link RuntimeException} for information on each overload. 
 * 
 * @author Zlatko Michailov
 */
public class AsyncException extends RuntimeException {

    public AsyncException() {
    }

    public AsyncException(String message) {
        super(message);
    }

    public AsyncException(Throwable cause) {
        super(cause);
    }

    public AsyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsyncException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    private static final long serialVersionUID = 42L;

}
