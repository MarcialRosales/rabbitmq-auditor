package io.pivotal.rabbitmq.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonChannel {
	String node;
	String vhost;
	int prefetchCount;
	boolean confirm;
	boolean transactional;
	int consumerCount;

	JsonMessageStats messageStats;


	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getVhost() {
		return vhost;
	}

	public void setVhost(String vhost) {
		this.vhost = vhost;
	}

	public int getPrefetchCount() {
		return prefetchCount;
	}

	@JsonProperty("prefetch_count")
	public void setPrefetchCount(int prefetchCount) {
		this.prefetchCount = prefetchCount;
	}

	public boolean isConfirm() {
		return confirm;
	}

	public void setConfirm(boolean confirm) {
		this.confirm = confirm;
	}

	public boolean isTransactional() {
		return transactional;
	}

	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}

	@JsonProperty("consumer_count")
	public int getConsumerCount() {
		return consumerCount;
	}

	public void setConsumerCount(int consumerCount) {
		this.consumerCount = consumerCount;
	}

	
	@JsonProperty("message_stats")
	public JsonMessageStats getMessageStats() {
		return messageStats;
	}

	public void setMessageStats(JsonMessageStats messageStats) {
		this.messageStats = messageStats;
	}
	
	@JsonIgnore
	public long getPublishCount() {
		return messageStats != null ? messageStats.getPublish() : 0;
	}
	
}