package scrapper;

public class FailedToLoginException extends RuntimeException {
    public FailedToLoginException() {
    }

    public FailedToLoginException(String s) {
        super(s);
    }

    public FailedToLoginException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
