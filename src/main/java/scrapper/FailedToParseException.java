package scrapper;

public class FailedToParseException extends RuntimeException {
    public FailedToParseException(String s) {
        super(s);
    }

    public FailedToParseException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
