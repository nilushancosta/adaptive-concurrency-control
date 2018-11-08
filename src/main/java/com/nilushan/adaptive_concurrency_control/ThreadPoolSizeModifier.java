package com.nilushan.adaptive_concurrency_control;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rollingmetrics.histogram.HdrBuilder;

public class ThreadPoolSizeModifier implements Runnable {

	public static int IN_PROGRESS_COUNT;
	public static MetricRegistry METRICS;
	public static HdrBuilder BUILDER;
	public static Timer TIMER;
	private static double oldTenSecondRate;
	private static double oldMeanLatency;
	private static double old99PLatency;
	public static int oldInProgressCount;
	private CustomThreadPool threadPool;

	/*
	 * Constructor
	 * 
	 * @param The thread pool to be modified
	 */
	public ThreadPoolSizeModifier(CustomThreadPool pool) {
		this.threadPool = pool;
		METRICS = new MetricRegistry();
		BUILDER = new HdrBuilder();
		TIMER = BUILDER.buildAndRegisterTimer(METRICS, "ThroughputAndLatency");
		AdaptiveConcurrencyControl.LOGGER.info(
				"Thread pool size, Current 10 Second Throughput, Throughput Difference, In pogress count, Average Latency, 99th percentile Latency"); // First
																																						// line
																																						// of
																																						// the
																																						// log
																																						// file
	}

	@Override
	public void run() {
		int currentThreadPoolSize = threadPool.getThreadPoolSize();
		double currentTenSecondRate = TIMER.getTenSecondRate();
		double rateDifference = currentTenSecondRate - oldTenSecondRate;
		int currentInProgressCount = IN_PROGRESS_COUNT;
		double currentMeanLatency = TIMER.getSnapshot().getMean();
		double current99PLatency = TIMER.getSnapshot().get99thPercentile();
		AdaptiveConcurrencyControl.LOGGER
				.info(currentThreadPoolSize + ", " + currentTenSecondRate + ", " + rateDifference + ", "
						+ currentInProgressCount + ", " + currentMeanLatency + ", " + current99PLatency); // Log metrics

///// Throughput optimized //////////////
		if (currentTenSecondRate - oldTenSecondRate >= oldTenSecondRate * 5 / 100) {
			threadPool.incrementPoolSizeBy(10);
		}

		if (oldTenSecondRate - currentTenSecondRate >= oldTenSecondRate * 5 / 100) {
			threadPool.decrementPoolSizeBy(10);
		}
//////End of Throughput optimized //////////////

/// Average Latency optimized //////////////
		if (oldMeanLatency - currentMeanLatency >= oldMeanLatency * 5 / 100) {
			threadPool.incrementPoolSizeBy(10);
		}

		if (currentMeanLatency - oldMeanLatency >= oldMeanLatency * 5 / 100) {
			threadPool.decrementPoolSizeBy(10);
		}
/// End of Average latency optimized //////////////

///// 99th percentile of latency optimized //////////////
		if ( old99PLatency - current99PLatency >= old99PLatency * 5 / 100) {
			threadPool.incrementPoolSizeBy(10);

		}

		if ( current99PLatency - old99PLatency >= old99PLatency * 5 / 100) {
			threadPool.decrementPoolSizeBy(10);
		}
///// End of 99th percentile of latency optimized //////////////
		
		oldTenSecondRate = currentTenSecondRate;
		oldMeanLatency = currentMeanLatency;
		old99PLatency = current99PLatency;
		oldInProgressCount = currentInProgressCount;
	}
}
