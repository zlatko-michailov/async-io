package org.michailov.async;

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
