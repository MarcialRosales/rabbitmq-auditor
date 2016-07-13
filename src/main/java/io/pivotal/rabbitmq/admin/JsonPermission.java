package io.pivotal.rabbitmq.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonPermission {
	String vhost;
	String configure = ".*";
	String write = ".*";
	String read = ".*";
	
	
	public JsonPermission() {
		
	}
	
	public JsonPermission(String configure, String write, String read) {
		super();
		this.configure = configure;
		this.write = write;
		this.read = read;
	}

	public String getConfigure() {
		return configure;
	}
	public void setConfigure(String configure) {
		this.configure = configure;
	}
	public String getWrite() {
		return write;
	}
	public void setWrite(String write) {
		this.write = write;
	}
	public String getRead() {
		return read;
	}
	public void setRead(String read) {
		this.read = read;
	}
	
	public String getVhost() {
		return vhost;
	}
	public void setVhost(String vhost) {
		this.vhost = vhost;
	}

}