package com.jiaxy.dreamwork.assist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Title:<br>
 * Desc:<br>
 * <p>
 *  线程池中运行的各种任务的情况
 * </p>
 *
 * @author tbrun
 *
 * @since 2015/05/16 18:45
 */
public class IncubatorStat {

    private Logger logger = LoggerFactory.getLogger(IncubatorStat.class);


    // key:the value of DreamTask.dream() returned value;value :running task number
    private ConcurrentHashMap<String,AtomicInteger> taskNumMap = new ConcurrentHashMap<String, AtomicInteger>();


    public int getRunningTaskNum(String dream){
        AtomicInteger num = taskNumMap.get(dream);
        if ( num != null ){
            return num.get();
        }
        return 0;
    }

    /**
     * 指定的dream 运行的个数是否已经超过了阀值
     *
     * @param dream 任务
     *
     * @return true 没有超过容许执行的个数
     */
    public boolean isExecutable(String dream){
        int runningNum = getRunningTaskNum(dream);
        if ( runningNum >= ThresholdUtil.getTaskNumThreshold(dream)){
            logger.info(dream+" running task num:"+runningNum);
            return false;
        } else {
            return true;
        }
    }

    public void add(String dream){
        AtomicInteger nums = taskNumMap.putIfAbsent(dream,new AtomicInteger(1));
        if ( nums != null ){
            nums.addAndGet(1);
        }
    }

    public void del(String dream){
        AtomicInteger nums = taskNumMap.get(dream);
        if ( nums != null ){
            nums.addAndGet(-1);
            logger.debug(" release one resource for " + dream);
            /*if ( nums.get() == 0 ){
                taskNumMap.remove(dream);
                logger.info(" remove the counter for "+dream);
            }*/
        }
    }


}
