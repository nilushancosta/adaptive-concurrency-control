package com.nilushan.adaptive_concurrency_control;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.Callable;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpUtil;

/**
 * Test to measure performance of Primality check
 */
public class Prime10m implements Runnable {

	private FullHttpRequest msg;
	private ChannelHandlerContext ctx;
	private Timer.Context timerContext;

	public Prime10m(ChannelHandlerContext ctx, FullHttpRequest msg, Timer.Context timerCtx) {
		this.msg = msg;
		this.ctx = ctx;
		this.timerContext = timerCtx;
	}

	@Override
	public void run() {
		Timer.Context throughputTimerContext = ThreadPoolSizeModifier.THROUGHPUT_TIMER.time();
		ByteBuf buf = null;
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
			Random rand = new Random();
			int number = rand.nextInt((10000021) - 10000000 ) + 10000000;  //Generate random integer between 100000 and 100020
			String resultString = "true";
			for (int i=2; i<number; i++) {
				if (number%i == 0) {
					resultString="false";
					break;
				}
			}
			buf = Unpooled.copiedBuffer(resultString.getBytes());
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in Prime10m Run method", e);
		}
		
		boolean keepAlive = HttpUtil.isKeepAlive(msg);
		FullHttpResponse response = null;
		try {
			response = new DefaultFullHttpResponse(HTTP_1_1, OK, buf);
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in Netty Handler", e);
		}
		String contentType = msg.headers().get(HttpHeaderNames.CONTENT_TYPE);
		if (contentType != null) {
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
		}
		response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
		if (!keepAlive) {
			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		} else {
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			ctx.write(response);
		}
		ctx.flush();
		throughputTimerContext.stop();
		timerContext.stop(); // Stop Dropwizard metrics timer
	}
}