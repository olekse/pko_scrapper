package util;

public class StopWatch {

    private long startTime;

    public void start(){
        startTime = System.currentTimeMillis();
    }

    public boolean timePassedLessThanMs(long time){
        if (System.currentTimeMillis() < startTime + time){
            return true;
        } else {
            return false;
        }
    }

}
