package scrapper;

public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException(String s) {
        super(s);
    }

    public WrongPasswordException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public WrongPasswordException(){
        super();
    }
}
