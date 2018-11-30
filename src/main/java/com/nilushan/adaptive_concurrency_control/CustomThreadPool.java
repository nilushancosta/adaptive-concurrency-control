package com.nilushan.adaptive_concurrency_control;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;

public class CustomThreadPool {

	private final int KEEP_ALIVE_TIME = 100;
	private TimeUnit timeUnit = TimeUnit.SECONDS;
	private ThreadPoolExecutor executor;

	/**
	 * The constructor
	 *
	 * @param initialPoolSize size of thread pool
	 */
	public CustomThreadPool(int initialPoolSize) {

		executor = new ThreadPoolExecutor(initialPoolSize, initialPoolSize, KEEP_ALIVE_TIME, timeUnit,
				new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	/**
	 * Submits a task to the thread pool
	 *
	 * @param task to be executed in the thread pool
	 */
	public Future<ByteBuf> submitTask(Prime task) {
		return executor.submit(task);
	}
	
	public Future<ByteBuf> submitTask(Sqrt task) {
		return executor.submit(task);
	}
	
	public Future<ByteBuf> submitTask(Factorial task) {
		return executor.submit(task);
	}
	
	public Future<ByteBuf> submitTask(DbWrite task) {
		return executor.submit(task);
	}
	
	public Future<ByteBuf> submitTask(DbRead task) {
		return executor.submit(task);
	}

	/**
	 * Increments the pool size by n. No upper limit on the pool size
	 */
	public void incrementPoolSizeBy(int n) {
		executor.setMaximumPoolSize(executor.getMaximumPoolSize() + n);
		executor.setCorePoolSize(executor.getCorePoolSize() + n);
	}

	/**
	 * Decrement the pool size by n. Minimum allowed size is 1
	 *
	 * @param n the number to increment by
	 */
	public void decrementPoolSizeBy(int n) {
		if (executor.getCorePoolSize() - n > 0 && executor.getMaximumPoolSize() - n > 0) {
			executor.setCorePoolSize(executor.getCorePoolSize() - n);
			executor.setMaximumPoolSize(executor.getMaximumPoolSize() - n);
		}
	}

	/*
	 * Returns the size of the thread pool
	 * 
	 */
	public int getThreadPoolSize() {
		return executor.getPoolSize();
	}

}
