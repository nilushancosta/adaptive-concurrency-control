package com.nilushan.adaptive_concurrency_control;

public interface CustomThreadPoolMBean {
    public void setPoolSize(int n);

    public int getPoolSize();
}
