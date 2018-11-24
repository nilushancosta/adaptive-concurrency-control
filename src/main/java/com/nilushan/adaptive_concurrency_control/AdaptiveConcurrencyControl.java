package com.nilushan.adaptive_concurrency_control;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptiveConcurrencyControl {

	private static final int THREAD_POOL_MODIFICATION_INITIAL_DELAY = 10;
	private static final int THREAD_POOL_MODIFICATION_PERIOD = 10;
	private static final int PORT = 15000;
	public static Logger LOGGER = LoggerFactory.getLogger(AdaptiveConcurrencyControl.class);

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			LOGGER.error("Arguments not found! Please specify the 3 arguments <TestName> <initialWorkerPoolCount> <Optimization>");
			System.exit(-1);
		}
		String testName = args[0];
		int initWorkerThreads = Integer.parseInt(args[1]);
		String optimization = args[2]; // T=Throughput Optimized, M=Mean latency Optimized, 99P=99th Percentile of latency optimized
		ScheduledExecutorService threadPoolSizeModifier = Executors.newScheduledThreadPool(1); // Create the thread pool
																								// to run the periodic
																								// thread count
																								// adjustment
		CustomThreadPool thirdThreadPool = new CustomThreadPool(initWorkerThreads); // Create the thread pool to handle
																					// workload processing
		threadPoolSizeModifier.scheduleAtFixedRate(new ThreadPoolSizeModifier(thirdThreadPool, optimization),
				THREAD_POOL_MODIFICATION_INITIAL_DELAY, THREAD_POOL_MODIFICATION_PERIOD, TimeUnit.SECONDS);
		new NettyServer(PORT, testName, thirdThreadPool).run();

	}
}
