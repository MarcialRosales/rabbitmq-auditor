package io.pivotal.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * policy:
 * {  enforce: { max-length-bytes: 1000000, max-length: 10000, message-ttl: 60000 },
 *    permissions: [
 *      permitAll: true,
 *    	{ ha-mode: all, permit: false },
 *    	{ ha-mode: nodes, permit: false },
 *      { ha-mode: exactly, ha-params:2, permit: true },
 *     ]
 *  } 
 * @author mrosales
 *
 */
@Service
public class PolicyEnforceService {

	private Logger logger = LoggerFactory.getLogger(PolicyEnforceService.class);
	
	private RabbitAdmin admin;
	private ApplicationEventPublisher publisher;
	private PlanConfiguration plan;
	
	private static final String auditPolicy = "policyEnforcer";
	private static final String ROOT_VHOST = "/";
	
	@Autowired
	public PolicyEnforceService(RabbitAdmin admin, ApplicationEventPublisher publisher, PlanConfiguration plan) {
		super();
		this.admin = admin;
		this.publisher = publisher;
		this.plan = plan;
	}

	@Bean
	public List<Declarable> prepareListener() {
		return Arrays.<Declarable> asList(new Queue(auditPolicy, false, false, true),
				new Binding(auditPolicy, DestinationType.QUEUE, "amq.rabbitmq.event", "policy.#", null),
				new Binding(auditPolicy, DestinationType.QUEUE, "amq.rabbitmq.event", "vhost.created", null));

	}
	
	
	@PostConstruct
	public void start() {
		try {
			if (admin.isThisUserAdministrator()) {
				applyOrCheckPlan();
			}else {
				logger.warn("RabbitMQ User has no administrator rights to enforce policies!!!!");
			}
		}catch(Exception e) {
			logger.error("Failed to check user is administrator", e);
		}
	}

	@RabbitListener(queues = auditPolicy)
	public void processPolicyEvent(Message m) {
		String name = (String) m.getMessageProperties().getHeaders().get("name");
		String vhost = (String) m.getMessageProperties().getHeaders().get("vhost");
		
		if (ROOT_VHOST.equals(vhost)) {
			return; // we don't handle events that occur in the root as only administrators are allowed to operate on that vhost
		}
			
		switch (m.getMessageProperties().getReceivedRoutingKey()) {
		case "policy.set":
			publisher.publishEvent(new PolicyChangedEvent(vhost, name));
			break;
		case "policy.cleared":
			publisher.publishEvent(new PolicyClearedEvent(vhost, name));
			break;
		case "vhost.created":
			publisher.publishEvent(new VHostCreatedEvent(name));
			break;
		}
	}

	/**
	 * 
	 * Template policies
	 * 1) HA ha.* { ha-mode: exactly, ha-params: 2, max-length: 1000}
	 * 2) .* {max-length: 1000}
	 * 
	 * Scenario 1:
	 * User creates :  ALT alt { alternate-exchange: mine, priority: 1000 }
	 * Operations tool merges: ALT alt { alternate-exchange: mine, max-length: 1000, priority: 1000 }
	 * Scenario 2: 
	 * User creates :  myHA ha.* { ha-mode: all, priority: 1000 }
	 * Operations tool merges && overrides: myHa ha.* { ha-mode: exactly, ha-params:2, max-length:1000, priority:1000 }
	 *  
	 * @param event
	 */
	@EventListener
	public void handlePolicyChangedEvent(PolicyChangedEvent event) {
		
		JsonPolicy policy = null;
		try {
			policy = admin.find(event.vhost, event.name);
		}catch(Exception e) {
			logger.error("Failed to load policy", e);
			return;
		}

		if (!plan.isCompliant(policy)) {
			logger.info("Policy {} not compliant on vhost {}. Overriding..", event.name, event.vhost);
			try {
				admin.updatePolicy(plan.enforce(policy));
			}catch(Exception e) {
				logger.error("Failed to update policy", e);
			}
		}
	}
	
	/** 
	 * Make sure the policy plan was not cleared. And if it was cleared, we create it again. 
	 * @param event
	 */
	@EventListener
	public void handlePolicyClearedEvent(PolicyClearedEvent event) {
		
		if (plan.getName().equals(event.name)) {
			logger.info("Policy plan {} cleared on vhost {}", event.name, event.vhost);
			try {
				admin.updatePolicy(plan.buildPolicyFor(event.vhost));
			} catch (Exception e) {
				logger.error("Failed to apply plan policy {} to vhost {}", event.name, event.vhost);
			}
		}
	}
	

	/** 
	 * Make sure that at least the policy plan exists on the new vhost. 
	 * @param event
	 */
	@EventListener
	public void handleVHostCreatedEvent(VHostCreatedEvent event) {
		
		logger.info("Detected new vhost {}", event.vhost);
		
		// Make sure this user (admin) has permissions on the vhost
		try {
			admin.addThisUserToVhost(event.vhost);
		}catch(Exception e) {
			logger.error("Failed to add THIS user to vhost {}. Reason:{}", event.vhost, e.getMessage());
		}
		
		JsonPolicy policy = null;
		try {
			policy = admin.find(event.vhost, plan.getName());
		}catch(Exception e) {
			// Create policy
			policy = plan.buildPolicyFor(event.vhost);
			
			logger.info("Creating Policy plan {} on new vhost {}", policy.getName(), event.vhost);
			try {
				admin.updatePolicy(policy);
			} catch (Exception e2) {
				logger.error("Failed to apply plan policy {} to vhost {}", policy.getName(), event.vhost);
			}			
			
		}
		
	}
	
	
	
	@Async
	public void applyOrCheckPlan() {
		JsonPolicy policyPlan = plan.buildPolicy();
		
		logger.info("applying or checkingn policy plan {}", policyPlan);
		List<JsonPolicy> policies = admin.listPolicies().stream().filter(p -> !ROOT_VHOST.equals(p.getVhost())).collect(Collectors.toList());
		Map<String, List<JsonPolicy>> policiesByVhost = policies.stream().collect(Collectors.groupingBy(JsonPolicy::getVhost));
		
		List<JsonVhost> vhosts = admin.listVhosts();
		
		// find vhosts without the plan policy
		List<JsonVhost> vhostsWithoutPlan = vhosts.stream().filter(vhost -> {
			if (ROOT_VHOST.equals(vhost.getName())) {
				return false;
			}
			List<JsonPolicy> lpolicies = policiesByVhost.get(vhost.getName());
			if (lpolicies == null) {
				return true;
			}
			return !lpolicies.stream().filter(p -> policyPlan.getName().equals(p.getName()) && ".*".equals(p.getPattern()) && plan.isCompliant(p)).findAny().isPresent();			
		}).collect(Collectors.toList());
		
		// apply policy plans
		logger.debug("Found {} vhosts without policy plan or not compliant", vhostsWithoutPlan.size());
		
		vhostsWithoutPlan.forEach(vhost -> {
			policyPlan.setVhost(vhost.getName());
			logger.debug("Found vhost {} without policy plan or not compliant", vhost.getName());
			try {
				admin.updatePolicy(policyPlan);
			}catch(Exception e) {
				logger.error("Failed to apply policy plan {} to vhost {} due to {}", policyPlan.getName(), policyPlan.getVhost(), e.getMessage());
			}
		});
		
		// make sure all other policies are compliant
		policies.stream().filter(p -> !plan.isCompliant(p)).forEach(p -> {
			try {
				logger.debug("Found policy {}|{} not compliant with policy plan {}", p.getVhost(), p.getName(), policyPlan.getName());
				admin.updatePolicy(plan.enforce(p));
			}catch(Exception e) {
				logger.error("Failed to enforce policy plan {} to vhost {} due to {}", policyPlan.getName(), policyPlan.getVhost(), e.getMessage());
			}
		});
	}

	
}


class PolicyChangedEvent {
	String vhost;
	String name;
	public PolicyChangedEvent(String vhost, String name) {
		super();
		this.vhost = vhost;
		this.name = name;
	}
	
}
class PolicyClearedEvent {
	String vhost;
	String name;
	public PolicyClearedEvent(String vhost, String name) {
		super();
		this.vhost = vhost;
		this.name = name;
	}
	
}
class VHostCreatedEvent {
	String vhost;
	public VHostCreatedEvent(String vhost) {
		super();
		this.vhost = vhost;
	}
	
}
