package io.pivotal.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rabbitmq.http.client.domain.MessageStats;

@JsonIgnoreProperties(ignoreUnknown = true)
class JsonVhost {
	String name;
	MessageStats messageStats;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@JsonProperty("message_stats")
	public MessageStats getMessageStats() {
		return messageStats;
	}
	public void setMessageStats(MessageStats messageStats) {
		this.messageStats = messageStats;
	}
	
	public long getPublishCount() {
		return messageStats != null ? messageStats.getBasicPublish() : 0;
	}
	
	
}