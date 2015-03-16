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
