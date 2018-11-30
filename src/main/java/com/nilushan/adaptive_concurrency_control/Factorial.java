package com.nilushan.adaptive_concurrency_control;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Factorial calculation
 */
public class Factorial implements Callable<ByteBuf> {

	public Factorial() {
	}

	@Override
	public ByteBuf call() {
		ByteBuf buf = null;
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
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
			buf = Unpooled.copiedBuffer(resultString.getBytes());
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in Factorial Run method", e);
		}
		return (buf);
	}

}