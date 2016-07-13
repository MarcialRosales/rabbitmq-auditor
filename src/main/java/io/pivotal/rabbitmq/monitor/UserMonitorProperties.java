package io.pivotal.rabbitmq.monitor;

public class UserMonitorProperties {
	String name;
	int minConnections;
	int maxConnections;
	
	public UserMonitorProperties() {
		
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	boolean checked;
	public void setChecked(){
		checked = true;		
	}
	public boolean getCheckedAndClear() {
		connectionCount = 0;
		boolean val = checked;
		checked = false;
		return val;
	}
	public int getMinConnections() {
		return minConnections;
	}
	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}
	public int getMaxConnections() {
		return maxConnections;
	}
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}


	int connectionCount;
	public void addConnection() {
		connectionCount++;
	}
	
}