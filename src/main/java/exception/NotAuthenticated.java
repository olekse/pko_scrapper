package exception;

public class NotAuthenticated extends RuntimeException {
    public NotAuthenticated(String s) {
        super(s);
    }
}
