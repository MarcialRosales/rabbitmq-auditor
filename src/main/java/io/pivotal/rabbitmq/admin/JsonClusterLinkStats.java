package io.pivotal.rabbitmq.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonClusterLinkStats {
	long sendBytes;

	@JsonProperty("send_bytes")
	public long getSendBytes() {
		return sendBytes;
	}

	public void setSendBytes(long sendBytes) {
		this.sendBytes = sendBytes;
	}
}
