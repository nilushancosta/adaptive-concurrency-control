package com.nilushan.adaptive_concurrency_control;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

import com.codahale.metrics.Timer;

import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Database Write
 */
public class DbWrite implements Runnable {

	private Object msg;
	private ChannelHandlerContext ctx;

	public DbWrite(ChannelHandlerContext ctx, Object msg) {
		this.msg = msg;
		this.ctx = ctx;
	}

	@Override
	public void run() {
		ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
		Timer.Context context = ThreadPoolSizeModifier.TIMER.time(); // start dropwizard timer
		Connection connection = null;
		PreparedStatement stmt = null;
		try {
			connection = DriverManager.getConnection(
					"jdbc:mysql://127.0.0.1:3306/echoserver?useSSL=false&autoReconnect=true&failOverReadOnly=false&maxReconnects=10",
					"root", "root");
			Timestamp current = Timestamp.from(Instant.now()); // get current timestamp
			String sql = "INSERT INTO Timestamp (timestamp) VALUES (?)";
			stmt = connection.prepareStatement(sql);
			stmt.setTimestamp(1, current);
			stmt.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Exception e) {
					e.printStackTrace(System.out);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e) {
					e.printStackTrace(System.out);
				}
			}
		}
		ctx.write(msg);
		ctx.flush();
		context.stop();
		ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
	}

}