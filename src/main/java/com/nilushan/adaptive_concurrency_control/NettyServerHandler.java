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
	private Future<ByteBuf> result;
	private Timer.Context timerContext;

	public NettyServerHandler(String name, CustomThreadPool pool, Timer.Context tContext) {
		this.testName = name;
		this.executingPool = pool;
		this.timerContext = tContext;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
		
		if (testName.equals("Factorial")) {
			Factorial ft = new Factorial();
			result = executingPool.submitTask(ft);
		} else if (testName.equals("Sqrt")) {
			Sqrt st = new Sqrt();
			result = executingPool.submitTask(st);
		} else if (testName.equals("Prime")) {
			Prime pr = new Prime();			
			result = executingPool.submitTask(pr);			
		} else if (testName.equals("DbWrite")) {
			DbWrite dw = new DbWrite();
			result = executingPool.submitTask(dw);
		} else if (testName.equals("DbRead")) {
			DbRead dr = new DbRead();
			result = executingPool.submitTask(dr);
		}
		
		
		boolean keepAlive = HttpUtil.isKeepAlive(msg);
		FullHttpResponse response = null;
		try {
			response = new DefaultFullHttpResponse(HTTP_1_1, OK, result.get());
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
		
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		timerContext.stop(); // Stop Dropwizard metrics timer
		
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
