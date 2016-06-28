package io.pivotal.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <pre>
 *  {
    "memory": 14032,
    "messages": 0,
    "consumers": 0,
    "name": "unrouted",
    "node": "rabbit_2@Flamingo"
  }
 * </pre>
 * 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class JsonQueue {
	long memory;
	long messages; // it should be ready + unacknoweldged
	long messagesReady;
	long messagesUnacknowledged;

	long messageBytesRam; // ready + unacknowledged
	long messageBytesPersistent;

	long consumers;
	String policy;

	@JsonProperty("message_bytes_persistent")
	public long getMessageBytesPersistent() {
		return messageBytesPersistent;
	}

	public void setMessageBytesPersistent(long messageBytesPersistent) {
		this.messageBytesPersistent = messageBytesPersistent;
	}

	String name;
	String node;
	String vhost;

	@JsonProperty("message_bytes_ram")
	public long getMessageBytesRam() {
		return messageBytesRam;
	}

	public void setMessageBytesRam(long messagesBytesRam) {
		this.messageBytesRam = messagesBytesRam;
	}

	@JsonProperty("message_ready")
	public long getMessagesReady() {
		return messagesReady;
	}

	public void setMessagesReady(long messagesReady) {
		this.messagesReady = messagesReady;
	}

	@JsonProperty("messages_unacknowledged")
	public long getMessagesUnacknowledged() {
		return messagesUnacknowledged;
	}

	public void setMessagesUnacknowledged(long messagesUnacknowledged) {
		this.messagesUnacknowledged = messagesUnacknowledged;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public String getVhost() {
		return vhost;
	}

	public void setvVost(String vHost) {
		this.vhost = vHost;
	}

	public long getMemory() {
		return memory;
	}

	public void setMemory(long memory) {
		this.memory = memory;
	}

	public long getMessages() {
		return messages;
	}

	public void setMessages(long messages) {
		this.messages = messages;
	}

	public long getConsumers() {
		return consumers;
	}

	public void setConsumers(long consumers) {
		this.consumers = consumers;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	@Override
	public String toString() {
		return "Queue [memory=" + memory + ", messages=" + messages + ", consumers=" + consumers + ", name=" + name
				+ ", node=" + node + "]";
	}

}