package com.nilushan.adaptive_concurrency_control;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Random;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Database Read
 */
public class DbRead implements Runnable {

	private Object msg;
	private ChannelHandlerContext ctx;

	public DbRead(ChannelHandlerContext ctx, Object msg) {
		this.msg = msg;
		this.ctx = ctx;
	}

	@Override
	public void run() {
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
			Timer.Context context = ThreadPoolSizeModifier.TIMER.time(); // start dropwizard timer
			Connection connection = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;
			Timestamp readTimestamp = null;
			try {
				Random randId = new Random();
				int toRead = randId.nextInt(50000) + 1;
				connection = DriverManager.getConnection(
						"jdbc:mysql://127.0.0.1:3306/echoserver?useSSL=false&autoReconnect=true&failOverReadOnly=false&maxReconnects=10",
						"root", "root");
				String sql = "SELECT timestamp FROM Timestamp WHERE id=?";
				stmt = connection.prepareStatement(sql);
				stmt.setInt(1, toRead);
				rs = stmt.executeQuery();
				while (rs.next()) {
					readTimestamp = rs.getTimestamp("timestamp");
				}
			} catch (Exception e) {
				AdaptiveConcurrencyControl.LOGGER.error("Exception", e);
			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (Exception e) {
						AdaptiveConcurrencyControl.LOGGER.error("Exception", e);
					}
				}
				if (stmt != null) {
					try {
						stmt.close();
					} catch (Exception e) {
						AdaptiveConcurrencyControl.LOGGER.error("Exception", e);
					}
				}
				if (connection != null) {
					try {
						connection.close();
					} catch (Exception e) {
						AdaptiveConcurrencyControl.LOGGER.error("Exception", e);
					}
				}
			}
			String readTimestampStr = readTimestamp.toString() + "\n";
			ByteBuf buf = Unpooled.copiedBuffer(readTimestampStr.getBytes());
			ReferenceCountUtil.release(msg);
			ctx.write(buf);
			ctx.flush();
			context.stop();
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in DbRead Run method", e);
		}

	}

}