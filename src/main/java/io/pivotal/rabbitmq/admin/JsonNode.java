package io.pivotal.rabbitmq.admin;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonNode {
	String name;
	boolean running;
	boolean memAlarm;
	boolean diskAlarm;
	long runQueue;
	long ioReopenCount;
	long msgStoreReadCount;
	long msgStoreWriteCount;
	List<JsonClusterLink> clusterLinks = new ArrayList<>();
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean getRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}
	@JsonProperty("mem_alarm")
	public boolean getMemAlarm() {
		return memAlarm;
	}
	public void setMemAlarm(boolean memAlarm) {
		this.memAlarm = memAlarm;
	}
	@JsonProperty("disk_alarm")
	public boolean getDiskAlarm() {
		return diskAlarm;
	}
	public void setDiskAlarm(boolean diskAlarm) {
		this.diskAlarm = diskAlarm;
	}
	@JsonProperty("run_queue")
	public long getRunQueue() {
		return runQueue;
	}
	public void setRunQueue(long runQueue) {
		this.runQueue = runQueue;
	}
	@JsonProperty("io_reopen_count")
	public long getIoReopenCount() {
		return ioReopenCount;
	}
	public void setIoReopenCount(long ioReopenCount) {
		this.ioReopenCount = ioReopenCount;
	}
	@JsonProperty("msg_store_read_count")
	public long getMsgStoreReadCount() {
		return msgStoreReadCount;
	}
	public void setMsgStoreReadCount(long msgStoreReadCount) {
		this.msgStoreReadCount = msgStoreReadCount;
	}
	@JsonProperty("msg_store_write_count")
	public long getMsgStoreWriteCount() {
		return msgStoreWriteCount;
	}
	public void setMsgStoreWriteCount(long msgStoreWriteCount) {
		this.msgStoreWriteCount = msgStoreWriteCount;
	}
	
	@JsonProperty("cluster_links")
	public List<JsonClusterLink> getClusterLinks() {
		return clusterLinks;
	}
	public void setClusterLinks(List<JsonClusterLink> clusterLinks) {
		this.clusterLinks = clusterLinks;
	}
	
	
}
