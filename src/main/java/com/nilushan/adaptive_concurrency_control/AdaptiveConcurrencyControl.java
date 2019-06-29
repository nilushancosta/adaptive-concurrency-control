package com.nilushan.adaptive_concurrency_control;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowArrayReservoir;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jmx.JmxReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class AdaptiveConcurrencyControl {

	private static final int PORT = 15000;
	public static Logger LOGGER = LoggerFactory.getLogger(AdaptiveConcurrencyControl.class);

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			LOGGER.error("Arguments not found! Please specify the 3 arguments <TestName> <initialWorkerPoolCount> <Metric Window Size>");
			System.exit(-1);
		}

		String testName = args[0];
		int initWorkerThreads = Integer.parseInt(args[1]);
		int windowSize = Integer.parseInt(args[2]);

		CustomThreadPool thirdThreadPool = new CustomThreadPool(initWorkerThreads);

		// Dropwizard metrics
		MetricRegistry metricRegistry = new MetricRegistry();
		Timer latencyTimer = metricRegistry.timer("response_times", () -> new Timer(
				new SlidingTimeWindowArrayReservoir(windowSize, TimeUnit.SECONDS)));

		JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
		jmxReporter.start();

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName rbeName = new ObjectName("thirdThreadPool:type=CustomThreadPool");
		server.registerMBean(thirdThreadPool, rbeName);

		new NettyServer(PORT, testName, thirdThreadPool, latencyTimer).start();

	}
}
