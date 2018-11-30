package com.nilushan.adaptive_concurrency_control;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.Callable;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Primality check
 */
public class Prime implements Callable<ByteBuf> {

	public Prime() {
	}

	@Override
	public ByteBuf call() {
		ByteBuf buf = null;
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
			BigInteger num = new BigInteger(2048, new Random());
			String resultString = String.valueOf(num.isProbablePrime(10)) + "\n";
			buf = Unpooled.copiedBuffer(resultString.getBytes());
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in Prime Run method", e);
		}
		return (buf);
	}
}