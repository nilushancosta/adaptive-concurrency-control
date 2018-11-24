package com.nilushan.adaptive_concurrency_control;

import java.math.BigInteger;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Factorial calculation
 */
public class Factorial implements Runnable {

	private Object msg;
	private ChannelHandlerContext ctx;

	public Factorial(ChannelHandlerContext ctx, Object msg) {
		this.msg = msg;
		this.ctx = ctx;
	}

	@Override
	public void run() {
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
			Timer.Context context = ThreadPoolSizeModifier.TIMER.time(); // start dropwizard timer
			BigInteger result = BigInteger.ONE;
			String resultString;
			int randomNumber = ThreadLocalRandom.current().nextInt(2000, 2999 + 1); // Generate random number between
																					// 2000
																					// and 2999
			for (int i = randomNumber; i > 0; i--) {
				result = result.multiply(BigInteger.valueOf(i));
			}
			resultString = result.toString().substring(0, 3) + "\n"; // get a substring to prevent the effect of network
																		// I/O
																		// affecting factorial calculation performance
			ByteBuf buf = Unpooled.copiedBuffer(resultString.getBytes());
			ctx.write(buf);
			ctx.flush();
			ReferenceCountUtil.release(msg);
			context.stop();
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in Factorial Run method", e);
		}
	}

}