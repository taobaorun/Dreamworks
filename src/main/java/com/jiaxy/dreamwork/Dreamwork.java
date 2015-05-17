package com.jiaxy.dreamwork;

import com.jiaxy.dreamwork.assist.IncubatorStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jiaxy.dreamwork.assist.ThresholdUtil.DEFAULT_TASK_TIMEOUT;
import static com.jiaxy.dreamwork.assist.ThresholdUtil.isTimeout;

/**
 * Title:<br>
 * Desc:<br>
 * <p>
 *     thread pool based on {@link java.util.concurrent.ThreadPoolExecutor}
 * </p>
 *
 * @author tbrun
 *
 * @since 2015/05/16 17:27
 */
public class Dreamwork {

    private Logger logger = LoggerFactory.getLogger(Dreamwork.class);

    private IncubatorStat stat = new IncubatorStat();

    private ConcurrentHashMap<String,ConcurrentLinkedQueue<DreamTask>> waitTaskQueueMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<DreamTask>>();

    private ExecutorService awaitTaskConsumer = Executors.newSingleThreadExecutor(new DreamworkThreadFactory("AwaitTaskConsumer",true));

    private ScheduledExecutorService checkService = Executors.newSingleThreadScheduledExecutor(new DreamworkThreadFactory("AwaitCheck",true));

    private Incubator dwpool = null;

    public Dreamwork(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, final BlockingQueue<Runnable> workQueue,boolean daemon) {
        dwpool = new Incubator(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,daemon);
//        dwpool.allowCoreThreadTimeOut(true);
        addResourceAvailableListener();
        checkResourceAvailable();
    }



    public Dreamwork(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        dwpool = new Incubator(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        addResourceAvailableListener();
        checkResourceAvailable();
    }

    public Dreamwork(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        dwpool = new Incubator(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler,true);
        addResourceAvailableListener();
        checkResourceAvailable();
    }

    public Dreamwork(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        dwpool = new Incubator(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        addResourceAvailableListener();
        checkResourceAvailable();
    }


    /**
     * delegate this task to dwpool (ThreadPoolExecutor)
     *
     * @param dreamTask the need execute task
     */
    public void execute(DreamTask dreamTask){
        try {
            dwpool.execute(dreamTask);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public void shutdown() {
        dwpool.shutdown();
    }


    public List<Runnable> shutdownNow() {
        return dwpool.shutdownNow();
    }


    public void dumpWaitTask(){

    }

    public String showWaitTaskNum(){
        StringBuilder sb = new StringBuilder("[");
        for (Map.Entry<String,ConcurrentLinkedQueue<DreamTask>> entry : waitTaskQueueMap.entrySet()){
            sb.append("{\"time\":")
                    .append(new Date().getTime())
                    .append(",")
                    .append("\"dream\":")
                    .append("\"")
                    .append(entry.getKey())
                    .append("\"")
                    .append(",")
                    .append("\"dreamTask\":")
                    .append(entry.getValue().size())
                    .append("}")
                    .append(",");

        }
        sb.append("]");
        System.out.println(sb.toString());
        return sb.toString();
    }

    public String showIncubatorStatus(){
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"active\":")
                .append(dwpool.getActiveCount())
                .append(",")
                .append("\"poolsize\":")
                .append(dwpool.getPoolSize())
                .append(",")
                .append("\"task\":")
                .append(dwpool.getTaskCount())
                .append(",")
                .append("\"largestpoolsize\":")
                .append(dwpool.getLargestPoolSize());
        sb.append("}");
        System.out.println(sb.toString());
        return sb.toString();
    }

    /**
     * 线程池添加资源(Thread)可用监听
     */
    private void addResourceAvailableListener() {
        dwpool.addThreadAvailableListener(new ThreadAvailableListener() {
            @Override
            public void retryDreamTask(final String dream) {
                awaitTaskConsumer.execute(new Runnable() {
                    @Override
                    public void run() {
                        //retry
                        ConcurrentLinkedQueue<DreamTask> waitQueue = waitTaskQueueMap.get(dream);
                        if ( waitQueue != null ){
                            DreamTask dreamTask = waitQueue.poll();
                            if ( dreamTask != null ){
                                if ( !isTimeout(dreamTask) ){
                                    logger.debug(dreamTask+" retry execute :"+dreamTask.dream());
                                    Dreamwork.this.execute(dreamTask);
                                } else {
                                    logger.info(dreamTask + " dropped for timeout :" + dreamTask.dream());
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    private void checkResourceAvailable(){
        checkService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String,ConcurrentLinkedQueue<DreamTask>> entry : waitTaskQueueMap.entrySet() ){
                    if ( stat.isExecutable(entry.getKey()) ){
                        DreamTask dreamTask = entry.getValue().poll();
                        if ( dreamTask != null ){
                            if ( !isTimeout(dreamTask) ){
                                logger.info("retry execute :" + dreamTask.dream() + " by check service");
                                //just only one
                                Dreamwork.this.execute(dreamTask);
                            } else {
                                logger.info("the dream task was dropped for timeout :" + dreamTask.dream() + " by check service");
                            }
                        }
                    }
                }
            }
        },DEFAULT_TASK_TIMEOUT - 500,DEFAULT_TASK_TIMEOUT - 500,TimeUnit.MILLISECONDS);


    }

    /**
     * thread pool based on {@link java.util.concurrent.ThreadPoolExecutor}
     */
    class Incubator extends ThreadPoolExecutor{
        private DreamworkThreadFactory defaultThreadFactory = new DreamworkThreadFactory("Incubator",true);

        private ThreadAvailableListener listener = null;

        public Incubator(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,boolean daemon) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
            defaultThreadFactory.setDaemon(daemon);
            setThreadFactory(defaultThreadFactory);
        }

        public Incubator(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        public Incubator(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler,boolean daemon) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
            defaultThreadFactory.setDaemon(daemon);
            setThreadFactory(defaultThreadFactory);
        }

        public Incubator(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        public void addThreadAvailableListener(ThreadAvailableListener listener){
            this.listener = listener;
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            DreamTask dreamTask = (DreamTask) r;
            if ( !stat.isExecutable(dreamTask.dream()) ){
                ConcurrentLinkedQueue waitTaskQueue = waitTaskQueueMap.get(dreamTask.dream());
                if ( waitTaskQueue == null ){
                    waitTaskQueueMap.putIfAbsent(dreamTask.dream(), new ConcurrentLinkedQueue<DreamTask>());
                    waitTaskQueueMap.get(dreamTask.dream()).offer(dreamTask);
                } else {
                    waitTaskQueue.offer(dreamTask);
                }
                logger.info(dreamTask +" is not allowed to execute");
                throw new RuntimeException(dreamTask+" exceed the threshold of task num");
            }
            stat.add(dreamTask.dream());
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            DreamTask dreamTask = (DreamTask) r;
            super.afterExecute(r, t);
            stat.del(dreamTask.dream());
            listener.retryDreamTask(dreamTask.dream());
        }
    }

    interface ThreadAvailableListener {

        void retryDreamTask(String dream);

    }

    class DreamworkThreadFactory implements ThreadFactory{

        private final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private boolean daemon;

        DreamworkThreadFactory(String name,boolean daemon) {
            SecurityManager s = System.getSecurityManager();
            this.daemon = daemon;
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "DW-" +name+"-"+
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            t.setDaemon(daemon);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    //TODO
                    //System.out.println(e.getMessage());
                }
            });
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }

        public void setDaemon(boolean daemon) {
            this.daemon = daemon;
        }
    }
}
