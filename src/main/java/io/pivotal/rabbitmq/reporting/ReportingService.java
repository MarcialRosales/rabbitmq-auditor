package io.pivotal.rabbitmq.reporting;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.pivotal.rabbitmq.admin.JsonConnection;
import io.pivotal.rabbitmq.admin.JsonQueue;
import io.pivotal.rabbitmq.admin.RabbitAdmin;


@Service
@Profile("Reporting")
@RestController()
public class ReportingService {

	private Logger logger = LoggerFactory.getLogger(ReportingService.class);
	private RabbitAdmin admin;
	
	@Autowired
	public ReportingService(RabbitAdmin admin) {
		this.admin = admin;
	}
	
	/**
	 * Produces a report of resource's usage among nodes, vhosts and users. The resources are : connections, channels and queues.
	 *  
	 * @param connectionThreshold
	 * @return
	 */
	@RequestMapping(path = "/api/resources/usage")
	public JsonResourceUsage connectivityReport() {
		JsonResourceUsage report = new JsonResourceUsage(); 
		admin.listConnections().stream().forEach(c -> report.take(c));
		admin.listQueues().stream().forEach(q -> report.take(q));
		
		return report;
	}
	
	

}


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
@JsonIgnoreProperties(ignoreUnknown = true)
class JsonResourceUsage {
	private Map<String, JsonNode> nodes = new HashMap<>(); 
	private Map<String, JsonVhostReport> vhosts = new HashMap<>();
	int connectionCount;
	int channelCount;
	int queueCount;
	
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
		connectionCount++;
		channelCount+=connection.getChannels();
		
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
		queueCount++;
	}
	
	public Collection<JsonNode> getNodes() {
		return nodes.values();
	}
	public Collection<JsonVhostReport> getVhosts() {
		return vhosts.values();
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
	int channelCount;
	int queueCount;
	
	JsonNode(String name) {
		this.name = name;
	}
	public void take(JsonConnection connection) {
		connectionCount++;
		channelCount+=connection.getChannels();
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
}
