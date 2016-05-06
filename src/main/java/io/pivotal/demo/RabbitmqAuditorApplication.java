package io.pivotal.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@SpringBootApplication
public class RabbitmqAuditorApplication {

	private static final String auditQueue = "audit.queue";
	private static final String auditExchange = "audit.exchange";
	private static final String auditChannel = "audit.channel";
	private static final String auditConnection = "audit.connection";
	private static final String auditAuthenticationFailure = "audit.authenticationFailure";
	private static final String auditBinding = "audit.binding";
	private static final String auditUser = "audit.user";
	private static final String auditVhost = "audit.vhost";
	
	@Autowired RRDs rrds;
	
	@Scheduled(fixedDelay = 10000) 
	public void clearRRD() {
		rrds.clearRRD();
	}
	
	@Scheduled(fixedDelay = 60000) 
	public void log() {
		rrds.log();
	}
	
	@Bean
	public RRDs rrds() {
		return new RRDs();
	}
	
	@Bean
	public List<Declarable> rabbitEntities() {
		
		return Arrays.<Declarable> asList(
				new Queue(auditQueue, false, true, true),
					new Binding(auditQueue, DestinationType.QUEUE, "amq.rabbitmq.event", "queue.#", null),
				new Queue(auditChannel, false, true, true),
					new Binding(auditChannel, DestinationType.QUEUE, "amq.rabbitmq.event", "channel.#", null),
				new Queue(auditConnection, false, true, true),
					new Binding(auditConnection, DestinationType.QUEUE, "amq.rabbitmq.event", "connection.#", null),
				new Queue(auditBinding, false, true, true),
					new Binding(auditBinding, DestinationType.QUEUE, "amq.rabbitmq.event", "binding.#", null),
				new Queue(auditUser, false, true, true),
					new Binding(auditUser, DestinationType.QUEUE, "amq.rabbitmq.event", "user.*", null),
				new Queue(auditExchange, false, true, true),
					new Binding(auditExchange, DestinationType.QUEUE, "amq.rabbitmq.event", "exchange.#", null),
				new Queue(auditVhost, false, true, true),
					new Binding(auditVhost, DestinationType.QUEUE, "amq.rabbitmq.event", "vhost.#", null),
				new Queue(auditAuthenticationFailure, false, true, true),
					new Binding(auditAuthenticationFailure, DestinationType.QUEUE, "amq.rabbitmq.event", "user.authentication.failure", null)	);
	}

	@Bean
	public SimpleMessageListenerContainer auditResourcesContainer(ConnectionFactory connectionFactory, RRDs rrds) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addQueueNames(auditQueue, auditBinding, auditExchange, auditVhost, auditUser);
		container.setMessageListener(listener(rrds));
		container.setConcurrentConsumers(2);
		return container;
	}
	@Bean
	public SimpleMessageListenerContainer auditAccessContainer(ConnectionFactory connectionFactory,  RRDs rrds) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addQueueNames(auditConnection, auditChannel, auditAuthenticationFailure);
		container.setMessageListener(listener(rrds));
		container.setConcurrentConsumers(5);
		return container;
	}

	AtomicBoolean loadQueues = new AtomicBoolean(true);


	public MessageListener logger() {
		return (m) -> {
//			System.out.println("AuditQueue: " + m.getMessageProperties().getHeaders().entrySet().stream()
//					.map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList()));
//			System.out.printf("%s  vhost:%s name:%s source:%s[%s] destination:%s[%s] %s\n",
//					m.getMessageProperties().getTimestamp(), m.getMessageProperties().getHeaders().get("vhost"),
//					m.getMessageProperties().getHeaders().get("name"),
//					m.getMessageProperties().getHeaders().get("source_name"),
//					m.getMessageProperties().getHeaders().get("source_kind"),
//					m.getMessageProperties().getHeaders().get("destination_name"),
//					m.getMessageProperties().getHeaders().get("destination_kind"),
//					m.getMessageProperties().getReceivedRoutingKey());
//			System.out.println(m.getBody() != null ? new String(m.getBody()) : "no body");
		};
	}
	
	@Bean
	public MessageListener listener(RRDs rrds) {
		return (m) -> {
			SimpleRRD rrd = rrds.get(m.getMessageProperties().getReceivedRoutingKey());
			if (rrd != null) {
				rrd.count(m.getMessageProperties().getTimestamp().getTime());
			}
		};
	}



	public static void main(String[] args) {
		SpringApplication.run(RabbitmqAuditorApplication.class, args);
	}
}
class RRDs {
	private Map<String, SimpleRRD> rrds;

	public RRDs() {
		List<String> names = Arrays.asList("queue.created", "queue.deleted", "channel.created", "channel.deleted", 
				"connection.created", "connection.closed", "binding.created", "binding.deleted");
		
		rrds = new HashMap<>();
		
		names.stream().forEach(n -> 
			 rrds.compute(n, (k,v) -> new SimpleRRD(k)));
		
	}
	public SimpleRRD get(String name) {
		return rrds.get(name);
	}
	public void clearRRD() {
		rrds.forEach((n, rrd) -> rrd.clearLast(1, 55));
	}
	public void log() {
		rrds.forEach((n,rrd) -> System.out.println(rrd));
	}
	public Collection<SimpleRRD> getAll() {
		return rrds.values();
	}
}
class SimpleRRD {
	private String name;
	
	private int last24Hours[] = new int[24];
	private int last60Minutes[] = new int[60];
	
	public SimpleRRD(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public int[] getLast60Minutes() {
		return last60Minutes;
	}
	public void count(long timestamp) {
		long elapsed = timestamp;
		int hour = (int)((elapsed / (60*60*1000)) % 24);
		int minute = (int)((elapsed / (60*1000)) % 60);
		last24Hours[hour]++;
		last60Minutes[minute]++;
	}
	
	public void clearLast(int retainLastHours, int retainLastMinutes) {
		long elapsed = System.currentTimeMillis();
		int hour = (int)((elapsed / (60*60*1000)) % 24);
		int minute = (int)((elapsed / (60*1000)) % 60);
		
		int diff = hour - retainLastHours;
		if (diff < 0) {
			diff = last24Hours.length + diff;
		}
		last24Hours[diff] = 0;
		
		diff = minute - retainLastMinutes;
		if (diff < 0) {
			diff = last60Minutes.length + diff;
		}
		last60Minutes[diff] = 0;
	}
	public String toString() {
		return name + "=" + Arrays.toString(last60Minutes);
	}
	 
}
@RestController
class Auditor {

	private RestTemplate restTemplate = new RestTemplate();;

	@Value("${spring.rabbitmq.admin}")
	String adminUrl;
	
	@Autowired RabbitProperties rabbit;
	
	
	RRDs rrds;
	
	@Autowired
	public void setRRDs(RRDs rrds) {
		this.rrds = rrds;
	}
	
	@RequestMapping("/")
	public JsonQueueDistribution report() {
		return generatedReport();
	}
	
	@RequestMapping("/rrds")
	public Collection<SimpleRRD> rrds() {
		return rrds.getAll();
	}
	@RequestMapping("/rrd")
	public ResponseEntity<?> rrd(@RequestParam String name) {
		SimpleRRD rrd = rrds.get(name);
		if (rrd == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(rrd);
	}

	@RequestMapping("/alarms")
	public JsonAlarms alarms(@RequestParam(required = false, defaultValue = "100") long memoryDepthThreshold,
			@RequestParam(required = false, defaultValue = "10") long connectionCountThreshold) {
		return generatedReport().checkAlarms(memoryDepthThreshold, connectionCountThreshold);
	}

	protected HttpHeaders getHttpHeaders() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		requestHeaders.set("Authorization", "Basic " + getHttpCredentials());
		return requestHeaders;
	}

	protected String getHttpCredentials() {
		String plainCreds = rabbit.getUsername() + ":" + rabbit.getPassword();
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		return base64Creds;
	}

	JsonQueueDistribution generatedReport() {

		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		ResponseEntity<List<JsonQueue>> queueResponse = restTemplate.exchange(
				"http://" + adminUrl + "/api/queues?columns=name,messages,message_bytes_ram,messages_ready,messages_unacknowledged,memory,consumers,policy,node,vhost",
				HttpMethod.GET, request, new ParameterizedTypeReference<List<JsonQueue>>() {
				});
		List<JsonQueue> queues = queueResponse.getBody();

		ResponseEntity<List<JsonConnection>> connectionResponse = restTemplate.exchange(
				"http://" + adminUrl + "/api/connections?columns=vhost,node,user,channels", HttpMethod.GET, request,
				new ParameterizedTypeReference<List<JsonConnection>>() {
				});
		List<JsonConnection> connections = connectionResponse.getBody();

		ResponseEntity<List<JsonPolicy>> policyResponse = restTemplate.exchange(
				"http://" + adminUrl + "/api/policies?columns=vhost,name,definition", HttpMethod.GET, request,
				new ParameterizedTypeReference<List<JsonPolicy>>() {
				});
		List<JsonPolicy> policies = policyResponse.getBody();

		JsonQueueDistribution distribution = new JsonQueueDistribution();
		distribution.analyze(queues, connections, policies);

		return distribution;
	}
	
}

class JsonHistogram {
	Histogram histogram;

	JsonHistogram(Histogram histogram) {
		this.histogram = histogram;
	}

	public long getSize() {
		return histogram.getCount();
	}

	public long getMin() {
		return histogram.getSnapshot().getMin();
	}

	public long getMax() {
		return histogram.getSnapshot().getMax();
	}

	public double getMedian() {
		return histogram.getSnapshot().getMedian();
	}

	public double get75thPercentile() {
		return histogram.getSnapshot().get75thPercentile();
	}

	public double get95thPercentile() {
		return histogram.getSnapshot().get95thPercentile();
	}

	public double get99thPercentile() {
		return histogram.getSnapshot().get99thPercentile();
	}

	public void update(int value) {
		histogram.update(value);
	}

	public void update(long value) {
		histogram.update(value);
	}

}

/**
 * <pre>
 *  {
    "memory": 14032,
    "messages": 0,
    "consumers": 0,
    "name": "unrouted",
    "node": "rabbit_2@Flamingo"
  }
 * </pre>
 * 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class JsonQueue {
	long memory;
	long messages; // it should be ready + unacknoweldged
	long messagesReady;
	long messagesUnacknowledged;

	long messageBytesRam; // ready + unacknowledged
	long messageBytesPersistent;

	long consumers;
	String policy;

	@JsonProperty("message_bytes_persistent")
	public long getMessageBytesPersistent() {
		return messageBytesPersistent;
	}

	public void setMessageBytesPersistent(long messageBytesPersistent) {
		this.messageBytesPersistent = messageBytesPersistent;
	}

	String name;
	String node;
	String vhost;

	@JsonProperty("message_bytes_ram")
	public long getMessageBytesRam() {
		return messageBytesRam;
	}

	public void setMessageBytesRam(long messagesBytesRam) {
		this.messageBytesRam = messagesBytesRam;
	}

	@JsonProperty("message_ready")
	public long getMessagesReady() {
		return messagesReady;
	}

	public void setMessagesReady(long messagesReady) {
		this.messagesReady = messagesReady;
	}

	@JsonProperty("messages_unacknowledged")
	public long getMessagesUnacknowledged() {
		return messagesUnacknowledged;
	}

	public void setMessagesUnacknowledged(long messagesUnacknowledged) {
		this.messagesUnacknowledged = messagesUnacknowledged;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public String getvhost() {
		return vhost;
	}

	public void setvhost(String vHost) {
		this.vhost = vHost;
	}

	public long getMemory() {
		return memory;
	}

	public void setMemory(long memory) {
		this.memory = memory;
	}

	public long getMessages() {
		return messages;
	}

	public void setMessages(long messages) {
		this.messages = messages;
	}

	public long getConsumers() {
		return consumers;
	}

	public void setConsumers(long consumers) {
		this.consumers = consumers;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	@Override
	public String toString() {
		return "Queue [memory=" + memory + ", messages=" + messages + ", consumers=" + consumers + ", name=" + name
				+ ", node=" + node + "]";
	}

}

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
class JsonConnection {
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

class JsonPolicyUsage {
	int haMode;
	int federationUpstreamSet;
	int federationUpstream;
	int messageTtl;
	int deadLetterExchange;
	int lazyQueue;
	int alternateExchange;
	int haSyncModeAutomatic;
	int haSyncModeManual;
	int expires;
	int maxLengthBytes;

	public void count(Map<String, JsonPolicy> policies, JsonQueue q) {
		if (q.getPolicy() == null) {
			return;
		}
		String key = q.getvhost().concat(q.getPolicy());
		JsonPolicy policy = policies.get(key);

		if (policy == null || policy.definition == null) {
			return;
		}

		if (policy.definition.alternateExchange != null) {
			alternateExchange++;
		}
		if (policy.definition.haMode != null) {
			haMode++;
			if (policy.definition.haSyncMode != null) {
				switch (policy.definition.haSyncMode) {
				case "automatic":
					haSyncModeAutomatic++;
					break;
				case "manual":
					haSyncModeManual++;
					break;
				}
			} else {
				haSyncModeManual++; // If it is not set then manual is assumed.
									// (https://www.rabbitmq.com/ha.html)
			}
		}
		if (policy.definition.federationUpstreamSet != null) {
			federationUpstreamSet++;
		}
		if (policy.definition.federationUpstream != null) {
			federationUpstream++;
		}
		if (policy.definition.messageTtl != null) {
			messageTtl++;
		}
		if (policy.definition.deadLetterExchange != null) {
			deadLetterExchange++;
		}
		if ("lazy".equals(policy.definition.queueMode)) {
			lazyQueue++;
		}
		if (policy.definition.maxLengthBytes != null) {
			maxLengthBytes++;
		}
	}

	public int getHaMode() {
		return haMode;
	}

	public int getFederationUpstreamSet() {
		return federationUpstreamSet;
	}

	public int getFederationUpstream() {
		return federationUpstream;
	}

	public int getMessageTtl() {
		return messageTtl;
	}

	public int getDeadLetterExchange() {
		return deadLetterExchange;
	}

	public int getLazyQueue() {
		return lazyQueue;
	}

	public int getAlternateExchange() {
		return alternateExchange;
	}

	public int getHaSyncModeAutomatic() {
		return haSyncModeAutomatic;
	}

	public int getHaSyncModeManual() {
		return haSyncModeManual;
	}

	public int getExpires() {
		return expires;
	}

	public int getMaxLengthBytes() {
		return maxLengthBytes;
	}

}

class JsonAlarms {
	private long memoryDepthThreshold;
	private long connectionCountThreshold;

	public long getMemoryDepthThreshold() {
		return memoryDepthThreshold;
	}

	public long getConnectionCountThreshold() {
		return connectionCountThreshold;
	}

	public JsonAlarms(long memoryDepthThreshold, long connectionCountThreshold) {
		super();
		this.memoryDepthThreshold = memoryDepthThreshold;
		this.connectionCountThreshold = connectionCountThreshold;
	}

	private long queueCount;
	private long queuesWithoutConsumers;
	private long queuesExceedThreshold;
	private long vHostExceedConnnectionThreshold;

	public long getQueuesWithoutConsumers() {
		return queuesWithoutConsumers;
	}

	public void countQueuesWithoutConsumers() {
		this.queuesWithoutConsumers++;
	}

	public long getQueuesExceedThreshold() {
		return queuesExceedThreshold;
	}

	public void countQueuesExceedThreshold() {
		this.queuesExceedThreshold++;
	}

	public long getvHostExceedConnnectionThreshold() {
		return vHostExceedConnnectionThreshold;
	}

	public void countvHostExceedConnnectionThreshold() {
		this.vHostExceedConnnectionThreshold++;
	}

	public long getQueueCount() {
		return queueCount;
	}

	public void setQueueCount(long queueCount) {
		this.queueCount = queueCount;
	}

}

class JsonQueueDistribution {
	private Map<String, JsonNodeQueueDistribution> nodes = new HashMap<>();
	Map<String, JsonVHostInfo> vhosts = new HashMap<>();
	private Map<String, JsonPolicy> policies = new HashMap<>();
	Map<String, JsonQueue> queues = new HashMap<>();

	final MetricRegistry metrics = new MetricRegistry();
	private JsonHistogram queueDistributionPerHost = new JsonHistogram(metrics.histogram("queueDistributionPerHost"));
	private JsonHistogram queueDistributionPerVhost = new JsonHistogram(metrics.histogram("queueDistributionPerVhost"));
	private JsonHistogram connectionDistributionPerHost = new JsonHistogram(
			metrics.histogram("connectionDistributionPerHost"));
	private JsonHistogram connectionDistributionPerVhost = new JsonHistogram(
			metrics.histogram("connectionDistributionPerVHost"));
	private JsonPolicyUsage policyUsage = new JsonPolicyUsage();

	int queueCount;
	int connectionCount;
	private List<JsonVHostInfo> topMemoryConsumers;

	public JsonAlarms checkAlarms(long memoryDepthThreshold, long connectionCountThreshold) {
		JsonAlarms json = new JsonAlarms(memoryDepthThreshold, connectionCountThreshold);
		json.setQueueCount(queues.size());
		queues.forEach((k, v) -> {
			if (v.getMessages() > 0 && v.getConsumers() < 1) {
				json.countQueuesWithoutConsumers();
			}
			if (v.getMessages() > memoryDepthThreshold) {
				json.countQueuesExceedThreshold();
			}
		});
		vhosts.forEach((k, v) -> {
			if (v.getConnectionCount() > connectionCountThreshold) {
				json.countvHostExceedConnnectionThreshold();
			}
		});

		return json;
	}

	void analyze(List<JsonQueue> queues, List<JsonConnection> connections, List<JsonPolicy> policies) {
		policies.stream().forEach(p -> count(p));

		queues.stream().forEach(q -> count(q)

		);
		connections.stream().forEach(c -> count(c));

		calculate();
	}
	
	public void count(JsonQueue q) {
		String key = q.getvhost().concat(q.getName());
		queues.computeIfAbsent(key, k -> q);

		nodes.computeIfAbsent(q.getNode(), JsonNodeQueueDistribution::new);
		nodes.get(q.getNode()).count(q);

		vhosts.computeIfAbsent(q.getvhost(), JsonVHostInfo::new);
		vhosts.get(q.getvhost()).count(q);

		policyUsage.count(policies, q);

		queueCount++;
	}

	public void count(JsonConnection c) {
		nodes.computeIfAbsent(c.getNode(), JsonNodeQueueDistribution::new);
		nodes.get(c.getNode()).count(c);

		vhosts.computeIfAbsent(c.getVhost(), JsonVHostInfo::new);
		vhosts.get(c.getVhost()).count(c);

		connectionCount++;

	}

	public void count(JsonPolicy p) {
		String key = p.vhost.concat(p.name);
		policies.put(key, p);

	}

	public void calculate() {
		// calculate queueDistributionPerHost
		List<JsonVHostInfo> lvHosts = new ArrayList<>(vhosts.values());

		Comparator<JsonVHostInfo> byMessageBytesInRam = (e1, e2) -> Long.compare(
				e2.getTotalMemory() + e2.getTotalMessageBytesRam(), e1.getTotalMemory() + e1.getTotalMessageBytesRam());
		topMemoryConsumers = lvHosts.stream().sorted(byMessageBytesInRam).limit(10).collect(Collectors.toList());

		nodes.values().forEach(n -> {
			queueDistributionPerHost.update(n.getQueueCount());
			connectionDistributionPerHost.update(n.getConnectionCount());
		});
		lvHosts.forEach(v -> {
			queueDistributionPerVhost.update(v.getQueueCount());
			connectionDistributionPerVhost.update(v.getConnectionCount());
		});

	}

	public Collection<JsonNodeQueueDistribution> getQueueDistributionPerNode() {
		return nodes.values();
	}

	public JsonHistogram getQueueDistributionPerHost() {
		return queueDistributionPerHost;
	}

	public JsonHistogram getQueueDistributionPerVhost() {
		return queueDistributionPerVhost;
	}

	public JsonHistogram getConnectionDistributionPerHost() {
		return connectionDistributionPerHost;
	}

	public JsonHistogram getConnectionDistributionPerVhost() {
		return connectionDistributionPerVhost;
	}

	public JsonPolicyUsage getPolicyUsage() {
		return policyUsage;
	}

	public int getVhostCount() {
		return vhosts.size();
	}

	public int getQueueCount() {
		return queueCount;
	}

	public int getConnectionCount() {
		return connectionCount;
	}

	public List<JsonVHostInfo> getTopVHostRAMConsumers() {
		return topMemoryConsumers;
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
class JsonPolicy {

	String name;
	JsonPolicyDefinition definition; // = new JsonPolicyDefinition();
	String vhost;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JsonPolicyDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(JsonPolicyDefinition definition) {
		this.definition = definition;
	}

	public String getVhost() {
		return vhost;
	}

	public void setVhost(String vhost) {
		this.vhost = vhost;
	}

}

@JsonIgnoreProperties(ignoreUnknown = true)
class JsonPolicyDefinition {
	String haMode;
	String federationUpstreamSet;
	String federationUpstream;
	String messageTtl;
	String deadLetterExchange;
	String queueMode;
	String alternateExchange;
	String haSyncMode;
	String expires;
	String maxLengthBytes;

	@JsonProperty("ha-mode")
	public String getHaMode() {
		return haMode;
	}

	public void setHaMode(String haMode) {
		this.haMode = haMode;
	}

	@JsonProperty("federation-upstream-set")
	public String getFederationUpstreamSet() {
		return federationUpstreamSet;
	}

	public void setFederationUpstreamSet(String federationUpstreamSet) {
		this.federationUpstreamSet = federationUpstreamSet;
	}

	@JsonProperty("federation-upstream")
	public String getFederationUpstream() {
		return federationUpstream;
	}

	public void setFederationUpstream(String federationUpstream) {
		this.federationUpstream = federationUpstream;
	}

	@JsonProperty("message-ttl")
	public String getMessageTtl() {
		return messageTtl;
	}

	public void setMessageTtl(String messageTtl) {
		this.messageTtl = messageTtl;
	}

	@JsonProperty("dead-letter-exchange")
	public String getDeadLetterExchange() {
		return deadLetterExchange;
	}

	public void setDeadLetterExchange(String deadLetterExchange) {
		this.deadLetterExchange = deadLetterExchange;
	}

	@JsonProperty("queue-mode")
	public String getQueueMode() {
		return queueMode;
	}

	public void setQueueMode(String queueMode) {
		this.queueMode = queueMode;
	}

	@JsonProperty("alternate-exchange")
	public String getAlternateExchange() {
		return alternateExchange;
	}

	@JsonProperty("alternate_exchange")
	public void setAlternateExchange(String alternateExchange) {
		this.alternateExchange = alternateExchange;
	}

	@JsonProperty("ha-sync-mode")
	public String getHaSyncMode() {
		return haSyncMode;
	}

	public void setHaSyncMode(String haSyncMode) {
		this.haSyncMode = haSyncMode;
	}

	@JsonProperty("expires")
	public String getExpires() {
		return expires;
	}

	public void setExpires(String expires) {
		this.expires = expires;
	}

	@JsonProperty("max-length-bytes")
	public String getMaxLengthBytes() {
		return maxLengthBytes;
	}

	public void setMaxLengthBytes(String maxLengthBytes) {
		this.maxLengthBytes = maxLengthBytes;
	}

}

class JsonVHostInfo {
	private String name;
	private int queueCount;
	private int connectionCount;

	private long totalMemory;
	private long totalMessageCount;
	private long totalMessageBytesRam;
	private long totalUnacknowledgedMessageCount;

	JsonVHostInfo(String name) {
		this.name = name;
	}

	public void count(JsonQueue q) {
		queueCount++;
		totalMemory += q.getMemory();
		totalMessageBytesRam += q.getMessageBytesRam();
		totalMessageCount += q.getMessages();
		totalUnacknowledgedMessageCount += q.getMessagesUnacknowledged();
	}

	public void count(JsonConnection c) {
		connectionCount++;
	}

	public int getQueueCount() {
		return queueCount;
	}

	public String getName() {
		return name;
	}

	public int getConnectionCount() {
		return connectionCount;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public long getTotalMessageCount() {
		return totalMessageCount;
	}

	public long getTotalMessageBytesRam() {
		return totalMessageBytesRam;
	}

	public long getTotalUnacknowledgedMessageCount() {
		return totalUnacknowledgedMessageCount;
	}

}

class JsonNodeQueueDistribution {
	private String name;
	private int queueCount;
	private long totalQueueMemory;
	private long totalMessageBytesRam;
	private long totalMessageCount;
	private long totalUnacknowledgedMessageCount;
	// private int mirrorQueueCount;

	private long connectionCount;

	JsonNodeQueueDistribution(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getQueueCount() {
		return queueCount;
	}

	public void setQueueCount(int queueCount) {
		this.queueCount = queueCount;
	}

	// public int getMirrorQueueCount() {
	// return mirrorQueueCount;
	// }
	// public void setMirrorQueueCount(int mirrorQueueCount) {
	// this.mirrorQueueCount = mirrorQueueCount;
	// }
	public void count(JsonQueue q) {
		queueCount++;
		totalQueueMemory += q.getMemory();
		totalMessageBytesRam += q.getMessageBytesRam();
		totalMessageCount += q.getMessages();
		totalUnacknowledgedMessageCount += q.getMessagesUnacknowledged();
	}

	public void count(JsonConnection c) {
		connectionCount++;
	}

	public long getTotalQueueMemory() {
		return totalQueueMemory;
	}

	public long getTotalMessageBytesRam() {
		return totalMessageBytesRam;
	}

	public long getTotalMessageCount() {
		return totalMessageCount;
	}

	public long getTotalUnacknowledgedMessageCount() {
		return totalUnacknowledgedMessageCount;
	}

	public long getConnectionCount() {
		return connectionCount;
	}

}

class JsonChannel {
	String node;
	String vhost;
	int prefetchCount;
	boolean confirm;
	boolean transactional;
	int consumerCount;

}
