package com.nilushan.adaptive_concurrency_control;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Hashtable;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
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
	private static boolean HAS_STARTED, INC_ITR, INC_CHECK_ITR, DEC_ITR, DEC_CHECK_ITR; // Used to identify when the
																						// algorithm is first run
																						// and the 4 iterations when
																						// run() is
																						// executed periodically
	private static boolean INC_IMPROVED; // Variable to check if increasing thread pool size makes improvements
	private static boolean DEC_IMPROVED; // Variable to check if decreasing thread pool size makes improvements
	int incrementLock, decrementLock;
	int resetMemory;
	private long oldCount;

	private ArrayList<Memory> metricMemory;
	/*
	 * Constructor
	 * 
	 * @param The thread pool to be modified
	 */

	public ThreadPoolSizeModifier(CustomThreadPool pool, String optimization) {
		this.threadPool = pool;
		this.optimizationAlgorithm = optimization;
		metricMemory = new ArrayList<Memory>();
		METRICS = new MetricRegistry();
		BUILDER = new HdrBuilder();
		BUILDER.resetReservoirOnSnapshot();
		BUILDER.withPredefinedPercentiles(new double[] { 0.99 }); // Predefine required percentiles
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
		resetMemory = 600;
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
		try {
			if (HAS_STARTED == false && (LATENCY_TIMER.getCount() > 0)) { // When this method is executed for the first
																			// time
				INC_ITR = true;
				HAS_STARTED = true;
			}
			int currentThreadPoolSize = threadPool.getThreadPoolSize();
			double currentTenSecondRate = THROUGHPUT_TIMER.getTenSecondRate();
			double rateDifference = (currentTenSecondRate - oldTenSecondRate) * 100 / oldTenSecondRate;
			int currentInProgressCount = IN_PROGRESS_COUNT;
			Snapshot latencySnapshot = LATENCY_TIMER.getSnapshot();
			double currentMeanLatency = latencySnapshot.getMean() / 1000000; // Divided by 1000000 to convert the time
																				// to ms
			double current99PLatency = latencySnapshot.get99thPercentile() / 1000000; // Divided by 1000000 to convert
																						// the
																						// time to ms
			AdaptiveConcurrencyControl.LOGGER
					.info(currentThreadPoolSize + ", " + currentTenSecondRate + ", " + rateDifference + ", "
							+ currentInProgressCount + ", " + currentMeanLatency + ", " + current99PLatency); // Log
																												// metrics

			//System.out.println(INC_ITR + "," + INC_CHECK_ITR + "," + DEC_ITR + "," + DEC_CHECK_ITR);
			if (optimizationAlgorithm.equals("T")) { // If Throughput Optimized
				if ((DEC_ITR == false || (DEC_ITR == true && DEC_IMPROVED == false))
						&& (((oldTenSecondRate - currentTenSecondRate) / oldTenSecondRate) * 100 > 10) && resetMemory != 300) {
					System.out.println("LOW");
					Memory current;
					for (int i = 0; i < metricMemory.size(); i++) {
						current = metricMemory.get(i);
						System.out.println(">>" + current.getThreadPoolSize() + "," + current.getCount() + ","
								+ current.getValue());
					}
					for (int i = 0; i < metricMemory.size() - 1; i++) {
						if (metricMemory.get(i).getThreadPoolSize() == 0) {
							continue;
						}

						System.out.print(i + "," + (i + 1) + "\n");
						if (currentTenSecondRate <= (metricMemory.get(i).getValue()
								+ metricMemory.get(i + 1).getValue()) / 2) {
							if (currentThreadPoolSize < metricMemory.get(i).getThreadPoolSize()) {
								System.out.println("set to:" + metricMemory.get(i).getThreadPoolSize());
								threadPool.incrementPoolTo(metricMemory.get(i).getThreadPoolSize());
							} else if (currentThreadPoolSize > metricMemory.get(i).getThreadPoolSize()) {
								System.out.println("set to:" + metricMemory.get(i).getThreadPoolSize());
								threadPool.decrementPoolSizeTo(metricMemory.get(i).getThreadPoolSize());
							}
							break;
						}

					}
				}

				if (INC_ITR == true && INC_IMPROVED == true) {
					System.out.println("ITR1");
					threadPool.incrementPoolSizeBy(10);
				}
				if (INC_CHECK_ITR == true && INC_IMPROVED == true) {
					if (((currentTenSecondRate - oldTenSecondRate) / oldTenSecondRate ) * 100 < 10) {
						INC_IMPROVED = false;
						threadPool.decrementPoolSizeBy(10);
						incrementLock = 6; // Prevent increments for the next 8 sets of iterations
					}
				}
//				if (DEC_ITR == true && DEC_IMPROVED == true) {
//					threadPool.decrementPoolSizeBy(10);
//				}
//				if (DEC_CHECK_ITR == true && DEC_IMPROVED == true) {
//					if ((oldTenSecondRate - currentTenSecondRate) * 100 / oldTenSecondRate > 5) {
//						DEC_IMPROVED = false;
//						threadPool.incrementPoolSizeBy(10);
//						decrementLock = 14; // Prevent decrements for the next 8 sets of iterations
//					}
//				}

				Memory current = new Memory();
				boolean isInAList = false;
				for (int i = 0; i < metricMemory.size(); i++) {
					current = metricMemory.get(i);
					if (current.getThreadPoolSize() == currentThreadPoolSize) {
						isInAList = true;
						current.setValue(((current.getValue() * current.getCount()) + currentTenSecondRate)
								/ (current.getCount() + 1));
						current.setCount(current.getCount() + 1);
						break;
					}
				}
				if (isInAList == false) {
					metricMemory.add(new Memory(currentThreadPoolSize, 1, currentTenSecondRate));
				}

//				if (HAS_STARTED == true) {
//					resetMemory -= 10;
//				}
//				if (resetMemory == 0) {
//					metricMemory.clear(); // clear metricMemory every 5 minutes
//					if (currentThreadPoolSize > 1) {
//						threadPool.decrementPoolSizeTo(1);
//						INC_IMPROVED = true;
//					}
//					resetMemory = 600;
//				}

			}

			if (optimizationAlgorithm.equals("M")) { // If Mean latency Optimized
				if (INC_ITR == true && INC_IMPROVED == true) {
					threadPool.incrementPoolSizeBy(10);
				}
				if (INC_CHECK_ITR == true && INC_IMPROVED == true) {
					System.out.println((((oldMeanLatency - currentMeanLatency) / oldMeanLatency) * 100));
					if ((((oldMeanLatency - currentMeanLatency) / oldMeanLatency) * 100) < 5) {
						INC_IMPROVED = false;
						threadPool.decrementPoolSizeBy(10);
						incrementLock = 6; // Lock increments
					}
				}
				if (DEC_ITR == true && DEC_IMPROVED == true) {
					threadPool.decrementPoolSizeBy(10);
				}
				if (DEC_CHECK_ITR == true && DEC_IMPROVED == true) {
					System.out.println((((oldMeanLatency - currentMeanLatency) / oldMeanLatency) * 100));
					if ((((oldMeanLatency - currentMeanLatency) / oldMeanLatency) * 100) < 5) {
						DEC_IMPROVED = false;
						threadPool.incrementPoolSizeBy(10);
						decrementLock = 6; // Lock decrements
					}
				}
			}

			if (optimizationAlgorithm.equals("99P")) { // If 99th Percentile of Latency Optimized
				if (INC_ITR == true && INC_IMPROVED == true) {
					threadPool.incrementPoolSizeBy(10);
				}
				if (INC_CHECK_ITR == true && INC_IMPROVED == true) {
					if ((((old99PLatency - current99PLatency) / old99PLatency) * 100) < 5) {
						INC_IMPROVED = false;
						threadPool.decrementPoolSizeBy(10);
						incrementLock = 6; // Lock increments
					}
				}
				if (DEC_ITR == true && DEC_IMPROVED == true) {
					threadPool.decrementPoolSizeBy(10);
				}
				if (DEC_CHECK_ITR == true && DEC_IMPROVED == true) {
					if ((((old99PLatency - current99PLatency) / old99PLatency) * 100) < 5) {
						DEC_IMPROVED = false;
						threadPool.incrementPoolSizeBy(10);
						decrementLock = 6; // Lock decrements
					}
				}
			}

			oldTenSecondRate = currentTenSecondRate;
			oldMeanLatency = currentMeanLatency;
			old99PLatency = current99PLatency;
			oldInProgressCount = currentInProgressCount;

			if (INC_ITR == true) { // If current iteration is INC_ITR, next would be INC_CHECK_ITR
				INC_ITR = false;
				INC_CHECK_ITR = true;
				DEC_ITR = false;
				DEC_CHECK_ITR = false;
			} else if (INC_CHECK_ITR == true) { // If current iteration is INC_CHECK_ITR, next would be DEC_ITR
				if (optimizationAlgorithm.equals("T")) {
					INC_ITR = true;
					DEC_ITR = false;
				} else {
					INC_ITR = false;
					DEC_ITR = true;
				}
				INC_CHECK_ITR = false;
				DEC_CHECK_ITR = false;
			} else if (DEC_ITR == true) { // If current iteration is DEC_ITR, next would be DEC_CHECK_ITR.
											// Throughput optimization would never reach this if condition
				INC_ITR = false;
				INC_CHECK_ITR = false;
				DEC_ITR = false;
				DEC_CHECK_ITR = true;
			} else if (DEC_CHECK_ITR == true) { // If current iteration is DEC_CHECK_ITR, next would be INC_ITR.
												// Throughput optimization would never reach this if condition
				INC_ITR = true;
				INC_CHECK_ITR = false;
				DEC_ITR = false;
				DEC_CHECK_ITR = false;
			}

			if (incrementLock > 0) {
				incrementLock--;
			} else if (incrementLock == 0) { // Reset INC_IMROVED after incrementLock reaches 0
				INC_IMPROVED = true;
			}

			if (decrementLock > 0) {
				decrementLock--;
			} else if (decrementLock == 0) { // Reset DEC_IMPROVED after decrementLock reaches 0
				DEC_IMPROVED = true;
			}

			long currentCount = LATENCY_TIMER.getCount();
			if ((currentCount - oldCount == 0) && (threadPool.getThreadPoolSize() > 1) ) {	//Set pool size to 1, if no requests were received during 10 seconds
				threadPool.decrementPoolSizeTo(1);
			}
			oldCount = currentCount;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
