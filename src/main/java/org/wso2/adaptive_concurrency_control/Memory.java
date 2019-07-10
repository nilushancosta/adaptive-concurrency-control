package org.wso2.adaptive_concurrency_control;

public class Memory {
	
	int threadPoolSize;
	int count;
	double value;
	
	public Memory(int size, int c, double val) {
		this.threadPoolSize = size;
		this.count = c;
		this.value = val;
	}
	
	public Memory() {
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

}
