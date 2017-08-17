package experimental;

public class FailedToParseException extends RuntimeException {
    public FailedToParseException() {
    }

    public FailedToParseException(String s) {
        super(s);
    }

    public FailedToParseException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
