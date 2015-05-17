package com.jiaxy.dreamwork.assist;

import com.jiaxy.dreamwork.DreamTask;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Title:<br>
 * Desc:<br>
 * <p>
 * </p>
 *
 * @author tbrun
 *
 * @since 2015/05/16 20:43
 */
public class ThresholdUtil {

    private static final Config config = ConfigFactory.load();

    public static final String TASK_NUM = ".num";

    public static final String TASK_TIMEOUT= ".timeout";

    public static final int DEFAULT_TASK_TIMEOUT = 2000;


    /**
     * 容许同时执行此任务的个数
     *
     * @param dream
     * @return
     */
    public static int getTaskNumThreshold(String dream){
        try {
            return config.getInt(dream+TASK_NUM);
        } catch (Exception e) {
            //default number
            return 10;
        }
    }


    /**
     * 任务等待超时时间,如果超时,任务会被丢弃
     *
     * @param dream
     * @return
     */
    public static int getTaskTimeoutThreshold(String dream){
        try {
            return config.getInt(dream+TASK_TIMEOUT);
        } catch (Exception e) {
            //default timeout
            return DEFAULT_TASK_TIMEOUT;
        }
    }


    /**
     * 在等待队列中的任务是否超时
     *
     * @param dreamTask
     * @return
     */
    public static boolean isTimeout(DreamTask dreamTask){
        if ( System.currentTimeMillis() - dreamTask.createTime() > getTaskTimeoutThreshold(dreamTask.dream())){
            return true;
        } else {
            return false;
        }
    }
}
