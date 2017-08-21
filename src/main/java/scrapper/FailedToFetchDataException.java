package scrapper;

public class FailedToFetchDataException extends RuntimeException {
    public FailedToFetchDataException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
