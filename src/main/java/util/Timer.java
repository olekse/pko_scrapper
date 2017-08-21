package util;

public class Timer {

    private long startTime;

    public void reset(){
        startTime = System.currentTimeMillis();
    }

    public long getElapsedTimeMs(){
        return System.currentTimeMillis() - startTime;
    }

    public boolean timePassedMoreThanMs(long time){
        return !timePassedLessThanMs(time);
    }

    public boolean timePassedLessThanMs(long time){
        if (System.currentTimeMillis() < startTime + time){
            return true;
        } else {
            return false;
        }
    }

}
