package io.pivotal.rabbitmq.monitor;

public class QueueMonitorProperties {
	String name;
	int depthThreshold;
	int unacknowledgedThreshold;
	int minConsumers;
	
	public QueueMonitorProperties() {
		
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getDepthThreshold() {
		return depthThreshold;
	}
	public void setDepthThreshold(int queueDepthThreshold) {
		this.depthThreshold = queueDepthThreshold;
	}
	public int getMinConsumers() {
		return minConsumers;
	}
	public void setMinConsumers(int minConsumers) {
		this.minConsumers = minConsumers;
	}
	boolean checked;
	public void setChecked(){
		checked = true;		
	}
	public boolean getCheckedAndClear() {
		boolean val = checked;
		checked = false;
		return val;
	}


	public int getUnacknowledgedThreshold() {
		return unacknowledgedThreshold;
	}
	public void setUnacknowledgedThreshold(int unacknowledgedThreshold) {
		this.unacknowledgedThreshold = unacknowledgedThreshold;
	}
}