package io.pivotal.rabbitmq.monitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import io.pivotal.rabbitmq.admin.JsonNode;
import io.pivotal.rabbitmq.admin.JsonQueue;
import io.pivotal.rabbitmq.admin.RabbitAdmin;
import io.pivotal.rabbitmq.policy.NotEraseableUsernamePasswordAuthenticationToken;

@Service
@Profile("Monitoring")
@EnableConfigurationProperties(MonitorServiceProperties.class)
public class MonitorService {

	private Logger logger = LoggerFactory.getLogger(MonitorService.class);
	private Logger nodesLogger = LoggerFactory.getLogger(MonitorService.class.getName() + "-nodes");
	
	@Autowired
	RabbitAdmin admin;
	
	@Autowired
	MonitorServiceProperties settings;
	
	@Autowired
	private RabbitProperties rabbit;
	
	private NotEraseableUsernamePasswordAuthenticationToken adminToken;

	Report report = new Report();
	
	@PostConstruct
	void init() {
		logger.info("Started MonitorService");
		adminToken = new NotEraseableUsernamePasswordAuthenticationToken(rabbit.getUsername(), rabbit.getPassword());
	}

	long monitorNodesCount;
	StringBuilder monitorNodes = new StringBuilder();
	
	private Map<String, AtomicLong> lastClusterLink =  new HashMap<>();
	private long lastSampleMs;
	
	@Scheduled(fixedDelay = 10000) // TODO externalize fixed delay
	private void monitorNodes() {
	
		if (monitorNodesCount == 0) {
			nodesLogger.info("listNodesLatency,[name,run_queue,state|alarms,ioReOpen,msgReadStore,msgWriteStore,clusterLinkRate],[...],"); 
		}
		try {
			SecurityContextHolder.getContext().setAuthentication(adminToken);
			long listNodesLatency = System.currentTimeMillis();
			List<JsonNode> nodes = admin.listNodes();
			listNodesLatency = System.currentTimeMillis() - listNodesLatency;
			
			monitorNodes.setLength(0);
			monitorNodes.append(listNodesLatency).append(",");
			nodes.stream().forEach(n -> {
				monitorNodes.append(" [").append(n.getName()).append(",").append(n.getRunQueue()).append(",");
				if (!n.getRunning()) {
					monitorNodes.append("no running");
				}else {
					monitorNodes.append("running");
					if (n.getMemAlarm()) {
						monitorNodes.append("Mem|");
					}
					if (n.getDiskAlarm()) {
						monitorNodes.append("Disk|");
					}
					// TODO partitions
					monitorNodes.append(",").append(n.getIoReopenCount()).append(",")
					      	.append(n.getMsgStoreReadCount()).append(",")
					      	.append(n.getMsgStoreWriteCount()).append(",");
				}
				long newClusterLink = n.getClusterLinks().stream().mapToLong(l -> l.getStats().getSendBytes()).sum();
				AtomicLong count = lastClusterLink.computeIfAbsent(n.getName(), (k) -> {return new AtomicLong(newClusterLink);});
				if (count.get() > 0 ) {
					long delta = newClusterLink - count.get();
					count.set(newClusterLink);
					long currentMs = System.currentTimeMillis();
					long elapsed = (currentMs - lastSampleMs)/1000;
					lastSampleMs = currentMs;
					monitorNodes.append(elapsed > 0 ? delta/elapsed : 0); // per second
				}
				monitorNodes.append("], ");
			});
			nodesLogger.info(monitorNodes.toString());
			
		}catch(Exception e) {
			nodesLogger.error("Failed to monitor", e);
		}finally {
			monitorNodesCount++;
			SecurityContextHolder.getContext().setAuthentication(null);				
		}
	}
	
	@Scheduled(fixedDelay = 5000) // TODO externalize fixed delay
	private void monitor() {
	
		long t0 = System.currentTimeMillis();
		
		report.clear();
		try {
			SecurityContextHolder.getContext().setAuthentication(adminToken);
			long listQueueLatency = System.currentTimeMillis();
			List<JsonQueue> queues = admin.listQueues(settings.getVhost());
			listQueueLatency = System.currentTimeMillis() - listQueueLatency;
			queues.stream().forEach(q -> settings.report(report, q));
			
			settings.checkAllQueuesExist(report);
			
			
			if (listQueueLatency > settings.getMgtApiLatencyThreshold()) {
				report.reportHighLatency("listQueues", listQueueLatency);
			}
			
			if (!report.isEmpty()) {
				logger.info(report.toString());
			}else if (logger.isDebugEnabled()) {
				logger.debug("Ok ({})", System.currentTimeMillis() - t0);
			}
			
			
		}catch(IOException e) {
			logger.error("Failed to monitor", e);
		}finally {
			SecurityContextHolder.getContext().setAuthentication(null);
		}
		
	}
	
	
}
class Report {
	StringBuilder sb  = new StringBuilder();
	
	void queueNotRunning(String q, String mode) {
		sb.append(q).append(" ").append(mode).append(", ");
	}
	void reportLowConsumers(String q, long current, int min) {
		sb.append("Low consumer ").append(q).append(" ").append(current).append("/").append(min).append(", ");
	}
	void reportDepthAlarm(String q, long current, int threshold) {
		sb.append("Queue depth alarm ").append(q).append(" ").append(current).append("/").append(threshold).append(", ");
	}
	void reportUnacknowledgedAlarm(String q, long current, int threshold) {
		sb.append("Queue unacknowledged alarm ").append(q).append(" ").append(current).append("/").append(threshold).append(", ");
	}
	void reportMissingQueue(String q) {
		sb.append("Missing queue ").append(q).append(", ");
	}
	
	void reportHighLatency(String mgtApi, long latency) { 
		sb.append("High latency: ").append(mgtApi).append(" ").append(latency);
	}

	boolean isEmpty() {
		return sb.length() == 0;
	}
	void clear() {
		sb.setLength(0);
	}
	
	public String toString() {
		return sb.toString();
	}
}

@ConfigurationProperties("monitor")
class MonitorServiceProperties {
	
	private static final String RUNNING = "running";

	@NotNull
	String vhost;
	
	long runQueueThreshold;
	long mgtApiLatencyThreshold;
	

	List<QueueMonitorProperties> queues = new ArrayList<>();
	List<UserMonitorProperties> users = new ArrayList<>();
	Map<String, QueueMonitorProperties> queueIndex = new HashMap<>();
	Map<String, UserMonitorProperties> userIndex = new HashMap<>();
	
	int queueDepthThreshold;
	
	public int getQueueDepthThreshold() {
		return queueDepthThreshold;
	}

	public void setQueueDepthThreshold(int queueDepthThreshold) {
		this.queueDepthThreshold = queueDepthThreshold;
	}

	public List<QueueMonitorProperties> getQueues() {
		return queues;
	}

	public void setQueues(List<QueueMonitorProperties> queues) {
		this.queues = queues;
	}
	

	public long getRunQueueThreshold() {
		return runQueueThreshold;
	}

	public void setRunQueueThreshold(long runQueueThreshold) {
		this.runQueueThreshold = runQueueThreshold;
	}

	public String getVhost() {
		return vhost;
	}

	public void setVhost(String vhost) {
		this.vhost = vhost;
	}
	
	public long getMgtApiLatencyThreshold() {
		return mgtApiLatencyThreshold;
	}

	public void setMgtApiLatencyThreshold(long mgtApiLatencyThreshold) {
		this.mgtApiLatencyThreshold = mgtApiLatencyThreshold;
	}
	
	public boolean hasQueue(String name) {
		return queues.contains(name);
	}
	public void report(Report report, JsonQueue queue) {
		QueueMonitorProperties qm = queueIndex.get(queue.getName());
		if (qm == null) {
			return;
		}
		if (!RUNNING.equals(queue.getState())) {
			report.queueNotRunning(queue.getName(), queue.getState());
		}
		if (queue.getConsumers() < qm.getMinConsumers()) {
			report.reportLowConsumers(queue.getName(), queue.getConsumers(), qm.getMinConsumers());
		}
		if (queue.getMessagesReady() > qm.getDepthThreshold()) {
			report.reportDepthAlarm(queue.getName(), queue.getMessages(), qm.getDepthThreshold());
		}
		if (queue.getMessagesUnacknowledged() > qm.getUnacknowledgedThreshold()) {
			report.reportDepthAlarm(queue.getName(), queue.getMessagesUnacknowledged(), qm.getUnacknowledgedThreshold());
		}
		
		qm.setChecked();
			
	}
	public void checkAllQueuesExist(final Report report) {
	queues.stream().forEach(qm -> {
			if (!qm.getCheckedAndClear()) report.reportMissingQueue(qm.getName());
		});		
	}
	
	@PostConstruct
	void init() {
		queues.stream().forEach(q -> queueIndex.put(q.getName(), q));
		users.stream().forEach(q -> userIndex.put(q.getName(), q));
	}
}
