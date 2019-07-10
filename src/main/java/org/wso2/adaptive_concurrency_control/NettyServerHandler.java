package org.wso2.adaptive_concurrency_control;

import org.wso2.adaptive_concurrency_control.tomcat.StandardThreadExecutor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

import java.lang.String;

import com.codahale.metrics.Timer;

public class NettyServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private String testName;
	private StandardThreadExecutor executingPool;

	public NettyServerHandler(String name, StandardThreadExecutor pool) {
		this.testName = name;
		this.executingPool = pool;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
		Timer.Context latencyTimerContext = NettyServer.latencyTimer.time();
		if (testName.equalsIgnoreCase("Prime1m")) {
			executingPool.execute(new Prime1m(ctx, msg, latencyTimerContext));
		} else if (testName.equalsIgnoreCase("Prime10m")) {
			executingPool.execute(new Prime10m(ctx, msg, latencyTimerContext));
		} else if (testName.equalsIgnoreCase("DbWrite")) {
			executingPool.execute(new DbWrite(ctx, msg, latencyTimerContext));
		} else if (testName.equalsIgnoreCase("DbRead")) {
			executingPool.execute(new DbRead(ctx, msg, latencyTimerContext));
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
