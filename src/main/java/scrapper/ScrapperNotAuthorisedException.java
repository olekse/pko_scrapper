package scrapper;

public class ScrapperNotAuthorisedException extends RuntimeException{
    public ScrapperNotAuthorisedException() {
    }

    public ScrapperNotAuthorisedException(String s) {
        super(s);
    }

    public ScrapperNotAuthorisedException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
