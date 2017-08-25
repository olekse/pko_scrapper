package exception;

public class WrongLoginFormat extends RuntimeException {
    public WrongLoginFormat(String s) {
        super(s);
    }
}
