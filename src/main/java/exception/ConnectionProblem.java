package exception;

public class ConnectionProblem extends RuntimeException {
    public ConnectionProblem(String s) {
        super(s);
    }

    public ConnectionProblem(String s, Throwable throwable) {
        super(s, throwable);
    }
}
