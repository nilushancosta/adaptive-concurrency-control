package com.nilushan.adaptive_concurrency_control;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.Callable;

import com.codahale.metrics.Timer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * Test to measure performance of Database Write
 */
public class DbWrite implements Callable<ByteBuf> {

	public DbWrite() {
	}

	@Override
	public ByteBuf call() {
		ByteBuf buf = null;
		try {
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT++;
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
				buf = Unpooled.copiedBuffer(current.toString().getBytes());
			} catch (Exception e) {
				AdaptiveConcurrencyControl.LOGGER.error("Exception", e);
			} finally {
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
			ThreadPoolSizeModifier.IN_PROGRESS_COUNT--;
		} catch (Exception e) {
			AdaptiveConcurrencyControl.LOGGER.error("Exception in DbWrite Run method", e);
		}
		return (buf);
	}

}