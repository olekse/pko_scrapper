package scrapper;

public class WrongAccountNumberException extends RuntimeException {
    public WrongAccountNumberException(String s) {
        super(s);
    }
}
