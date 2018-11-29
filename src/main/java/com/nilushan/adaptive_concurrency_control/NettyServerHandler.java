package com.nilushan.adaptive_concurrency_control;

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

public class NettyServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private String testName;
	private CustomThreadPool executingPool;

	public NettyServerHandler(String name, CustomThreadPool pool) {
		this.testName = name;
		this.executingPool = pool;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
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
		
		boolean keepAlive = HttpUtil.isKeepAlive(msg);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, msg.content().copy());
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
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
