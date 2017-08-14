package scrapper;

public class FailedToFetchDataException extends RuntimeException {
    public FailedToFetchDataException() {
    }

    public FailedToFetchDataException(String s) {
        super(s);
    }

    public FailedToFetchDataException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
