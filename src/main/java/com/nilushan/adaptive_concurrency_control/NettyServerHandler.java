package com.nilushan.adaptive_concurrency_control;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpUtil;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.lang.String;
import java.util.concurrent.Future;

import com.codahale.metrics.Timer;

public class NettyServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private String testName;
	private CustomThreadPool executingPool;
	private Timer.Context timerContext;

	public NettyServerHandler(String name, CustomThreadPool pool, Timer.Context tContext) {
		this.testName = name;
		this.executingPool = pool;
		this.timerContext = tContext;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {

		if (testName.equals("Factorial")) {
			executingPool.submitTask(new Factorial(ctx, msg, timerContext));
		} else if (testName.equals("Sqrt")) {
			executingPool.submitTask(new Sqrt(ctx, msg, timerContext));
		} else if (testName.equals("Prime")) {
			executingPool.submitTask(new Prime(ctx, msg, timerContext));
		} else if (testName.equals("DbWrite")) {
			executingPool.submitTask(new DbWrite(ctx, msg, timerContext));
		} else if (testName.equals("DbRead")) {
			executingPool.submitTask(new DbRead(ctx, msg, timerContext));
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
