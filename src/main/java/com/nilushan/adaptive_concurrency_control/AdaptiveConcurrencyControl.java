package com.nilushan.adaptive_concurrency_control;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowArrayReservoir;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jmx.JmxReporter;
import com.nilushan.adaptive_concurrency_control.tomcat.StandardThreadExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class AdaptiveConcurrencyControl {

	private static final int PORT = 15000;
	public static Logger LOGGER = LoggerFactory.getLogger(AdaptiveConcurrencyControl.class);

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			LOGGER.error("Arguments not found! Please specify the 3 arguments <TestName> <fixed_pool_size or 'tomcat'> <Metric Window Size>");
			System.exit(-1);
		}

		// Start tomcat executor with default configurations
		StandardThreadExecutor threadPool = new StandardThreadExecutor();

		String testName = args[0];

		// check whether the test is for a fixed pool size
		if (!args[1].equalsIgnoreCase("tomcat")){
			// if for a fixed pool size, set the thread pool size of tomcat executor to that fixed value
			// note - we can make the tomcat executor a fixed executor by making corePoolSize = macThreads
			int fixedPoolSize = Integer.parseInt(args[1]);
			threadPool.resizePool(fixedPoolSize, fixedPoolSize);
		}

		int windowSize = Integer.parseInt(args[2]);

		// Dropwizard metrics
		MetricRegistry metricRegistry = new MetricRegistry();
		Timer latencyTimer = metricRegistry.timer("response_times", () -> new Timer(
				new SlidingTimeWindowArrayReservoir(windowSize, TimeUnit.SECONDS)));

		JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
		jmxReporter.start();

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName threadPoolName = new ObjectName("threadPool:type=StandardThreadExecutor");
		server.registerMBean(threadPool, threadPoolName);

		new NettyServer(PORT, testName, threadPool, latencyTimer).start();

	}
}
