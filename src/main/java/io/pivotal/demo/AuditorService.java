package io.pivotal.demo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Service
@Profile("Auditing")
@RestController("auditor")
public class AuditorService {

	private Logger logger = LoggerFactory.getLogger(AuditorService.class);
	
	private static final String auditor = "auditor";
	private Set<String> monitorVhosts = Collections.synchronizedSet(new HashSet<>());

	// this is necessary until the connection.closed and channel.closed contains the vhost
	private Map<String, String> pids = new HashMap<>();
	
	@Bean
	public List<Declarable> prepareListener() {
		return Arrays.<Declarable> asList(new Queue(auditor, false, false, true),
				new Binding(auditor, DestinationType.QUEUE, "amq.rabbitmq.event", "#", null),
				new Binding(auditor, DestinationType.QUEUE, "amq.rabbitmq.event", "consumer.#", null),
				new Binding(auditor, DestinationType.QUEUE, "amq.rabbitmq.event", "channel.#", null),
				new Binding(auditor, DestinationType.QUEUE, "amq.rabbitmq.event", "connection.#", null),
				new Binding(auditor, DestinationType.QUEUE, "amq.rabbitmq.event", "queue.#", null));

	}

	@RabbitListener(queues = auditor)
	public void processPolicyEvent(Message m) {
		String vhost = (String) m.getMessageProperties().getHeaders().get("vhost");
		
			String event = m.getMessageProperties().getReceivedRoutingKey();
			switch(event) {
			case "connection.created":
			case "channel.created":
				pids.put(m.getMessageProperties().getHeaders().get("pid").toString(), vhost);
				break;
			case "connection.closed":
			case "channel.closed":
				vhost = pids.remove(m.getMessageProperties().getHeaders().get("pid").toString());
			}
		
		if (monitorVhosts.contains(vhost)) {
			logger.info("{} {}", m.getMessageProperties().getReceivedRoutingKey(), m.getMessageProperties().toString());		
		}else if (logger.isDebugEnabled()) {
			logger.debug("XX {} {}", m.getMessageProperties().getReceivedRoutingKey(), m.getMessageProperties().toString());
		}
			
	}

	@RequestMapping("auditor/start/{vhost}")
	public void start(@PathVariable String vhost) {
		monitorVhosts.add(vhost);
	}
	@RequestMapping("auditor/stop/{vhost}")
	public void stop(@PathVariable String vhost) {
		monitorVhosts.remove(vhost);
	}
	@RequestMapping("auditor") 
	public String list() {
		return monitorVhosts.toString();
	}
}
