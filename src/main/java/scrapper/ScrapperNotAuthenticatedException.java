package scrapper;

public class ScrapperNotAuthenticatedException extends RuntimeException{
    public ScrapperNotAuthenticatedException() {
    }

    public ScrapperNotAuthenticatedException(String s) {
        super(s);
    }

    public ScrapperNotAuthenticatedException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
