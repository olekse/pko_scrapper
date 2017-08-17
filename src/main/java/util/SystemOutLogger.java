package util;

public class SystemOutLogger implements Logger {
    @Override
    public <T> void log(T line) {
        System.out.println(line);
    }
}
