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


    // key:the value of DreamTask.dream() returned value
    private ConcurrentHashMap<String,AtomicInteger> taskNums = new ConcurrentHashMap<String, AtomicInteger>();


    public int getRunningTaskNums(String dream){
        AtomicInteger nums = taskNums.get(dream);
        if ( nums != null ){
            return nums.get();
        }
        return 0;
    }

    public void add(String dream){
        AtomicInteger nums = taskNums.putIfAbsent(dream,new AtomicInteger(1));
        if ( nums != null ){
            nums.addAndGet(1);
        }
    }

    public void del(String dream){
        AtomicInteger nums = taskNums.get(dream);
        if ( nums != null ){
            nums.addAndGet(-1);
            logger.debug(" release one resource for "+dream);
            /*if ( nums.get() == 0 ){
                taskNums.remove(dream);
                logger.info(" remove the counter for "+dream);
            }*/
        }
    }


}
