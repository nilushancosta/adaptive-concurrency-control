package com.nilushan.adaptive_concurrency_control;

import java.util.concurrent.Callable;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Square root calculation
 */
public class Sqrt implements Callable<ByteBuf> {

	public Sqrt() {

	}

	@Override
	public ByteBuf call() {
		Timer.Context throughputTimerContext = ThreadPoolSizeModifier.THROUGHPUT_TIMER.time();
		ByteBuf buf = null;
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
			String resultString;
			double randomNumber = ThreadLocalRandom.current().nextDouble(6000000000.0000000, 6900000000.0000000 + 1); // Generate
																														// random
																														// number
																														// between
																														// 6000000000.0000000
																														// to
																														// 6900000000.0000000
			double sqrt = Math.sqrt(randomNumber);
			resultString = String.valueOf(sqrt) + "\n";
			buf = Unpooled.copiedBuffer(resultString.getBytes());
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in Sqrt run method", e);
		}
		throughputTimerContext.stop();
		return (buf);
	}

}