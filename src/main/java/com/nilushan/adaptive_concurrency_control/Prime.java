package com.nilushan.adaptive_concurrency_control;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.Callable;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Test to measure performance of Primality check
 */
public class Prime implements Callable<ByteBuf> {
	
	public Prime() {
		
	}

	@Override
	public ByteBuf call() {
		Timer.Context throughputTimerContext = ThreadPoolSizeModifier.THROUGHPUT_TIMER.time();
		ByteBuf buf = null;
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
			//TimeUnit.MILLISECONDS.sleep(5000);
			BigInteger num = new BigInteger(2048, new Random());
			String resultString = String.valueOf(num.isProbablePrime(10)) + "\n";
			buf = Unpooled.copiedBuffer(resultString.getBytes());
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in Prime Run method", e);
		}
		throughputTimerContext.stop();
		return (buf);
	}
}