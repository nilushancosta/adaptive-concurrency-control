package com.nilushan.adaptive_concurrency_control;

import java.math.BigInteger;
import java.util.Random;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Primality check
 */
public class Prime implements Runnable {

	private Object msg;
	private ChannelHandlerContext ctx;

	public Prime(ChannelHandlerContext ctx, Object msg) {
		this.msg = msg;
		this.ctx = ctx;
	}

	@Override
	public void run() {
		ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
		Timer.Context context = ThreadPoolSizeModifier.TIMER.time(); // start dropwizard timer
		BigInteger num = new BigInteger(2048, new Random());
		String resultString = String.valueOf(num.isProbablePrime(10)) + "\n";
		ByteBuf buf = Unpooled.copiedBuffer(resultString.getBytes());
		ctx.write(buf);
		ctx.flush();
		ReferenceCountUtil.release(msg);
		context.stop();
		ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
	}

}