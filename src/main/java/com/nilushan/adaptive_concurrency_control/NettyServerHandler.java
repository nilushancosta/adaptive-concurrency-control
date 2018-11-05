package com.nilushan.adaptive_concurrency_control;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.String;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

	private String testName;
	private CustomThreadPool executingPool;

	public NettyServerHandler(String name, CustomThreadPool pool) {
		this.testName = name;
		this.executingPool = pool;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (testName.equals("Factorial")) {
			executingPool.submitTask(new Factorial(ctx, msg));
		} else if (testName.equals("Sqrt")) {
			executingPool.submitTask(new Sqrt(ctx, msg));
		} else if (testName.equals("Prime")) {
			executingPool.submitTask(new Prime(ctx, msg));
		} else if (testName.equals("DbWrite")) {
			executingPool.submitTask(new DbWrite(ctx, msg));
		} else if (testName.equals("DbRead")) {
			executingPool.submitTask(new DbRead(ctx, msg));
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
