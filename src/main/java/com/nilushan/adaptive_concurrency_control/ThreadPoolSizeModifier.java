package com.nilushan.adaptive_concurrency_control;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.rollingmetrics.histogram.HdrBuilder;

public class ThreadPoolSizeModifier implements Runnable {

	public static int IN_PROGRESS_COUNT;
	public static MetricRegistry METRICS;
	public static HdrBuilder BUILDER;
	public static Timer LATENCY_TIMER;
	public static MetricRegistry METRICS2;
	public static HdrBuilder BUILDER2;
	public static Timer THROUGHPUT_TIMER;
	private static double oldTenSecondRate;
	private static double oldMeanLatency;
	private static double old99PLatency;
	public static int oldInProgressCount;
	private CustomThreadPool threadPool;
	private String optimizationAlgorithm;
	private static boolean HAS_STARTED, INC_ITR, INC_CHECK_ITR, DEC_ITR, DEC_CHECK_ITR; // Used to identify when the algorithm is first run
																						// and the 4 iterations when run() is
																						// executed periodically
	private static boolean INC_IMPROVED; // Variable to check if increasing thread pool size makes improvements
	private static boolean DEC_IMPROVED; // Variable to check if decreasing thread pool size makes improvements
	int incrementLock, decrementLock;
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
		LATENCY_TIMER = BUILDER.buildAndRegisterTimer(METRICS, "ThroughputAndLatency");
		METRICS2 = new MetricRegistry();
		BUILDER2 = new HdrBuilder();
		THROUGHPUT_TIMER = BUILDER2.buildAndRegisterTimer(METRICS2, "ThroughputAndLatency2");
		HAS_STARTED = false;
		INC_ITR = false;
		INC_CHECK_ITR = false;
		DEC_ITR = false;
		DEC_CHECK_ITR = false;
		INC_IMPROVED = true;
		DEC_IMPROVED = true;
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
			INC_ITR = true;
			HAS_STARTED = true;
		}
		int currentThreadPoolSize = threadPool.getThreadPoolSize();
		double currentTenSecondRate = THROUGHPUT_TIMER.getTenSecondRate();
		double rateDifference = currentTenSecondRate - oldTenSecondRate;
		int currentInProgressCount = IN_PROGRESS_COUNT;
		double currentMeanLatency = LATENCY_TIMER.getSnapshot().getMean() / 1000000; //Divided by 1000000 to convert the time to ms
		double current99PLatency = LATENCY_TIMER.getSnapshot().get99thPercentile() / 1000000; //Divided by 1000000 to convert the time to ms
		AdaptiveConcurrencyControl.LOGGER.info(
				currentThreadPoolSize + ", " + currentTenSecondRate + ", " + rateDifference
						+ ", " + currentInProgressCount + ", " + currentMeanLatency + ", " + current99PLatency); // Log metrics

		if (optimizationAlgorithm.equals("T")) { // If Throughput Optimized
			if (INC_ITR == true && INC_IMPROVED == true) {
				System.out.println("ITR1");
				threadPool.incrementPoolSizeBy(10);
			}
			if (INC_CHECK_ITR == true && INC_IMPROVED == true) {
				if (currentTenSecondRate - oldTenSecondRate < oldTenSecondRate * 5 / 100) {
					INC_IMPROVED = false;
					threadPool.decrementPoolSizeBy(10);
					incrementLock = 10; //Prevent increments for the next two sets of iterations
				}
			}
			if (DEC_ITR == true && DEC_IMPROVED == true) {
				threadPool.decrementPoolSizeBy(10);
			}
			if (DEC_CHECK_ITR == true && DEC_IMPROVED == true) {
				if (currentTenSecondRate - oldTenSecondRate < oldTenSecondRate * 5 / 100) {
					DEC_IMPROVED = false;
					threadPool.incrementPoolSizeBy(10);
					decrementLock = 10; //Prevent decrements for the next two sets of iterations
				}
			}
		}

		if (optimizationAlgorithm.equals("M")) { // If Mean latency Optimized
			if (INC_ITR == true && INC_IMPROVED == true) {
				threadPool.incrementPoolSizeBy(10);
			}
			if (INC_CHECK_ITR == true) {
				if (currentMeanLatency - oldMeanLatency < oldMeanLatency * 5 / 100) {
					INC_IMPROVED = false;
				}
			}
			if (DEC_ITR == true && DEC_IMPROVED == true) {
				threadPool.decrementPoolSizeBy(10);
			}
			if (DEC_CHECK_ITR == true && DEC_IMPROVED == true) {
				if (currentMeanLatency - oldMeanLatency < oldMeanLatency * 5 / 100) {
					DEC_IMPROVED = false;
					threadPool.incrementPoolSizeBy(10);
				}
			}
		}

		if (optimizationAlgorithm.equals("99P")) { // If 99th Percentile of Latency Optimized
			if (INC_ITR == true && INC_IMPROVED == true) {
				threadPool.incrementPoolSizeBy(10);
			}
			if (INC_CHECK_ITR == true) {
				if (current99PLatency - old99PLatency < old99PLatency * 10 / 100) {
					INC_IMPROVED = false;
				}
			}
			if (DEC_ITR == true && DEC_IMPROVED == true) {
				threadPool.decrementPoolSizeBy(10);
			}
			if (DEC_CHECK_ITR == true && DEC_IMPROVED == true) {
				if (currentMeanLatency - oldMeanLatency < oldMeanLatency * 5 / 100) {
					DEC_IMPROVED = false;
					threadPool.incrementPoolSizeBy(10);
				}
			}
		}

		oldTenSecondRate = currentTenSecondRate;
		oldMeanLatency = currentMeanLatency;
		old99PLatency = current99PLatency;
		oldInProgressCount = currentInProgressCount;

		if (INC_ITR == true) { //If current iteration is INC_ITR, next would be INC_CHECK_ITR
			INC_ITR = false;
			INC_CHECK_ITR = true;
			DEC_ITR = false;
			DEC_CHECK_ITR = false;
		} else if (INC_CHECK_ITR == true) { //If current iteration is INC_CHECK_ITR, next would be DEC_ITR
			INC_ITR = false;
			INC_CHECK_ITR = false;
			DEC_ITR = true;
			DEC_CHECK_ITR = false;
		} else if (DEC_ITR == true) { //If current iteration is DEC_ITR, next would be DEC_CHECK_ITR
			INC_ITR = false;
			INC_CHECK_ITR = false;
			DEC_ITR = false;
			DEC_CHECK_ITR = true;
		} else if (DEC_CHECK_ITR == true) { //If current iteration is DEC_CHECK_ITR, next would be INC_ITR
			INC_ITR = true;
			INC_CHECK_ITR = false;
			DEC_ITR = false;
			DEC_CHECK_ITR = false;
		}
	
		if (incrementLock > 0) {
			incrementLock-- ;
		} else if (incrementLock == 0) { //Reset INC_IMROVED after incrementLock reaches 0
			INC_IMPROVED = true;
		}
		
		if (decrementLock > 0) {
			decrementLock--;
		} else if (decrementLock == 0) { //Reset DEC_IMPROVED after decrementLock reaches 0
			DEC_IMPROVED = true;
		}
	}

}
