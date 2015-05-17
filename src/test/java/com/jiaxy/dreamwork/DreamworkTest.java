package com.jiaxy.dreamwork;


import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DreamworkTest {

    @org.junit.Before
    public void setUp() throws Exception {

    }

    @org.junit.Test
    public void testExecute() throws Exception {


    }

    public static void main(String[] args){
        AtomicInteger doneWork = new AtomicInteger(0);
        Dreamwork dreamwork = new Dreamwork(100,800,1000, TimeUnit.MILLISECONDS,new LinkedBlockingDeque<Runnable>(100),true);
        int size = 80;
        int i = 0;
        //monitor(dreamwork);
        while ( size-- > 0 ){

            try {
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
                dreamwork.execute(new MyDream("a.test"+i,i,doneWork));
            } catch (Throwable e) {
                e.printStackTrace();
            }
            i++;
        }
        boolean done = false;
        while ( true ){
            if ( doneWork.get() == 800){
                done = true;
                break;
            } else {
                try {
                    Thread.sleep(1000);
                    System.out.println(doneWork.get() + " done");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if ( !done ){
            throw new RuntimeException("works were not done:done work num is "+doneWork.get());
        } else {
            dreamwork.shutdown();
        }


    }

    public static void monitor(final Dreamwork dreamwork){

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(dreamwork.new DreamworkThreadFactory("MONITOR",true));
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                dreamwork.showWaitTaskNum();
                dreamwork.showIncubatorStatus();

            }
        },1000,1000,TimeUnit.MILLISECONDS);

    }

    static class MyDream implements DreamTask{

        int index = 0;

        private AtomicInteger doneWork;

        private String dream;

        private long createTime;

        public MyDream(String dream,int index,AtomicInteger doneWork) {
            this.createTime = System.currentTimeMillis();
            this.dream = dream;
            this.doneWork = doneWork;
            this.index = index;
        }

        @Override
        public String dream() {
            return dream;
        }

        @Override
        public long firstExecutedTime() {
            return createTime;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(new Random().nextInt(5000) + 1000);
                doneWork.addAndGet(1);
            } catch (InterruptedException e) {
            }
            //System.out.println("-------------"+dream()+"--" + index);
        }


        @Override
        public String toString() {
            return "MyDream{" +
                    "index=" + index +
                    ", doneWork=" + doneWork +
                    ", dream='" + dream + '\'' +
                    '}';
        }
    }

}