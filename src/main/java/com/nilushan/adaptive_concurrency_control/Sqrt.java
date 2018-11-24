package com.nilushan.adaptive_concurrency_control;

import com.codahale.metrics.Timer;

import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Square root calculation
 */
public class Sqrt implements Runnable {

	private Object msg;
	private ChannelHandlerContext ctx;

	public Sqrt(ChannelHandlerContext ctx, Object msg) {
		this.msg = msg;
		this.ctx = ctx;
	}

	@Override
	public void run() {
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
			Timer.Context context = ThreadPoolSizeModifier.TIMER.time(); // start dropwizard timer
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
			ctx.write(Unpooled.copiedBuffer(resultString.getBytes()));
			ctx.flush();
			ReferenceCountUtil.release(msg);
			context.stop();
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in Sqrt run method", e);
		}
	}

}