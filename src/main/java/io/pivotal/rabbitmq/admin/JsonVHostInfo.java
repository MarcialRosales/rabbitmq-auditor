package io.pivotal.rabbitmq.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonVHostInfo {
	private String name;
	private int queueCount;
	private int connectionCount;

	private long totalMemory;
	private long totalMessageCount;
	private long totalMessageBytesRam;
	private long totalUnacknowledgedMessageCount;

	JsonVHostInfo(String name) {
		this.name = name;
	}

	public void count(JsonQueue q) {
		queueCount++;
		totalMemory += q.getMemory();
		totalMessageBytesRam += q.getMessageBytesRam();
		totalMessageCount += q.getMessages();
		totalUnacknowledgedMessageCount += q.getMessagesUnacknowledged();
	}

	public void count(JsonConnection c) {
		connectionCount++;
	}

	public int getQueueCount() {
		return queueCount;
	}

	public String getName() {
		return name;
	}

	public int getConnectionCount() {
		return connectionCount;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public long getTotalMessageCount() {
		return totalMessageCount;
	}

	public long getTotalMessageBytesRam() {
		return totalMessageBytesRam;
	}

	public long getTotalUnacknowledgedMessageCount() {
		return totalUnacknowledgedMessageCount;
	}

}