package io.pivotal.rabbitmq.admin;

public class JsonClusterLink {
	String name;
	JsonClusterLinkStats stats;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public JsonClusterLinkStats getStats() {
		return stats;
	}
	public void setStats(JsonClusterLinkStats stats) {
		this.stats = stats;
	}
	
}
