package io.pivotal.rabbitmq.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <pre>
 * {
    "channels": 1,
    "node": "rabbit_2@Flamingo",
    "user": "bob",
    "vhost": "/"
  }
 * </pre>
 * 
 * @author mrosales
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonConnection {
	int channels;
	String node;
	String user;
	String vhost;

	public int getChannels() {
		return channels;
	}

	public void setChannels(int channels) {
		this.channels = channels;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getVhost() {
		return vhost;
	}

	public void setVhost(String vhost) {
		this.vhost = vhost;
	}

}