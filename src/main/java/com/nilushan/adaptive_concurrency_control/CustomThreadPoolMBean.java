package com.nilushan.adaptive_concurrency_control;

public interface CustomThreadPoolMBean {
    public void changePoolSize(int n);

    public int getPoolSize();
}
