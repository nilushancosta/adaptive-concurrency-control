package com.nilushan.adaptive_concurrency_control.tomcat;

public interface StandardThreadExecutorMBean {
    boolean resizePool(int corePoolSize, int maximumPoolSize);

    int getActiveCount();

    int getPoolSize();

    int getCorePoolSize();

    int getMaxThreads();

}
