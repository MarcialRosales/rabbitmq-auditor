package io.pivotal.demo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Connection/Channel view:
 * top #vhost according to # connections
 * top #vhost according to # channels
 * top #user according to #  
 * 
 * Distribution per node of queues, and connections
 * 
 * @author mrosales
 *
 */
@Service
@Profile("Auditing")
@RestController("auditor")
public class AuditorService {

	private Logger logger = LoggerFactory.getLogger(AuditorService.class);
	private RabbitAdmin admin;
	
	@Autowired
	public AuditorService(RabbitAdmin admin) {
		this.admin = admin;
	}
	
	@RequestMapping("/connectivityReport")
	public JsonConnectivityReport connectivityReport() {
		JsonConnectivityReport report = new JsonConnectivityReport(); 
		admin.listConnections().stream().forEach(c -> report.take(c));
		admin.listQueues().stream().forEach(q -> report.take(q));
		return report;
	}

}
@JsonIgnoreProperties(ignoreUnknown = true)
class JsonConnectivityReport {
	private Map<String, JsonNode> nodes = new HashMap<>(); 
	private Map<String, JsonVhostReport> vhosts = new HashMap<>();
	
	
	public void take(JsonConnection connection) {
		JsonNode node = nodes.get(connection.getNode());
		if (node == null) {
			nodes.put(connection.getNode(), node = new JsonNode(connection.getNode()));
		}
		node.take(connection);
		JsonVhostReport vhost = vhosts.get(connection.getVhost());
		if (vhost == null) {
			vhosts.put(connection.getVhost(), vhost = new JsonVhostReport(connection.getVhost()));
		}
		vhost.take(connection);
	}
	public void take(JsonQueue queue) {
		JsonNode node = nodes.get(queue.getNode());
		if (node == null) {
			nodes.put(queue.getNode(), node = new JsonNode(queue.getNode()));
		}
		node.take(queue);
		JsonVhostReport vhost = vhosts.get(queue.getVhost());
		if (vhost == null) {
			vhosts.put(queue.getVhost(), vhost = new JsonVhostReport(queue.getVhost()));
		}
		vhost.take(queue);
	}
	public Collection<JsonNode> getNodes() {
		return nodes.values();
	}
	public Collection<JsonVhostReport> getVhosts() {
		return vhosts.values();
	}
}

class JsonVhostReport {
	int connectionCount;
	int channelCount;
	int queueCount;
	Map<String, JsonUserReport> users = new HashMap<>();
	
	String name;
	public JsonVhostReport(String name) {
		this.name = name;
	}
	public void take(JsonConnection connection) {
		connectionCount++;
		channelCount+=connection.getChannels();
		
		JsonUserReport user = users.get(connection.getUser());
		if (user == null) {
			users.put(connection.getUser(), user = new JsonUserReport(connection.getUser()));
		}
		user.take(connection);
	}
	
	public void take(JsonQueue queue) {
		queueCount++;
	}
	public String getName() {
		return name;
	}
	public int getConnectionCount() {
		return connectionCount;
	}
	public int getChannelCount() {
		return channelCount;
	}
	public int getQueueCount() {
		return queueCount;
	}
	
	public Collection<JsonUserReport> getUsers() {
		return users.values();
	}
}
class JsonUserReport {
	String name;
	int connectionCount;
	int channelCount;
	
	public JsonUserReport(String name) {
		this.name = name;
	}
	public void take(JsonConnection connection) {
		connectionCount++;
		channelCount+=connection.getChannels();
	}
	public String getName() {
		return name;
	}
	public int getConnectionCount() {
		return connectionCount;
	}
	public int getChannelCount() {
		return channelCount;
	}

	
}
class JsonNode {
	String name;
	int connectionCount;
	int queueCount;
	
	JsonNode(String name) {
		this.name = name;
	}
	public void take(JsonConnection connection) {
		connectionCount++;
	}
	public void take(JsonQueue queue) {
		queueCount++;
	}
	public String getName() {
		return name;
	}
	public int getConnectionCount() {
		return connectionCount;
	}
	public int getQueueCount() {
		return queueCount;
	}
}
