package com.jiaxy.dreamwork;

/**
 * Title:<br>
 * Desc:<br>
 * <p>
 * </p>
 *
 * @author tbrun
 *
 * @since 2015/05/16 18:17
 */
public interface DreamTask extends Runnable{

    /**
     *
     * @return the type of the dream task
     */
    String dream();


    /**
     *
     * @return the first executed time of the dream task
     */
    long firstExecutedTime();


    //void updateFirstExecutedTime(long executedTime);


}
