package io.pivotal.demo;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

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
@Profile("PolicyEnforcement")
public class PolicyEnforceService {

	private Logger logger = LoggerFactory.getLogger(PolicyEnforceService.class);
	
	private RabbitAdmin admin;
	private PlanConfiguration plan;
	
	public static final String ROOT_VHOST = "/";
	
	
	@Autowired
	public PolicyEnforceService(RabbitAdmin admin, PlanConfiguration plan) {
		super();
		this.admin = admin;
		this.plan = plan;
	}
	
	public PlanProperties getCurrentPlan() {
		return  plan.getProperties();
	}
	public JsonPolicy getCurrentPolicyPlan() {
		return  plan.buildPolicy();
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
	public void overridePolicyIfNotCompliant(String vhost, String name) {
		
		JsonPolicy policy = null;
		try {
			policy = admin.find(vhost, name);
		}catch(Exception e) {
			logger.error("Failed to load policy", e);
			return;
		}

		if (!plan.isCompliant(policy)) {
			logger.info("Policy {} not compliant on vhost {}. Overriding..", name, vhost);
			try {
				enforcePolicy(policy);
			}catch(Exception e) {
				logger.error("Failed to update policy", e);
			}
		}
	}
	
	
	/** 
	 * Make sure that at least the policy plan exists on the new vhost. 
	 * @param event
	 */
	public void ensurePolicyPlanExistsOnVhost(String vhost) {
		
		// Make sure this user (admin) has permissions on the vhost
		try {
			admin.addThisUserToVhost(vhost);
		}catch(Exception e) {
			logger.error("Failed to add THIS user to vhost {}. Reason:{}", vhost, e.getMessage());
		}
		
		JsonPolicy policy = null;
		try {
			policy = admin.find(vhost, plan.getName());
		}catch(Exception e) {
			// Create policy
			policy = plan.buildPolicyFor(vhost);
			
			logger.info("Creating Policy plan {} on new vhost {}", policy.getName(), vhost);
			try {
				admin.updatePolicy(policy);
			} catch (Exception e2) {
				logger.error("Failed to apply plan policy {} to vhost {}", policy.getName(), vhost);
			}			
			
		}
		
	}
	
	
	
	public PlanComplianceReport findUncompliantVHosts() {
		JsonPolicy policyPlan = plan.buildPolicy();
		PlanComplianceReport report = new PlanComplianceReport();
		
		logger.info("Checking policy plan {}", policyPlan);
		
		List<JsonPolicy> policies = admin.listPolicies().stream().filter(p -> !ROOT_VHOST.equals(p.getVhost())).collect(Collectors.toList());
		Map<String, List<JsonPolicy>> policiesByVhost = policies.stream().collect(Collectors.groupingBy(JsonPolicy::getVhost));
		
		List<JsonVhost> vhosts = admin.listVhosts();
		report.vHostCount = vhosts.size();
		
		// find vhosts without the plan policy
		vhosts.stream().filter(vhost -> {
			if (ROOT_VHOST.equals(vhost.getName())) {
				return false;
			}
			List<JsonPolicy> lpolicies = policiesByVhost.get(vhost.getName());
			if (lpolicies == null) {
				return true;
			}
			return !lpolicies.stream().filter(p -> policyPlan.getName().equals(p.getName()) && ".*".equals(p.getPattern()) && plan.isCompliant(p)).findAny().isPresent();			
		}).forEach(vhost -> report.withoutPlan(vhost.getName()));
				
		// make sure all other policies are compliant
		policies.stream().filter(p -> !plan.isCompliant(p)).forEach(p -> report.uncompliant(p.vhost));
		
		return report;
	}
	
	public EnforcedPlanReport enforcePlan(@RequestParam String vhost) {
		JsonPolicy policyPlan = plan.buildPolicy();
		policyPlan.setVhost(vhost);
		try {
			enforcePolicy(policyPlan);
			return new EnforcedPlanReport(vhost);
		} catch (IOException e) {
			throw new RuntimeException("Unable to enforce policy", e);
		}
	}
	
	public void deletePlan() {
		admin.listPolicies().stream().filter(p -> p.name.equals(plan.getName())).forEach(p -> {
			try {
				admin.addThisUserToVhost(p.vhost);
				admin.deletePolicy(p);
			} catch (Exception e) {
				logger.error("Failed to remove policy plan {} from vhost {} due to {}", plan.getName(), p.getVhost(), e.getMessage());
			}
		});	
	}
	public void deletePlan(String vhost) {
		try {
			admin.addThisUserToVhost(vhost);
			admin.deletePolicy(vhost, plan.getName());
		} catch (Exception e) {
			logger.error("Failed to remove policy plan {} from vhost {} due to {}", plan.getName(), vhost, e.getMessage());
		}
	}
	
	public EnforcedPlanReport enforcePlan() {
		JsonPolicy policyPlan = plan.buildPolicy();
		EnforcedPlanReport report = new EnforcedPlanReport();
		
		logger.info("Applying or checking policy plan {}", policyPlan);
		List<JsonPolicy> policies = admin.listPolicies().stream().filter(p -> !ROOT_VHOST.equals(p.getVhost())).collect(Collectors.toList());
		Map<String, List<JsonPolicy>> policiesByVhost = policies.stream().collect(Collectors.groupingBy(JsonPolicy::getVhost));
		
		List<JsonVhost> vhosts = admin.listVhosts();
		logger.debug("Found {} vhosts", vhosts.size());
		
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
		logger.debug("Found {} vhosts without policy plan or not compliant out of {} vhosts", vhostsWithoutPlan.size(), vhosts.size());
		
		vhostsWithoutPlan.forEach(vhost -> {
			policyPlan.setVhost(vhost.getName());
			logger.trace("Found vhost {} without policy plan or not compliant", vhost.getName());
			try {
				enforcePolicy(policyPlan);
				report.enforced(vhost.getName());
			}catch(Exception e) {
				logger.error("Failed to apply policy plan {} to vhost {} due to {}", policyPlan.getName(), policyPlan.getVhost(), e.getMessage());
			}
		});
		
		// make sure all other policies are compliant
		policies.stream().filter(p -> !plan.isCompliant(p)).forEach(p -> {
			try {
				logger.trace("Found policy {}|{} not compliant with policy plan {}", p.getVhost(), p.getName(), policyPlan.getName());
				enforcePolicy(p);
				report.enforced(p.getVhost());
			}catch(Exception e) {
				logger.error("Failed to enforce policy plan {} to vhost {} due to {}", policyPlan.getName(), policyPlan.getVhost(), e.getMessage());
			}
		});
		
		return report;
	}

	private void enforcePolicy(JsonPolicy policy) throws IOException {
		plan.enforce(policy);
		
		// ensure this user is a user of the vhost so that we can operate on the vhost
		admin.addThisUserToVhost(policy.vhost);
		
		if (policy.hasEmptyDefinition()) {
			admin.deletePolicy(policy);
		}else {
			admin.updatePolicy(policy);
		}
	}
}

class PlanComplianceReport {
	int vHostCount;
	Set<String> vHostsWithoutPlan = new HashSet<>();
	Set<String> uncompliantVhosts = new HashSet<>();
	
	
	public int getvHostCount() {
		return vHostCount;
	}
	public int getvHostsWithoutPlanCount() {
		return vHostsWithoutPlan.size();
	}
	public int getUncompliantVhostsCount() {
		return uncompliantVhosts.size();
	}
	public Set<String> getvHostsWithoutPlan() {
		return vHostsWithoutPlan;
	}
	public Set<String> getUncompliantVhosts() {
		return uncompliantVhosts;
	}
	void uncompliant(String vhost) {
		uncompliantVhosts.add(vhost);
	}
	void withoutPlan(String vhost) {
		vHostsWithoutPlan.add(vhost);
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


@Component
@Profile("PolicyEnforcement")
@ConditionalOnProperty(name = "plan.enforce-mode", havingValue = "automatic")
class AutomaticPolicyEnforcer implements CommandLineRunner {
	
	private Logger logger = LoggerFactory.getLogger(AutomaticPolicyEnforcer.class);
	
	static final String auditPolicyQueue = "policyEnforcer";
	
	private PolicyEnforceService policyEnforcer;
	private RabbitProperties rabbit;
	private NotEraseableUsernamePasswordAuthenticationToken adminToken;
	
	@Autowired
	public AutomaticPolicyEnforcer(PolicyEnforceService policyEnforcer, RabbitProperties rabbit) {
		this.policyEnforcer = policyEnforcer;
		this.rabbit = rabbit;
	}
	
	@PostConstruct
	void init() {
		logger.info("AutomaticPolicyEnforcer started");
		adminToken = new NotEraseableUsernamePasswordAuthenticationToken(rabbit.getUsername(), rabbit.getPassword());
	}
	
	@Override
	public void run(String... args) throws Exception {
		try {
			SecurityContextHolder.getContext().setAuthentication(adminToken);
			logger.info("Enforcing plan across all vhosts ..." );
			policyEnforcer.enforcePlan();
		}finally {
			logger.info("Terminated enforcing plan across all vhosts" );
			SecurityContextHolder.getContext().setAuthentication(null);
		}
	}
	
	@Bean
	public List<Declarable> prepareListener() {
		logger.info("Live monitoring of policy changes and vhost creations thru Rabbit Queue {}", auditPolicyQueue);

		return Arrays.<Declarable> asList(new Queue(auditPolicyQueue, false, false, true),
			new Binding(auditPolicyQueue, DestinationType.QUEUE, "amq.rabbitmq.event", "policy.#", null),
			new Binding(auditPolicyQueue, DestinationType.QUEUE, "amq.rabbitmq.event", "vhost.created", null));

	}

//	@Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
		
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setQueueNames(auditPolicyQueue);
        endpoint.setMessageListener(message -> {
        	try {
            	SecurityContextHolder.getContext().setAuthentication(adminToken);
        		processPolicyEvent(message);
        		
        	}finally {
        		SecurityContextHolder.getContext().setAuthentication(null);
        	}
        });
        registrar.registerEndpoint(endpoint);
    }
    

    @RabbitListener(queues = {auditPolicyQueue })
	private void processPolicyEvent(Message m) {
		String name = (String) m.getMessageProperties().getHeaders().get("name");
		String vhost = (String) m.getMessageProperties().getHeaders().get("vhost");
		
		if (PolicyEnforceService.ROOT_VHOST.equals(vhost)) {
			return; // we don't handle events that occur in the root as only administrators are allowed to operate on that vhost
		}
			
		try {
			SecurityContextHolder.getContext().setAuthentication(adminToken);
			switch (m.getMessageProperties().getReceivedRoutingKey()) {
			case "policy.set":
				handlePolicyChangedEvent(vhost, name);
				break;
			case "policy.cleared":
				handlePolicyClearedEvent(vhost, name);
				break;
			case "vhost.created":
				handleVHostCreatedEvent(vhost);
				break;
			}
		}finally {
			SecurityContextHolder.getContext().setAuthentication(null);
		}
	}
	
	public void handleVHostCreatedEvent(String vhost) {
		logger.debug("Detected new vhost {}", vhost);
		
		policyEnforcer.ensurePolicyPlanExistsOnVhost(vhost);
	}
	public void handlePolicyClearedEvent(String vhost, String name) {
		
		if (policyEnforcer.getCurrentPlan().getName().equals(name)) {
			logger.debug("Policy plan {} cleared on vhost {}",name, vhost);
			try {
				policyEnforcer.ensurePolicyPlanExistsOnVhost(vhost);
			} catch (Exception e) {
				logger.error("Failed to apply plan policy {} to vhost {}", name, vhost);
			}
		}
	}

	public void handlePolicyChangedEvent(String vhost, String name) {
		logger.debug("Modified policy {} on vhost {}", name, vhost);
		policyEnforcer.overridePolicyIfNotCompliant(vhost, name);
	}
}

@RestController
class PolicyEnforcerRestEndpoint {
	
	@Autowired
	PolicyEnforceService policyEnforcer;
	
	
	@RequestMapping("plan/uncompliantVhosts")
	public PlanComplianceReport findUncompliantVHosts() {
		return policyEnforcer.findUncompliantVHosts();
	}
	
	@RequestMapping(value = "plan/{vhost}/enforce", method = RequestMethod.POST)
	public EnforcedPlanReport enforcePlan(@PathVariable String vhost) {
		return policyEnforcer.enforcePlan(vhost);
	}
	
	@RequestMapping(value = "plan/enforce", method = RequestMethod.POST)
	public Callable<EnforcedPlanReport> enforcePlan() {
		return () -> { return asyncEnforcePlan().get(); };
	}

	private Future<EnforcedPlanReport> asyncEnforcePlan() {
		return new AsyncResult<EnforcedPlanReport>(policyEnforcer.enforcePlan());
	}
	@RequestMapping(value = "plan")
	public PlanProperties getCurrentPlan() {
		return policyEnforcer.getCurrentPlan();
	}
	@RequestMapping(value = "plan/policy")
	public JsonPolicy getCurrentPolicy() {
		return policyEnforcer.getCurrentPolicyPlan();
	}
	
	@RequestMapping(value = "plan", method = RequestMethod.DELETE)
	public void delete() {
		policyEnforcer.deletePlan();
	}

	@RequestMapping(value = "plan/{vhost}", method = RequestMethod.DELETE)
	public void delete(@PathVariable String vhost) {
		policyEnforcer.deletePlan(vhost);
	}
	
	@ExceptionHandler({HttpClientErrorException.class})
	public void handleHttpErrors(HttpServletResponse response, HttpClientErrorException e) throws IOException {
		response.sendError(e.getStatusCode().value(), e.getMessage());	
	}
	
	
}
@Component
class AuthenticationManagerImpl implements AuthenticationManager {

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		
		return new NotEraseableUsernamePasswordAuthenticationToken(authentication);
	}
	
}
class NotEraseableUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotEraseableUsernamePasswordAuthenticationToken(Authentication auth) {
		super(auth.getPrincipal(), auth.getCredentials(), Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
		
	}
	public NotEraseableUsernamePasswordAuthenticationToken(String username, String credentials) {
		super(username, credentials, Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
	}
	@Override
	public void eraseCredentials() {
		
	}

}
class EnforcedPlanReport {
	Set<String> vhosts = new HashSet<>();
	
	EnforcedPlanReport() {
		
	}
	EnforcedPlanReport(String vhost){
		vhosts.add(vhost);
	}
	
	void enforced(String vhost) {
		vhosts.add(vhost);
	}

	public Set<String> getVhosts() {
		return vhosts;
	}
	
	public int getVhostsCount() {
		return vhosts.size();
	}
	
}

