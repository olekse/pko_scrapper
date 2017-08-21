package scrapper;

public class ScrapperNotAuthenticatedException extends RuntimeException{
    public ScrapperNotAuthenticatedException(String s) {
        super(s);
    }
}
