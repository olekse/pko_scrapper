package exception;

public class WrongAccountNumber extends RuntimeException {
    public WrongAccountNumber(String s) {
        super(s);
    }
}
