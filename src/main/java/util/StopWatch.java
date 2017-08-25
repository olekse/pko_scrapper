package util;

public class StopWatch {

    private long startTime;

    public StopWatch(){
        startTime = System.currentTimeMillis();
    }

    public boolean isTimePassedLessThanMs(long time){
        if (System.currentTimeMillis() < startTime + time){
            return true;
        } else {
            return false;
        }
    }

}
