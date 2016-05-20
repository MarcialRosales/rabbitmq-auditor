package io.pivotal.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class JsonPermission {
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
	
}