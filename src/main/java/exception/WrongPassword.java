package exception;

public class WrongPassword extends RuntimeException {
    public WrongPassword(String s) {
        super(s);
    }
}
