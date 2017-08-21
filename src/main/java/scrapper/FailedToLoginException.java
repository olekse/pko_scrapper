package scrapper;

public class FailedToLoginException extends RuntimeException {
    public FailedToLoginException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
