package io.pivotal.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class JsonMessageStats {
	long publish;

	public long getPublish() {
		return publish;
	}

	public void setPublish(long publish) {
		this.publish = publish;
	}
	
}