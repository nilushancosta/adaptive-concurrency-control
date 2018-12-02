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
	private String optimizationAlgorithm;
	private static boolean HAS_STARTED, ODD_ITR, EVEN_ITR; // Used to identify the odd and even iterations when run() is
															// executed periodically
	private static boolean IMPROVED; //Variable to check if changing thread pool size makes improvements

	/*
	 * Constructor
	 * 
	 * @param The thread pool to be modified
	 */
	public ThreadPoolSizeModifier(CustomThreadPool pool, String optimization) {
		this.threadPool = pool;
		this.optimizationAlgorithm = optimization;
		METRICS = new MetricRegistry();
		BUILDER = new HdrBuilder();
		TIMER = BUILDER.buildAndRegisterTimer(METRICS, "ThroughputAndLatency");
		HAS_STARTED = false;
		ODD_ITR = false;
		EVEN_ITR = false;
		IMPROVED = true;
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
		if (HAS_STARTED == false) { // When this method is executed for the first time
			ODD_ITR = true;
			HAS_STARTED = true;
		}
		//System.out.println(HAS_STARTED + "," + ODD_ITR + "," + EVEN_ITR);
		int currentThreadPoolSize = threadPool.getThreadPoolSize();
		double currentTenSecondRate = TIMER.getTenSecondRate();
		double rateDifference = currentTenSecondRate - oldTenSecondRate;
		int currentInProgressCount = IN_PROGRESS_COUNT;
		double currentMeanLatency = TIMER.getSnapshot().getMean() / 1000000; // Divided by 1000000 to convert the time to ms
		double current99PLatency = TIMER.getSnapshot().get99thPercentile() / 1000000 ; //Divided by 1000000 to convert the time to ms
		AdaptiveConcurrencyControl.LOGGER
				.info(currentThreadPoolSize + ", " + currentTenSecondRate + ", " + rateDifference + ", "
						+ currentInProgressCount + ", " + currentMeanLatency + ", " + current99PLatency); // Log metrics

		if (optimizationAlgorithm.equals("T")) { // If Throughput Optimized
			if (ODD_ITR == true && IMPROVED == true) {
				threadPool.incrementPoolSizeBy(10);
			}
			if (EVEN_ITR == true) {
				if ( currentTenSecondRate - oldTenSecondRate < oldTenSecondRate * 10 / 100) {
					IMPROVED = false;
				}
				else {
					IMPROVED = true;
				}
			}
		}

		if (optimizationAlgorithm.equals("M")) { // If Mean latency Optimized
			if (ODD_ITR == true && IMPROVED == true) {
				threadPool.incrementPoolSizeBy(10);
			}
			if (EVEN_ITR == true) {
				//System.out.println(currentMeanLatency + "," + oldMeanLatency);
				if ( currentMeanLatency - oldMeanLatency < oldMeanLatency * 10 / 100) {
					IMPROVED = false;
				}
				else {
					IMPROVED = true;
				}
			}
		}

		if (optimizationAlgorithm.equals("99P")) { // If 99th Percentile of Latency Optimized
			if (ODD_ITR == true && IMPROVED == true) {
				threadPool.incrementPoolSizeBy(10);
			}
			if (EVEN_ITR == true) {
				if (current99PLatency - old99PLatency < old99PLatency * 10 / 100) {
					IMPROVED = false;
				}
				else {
					IMPROVED = true;
				}
			}
		}

		oldTenSecondRate = currentTenSecondRate;
		oldMeanLatency = currentMeanLatency;
		old99PLatency = current99PLatency;
		oldInProgressCount = currentInProgressCount;

		if (ODD_ITR == true) {
			ODD_ITR = false;
			EVEN_ITR = true;
		} else if (EVEN_ITR == true) {
			EVEN_ITR = false;
			ODD_ITR = true;
		}
	}

}
