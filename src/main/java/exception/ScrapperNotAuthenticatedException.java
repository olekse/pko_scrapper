package exception;

public class ScrapperNotAuthenticatedException extends RuntimeException{
    public ScrapperNotAuthenticatedException(String s) {
        super(s);
    }
}
