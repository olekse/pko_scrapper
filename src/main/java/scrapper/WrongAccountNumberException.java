package scrapper;

public class WrongAccountNumberException extends RuntimeException {
    public WrongAccountNumberException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public WrongAccountNumberException(String s) {
        super(s);
    }

    public WrongAccountNumberException(){
        super();
    }
}
