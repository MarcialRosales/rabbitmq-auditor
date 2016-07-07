package io.pivotal.demo;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Configuration
@EnableConfigurationProperties(PlanProperties.class)
public class PlanConfiguration {
	
	@Autowired
	private PlanProperties plan;
	
	public String getName() {
		return plan.getName();
	}
	public PlanProperties getProperties() {
		return plan;
	}
	
	public JsonPolicy buildPolicyFor(String vhost) {
		JsonPolicy policy = buildPolicy();
		policy.setVhost(vhost);
		return policy;
	}
	public JsonPolicy buildPolicy() {
		JsonPolicy policy = new JsonPolicy();
		policy.setName(plan.getName());
		policy.setApplyTo("queues");
		policy.setPattern(".*");
		policy.setPriority(0);
		
		policy.setDefinition(new JsonPolicyDefinition());
		
		return enforce(policy);
	}

	public JsonPolicy enforce(JsonPolicy policy) {
		// enforce
		if (plan.getName().equals(policy.getName())) {
			policy.setPattern(".*");
			policy.setApplyTo("queues");
		}
		
		if (plan.getMaxQueueLengthBytes() > 0) {
			policy.getDefinition().setMaxLengthBytes(plan.getMaxQueueLengthBytes());
		}
		if (plan.getMaxQueueLength() > 0) {
			policy.getDefinition().setMaxLength(plan.getMaxQueueLength());
		}
		if (plan.getMaxMessageTtl() > 0) {
			policy.getDefinition().setMessageTtl(plan.getMaxMessageTtl());
		}
		if (plan.getQueueMasterLocator() != null) {
			policy.getDefinition().setQueueMasterLocator(plan.getQueueMasterLocator());
		}
		
		// check allowed policy parameters
		if (!plan.isAllowMirrorQueues() && policy.getDefinition().getHaMode() != null) {
			policy.getDefinition().clearHaMode();
		}else if (plan.isAllowMirrorQueues() && policy.getDefinition().getHaMode() != null && plan.getMaxSlaves() > 0) {
			
			switch(policy.getDefinition().getHaMode()) {
			case "exactly": 
				policy.getDefinition().setHaParams(plan.getMaxSlaves());
				break;
			case "nodes": // for nodes, ideally, we should allow them to select at most maxSlaves
			case "all":
				policy.getDefinition().setHaMode("exactly");
				policy.getDefinition().setHaParams(plan.getMaxSlaves());
				break;
			}
			
			if (plan.getHaSyncMode() != null) {
				policy.getDefinition().setHaSyncMode(plan.getHaSyncMode());
			}
			if (plan.getHaPromoteOnShutdown() != null) {
				policy.getDefinition().setHaPromoteOnShutdown(plan.getHaPromoteOnShutdown());
			}
			
		}
		return policy;
	}
	
	public boolean isCompliant(JsonPolicy policy) {
		if (plan.getName().equals(policy.getName()) && (!".*".equals(policy.getPattern()) || "exchanges".equals(policy.getApplyTo()))) {
			return false;
		}
		if (plan.getMaxQueueLengthBytes() > 0 && (policy.getDefinition().getMaxLengthBytes() == null || policy.getDefinition().getMaxLengthBytes() < plan.getMaxQueueLengthBytes())) {
			return false;
		}
		if (plan.getMaxQueueLength() > 0 && (policy.getDefinition().getMaxLength() == null || policy.getDefinition().getMaxLength() < plan.getMaxQueueLength())) {
			return false;
		}
		if (plan.getMaxMessageTtl() > 0 && (policy.getDefinition().getMessageTtl() == null || policy.getDefinition().getMessageTtl() < plan.getMaxMessageTtl())) {
			return false;
		}
		if (plan.getQueueMasterLocator() != null && !plan.getQueueMasterLocator().equals(policy.getDefinition().getQueueMasterLocator())) {
			return false;
		}
		
		if (!plan.isAllowMirrorQueues() && policy.getDefinition().getHaMode() != null) {
			return false;
		}
		if (plan.isAllowMirrorQueues()) {
			if (plan.getMaxSlaves() > 0 && "all".equals(policy.getDefinition().getHaMode())) {
				return false;
			}
			if (plan.getMaxSlaves() > 0 && "exactly".equals(policy.getDefinition().getHaMode()) && policy.getDefinition().getHaParams() != null
					&& policy.getDefinition().getHaParams() > plan.getMaxSlaves() ) {
				return false;
			}
			if (plan.getHaSyncMode() != null && !plan.getHaSyncMode().equals(policy.getDefinition().getHaSyncMode())) {
				return false;
			}
			if (plan.getHaPromoteOnShutdown() != null && !plan.getHaPromoteOnShutdown().equals(policy.getDefinition().getHaPromoteOnShutdown())) {
				return false;
			}
			
			
		}
		return true;
	}
	
	public boolean matches(JsonPolicy policy) {
		if (plan.getName().equals(policy.getName()) && (!".*".equals(policy.getPattern()) || "exchanges".equals(policy.getApplyTo()))) {
			return false;
		}
		if (plan.getMaxQueueLengthBytes() != (policy.getDefinition().getMaxLengthBytes() != null ? policy.getDefinition().getMaxLengthBytes() : 0)) {
			return false;
		}
		if (plan.getMaxQueueLength() != (policy.getDefinition().getMaxLength() != null ? policy.getDefinition().getMaxLength() : 0)) {
			return false;
		}
		if (plan.getMaxMessageTtl() != (policy.getDefinition().getMessageTtl() != null ? policy.getDefinition().getMessageTtl(): 0)) {
			return false;
		}
		if ((plan.getQueueMasterLocator() == null && policy.getDefinition().getQueueMasterLocator() != null) ||
				(plan.getQueueMasterLocator() != null && !plan.getQueueMasterLocator().equals(policy.getDefinition().getQueueMasterLocator()))) {
			return false;
		}

		if (!plan.isAllowMirrorQueues() && policy.getDefinition().getHaMode() != null) {
			return false;
		}
		
		return true;
	}
	
	
}
@ConfigurationProperties("plan")
@JsonInclude(Include.NON_NULL)
class PlanProperties {
	@NotBlank
	private String name;
	
	@Min(0)
	private long maxQueueLength;
	
	@Min(0)
	private long maxQueueLengthBytes;
	
	@Min(0)
	private long maxMessageTtl;
	
	private boolean allowMirrorQueues;
	
	@Min(0)
	private int maxSlaves;
	
	private String haSyncMode; 
	
	private String queueMasterLocator;
	
	private String haPromoteOnShutdown;
	
	private String enforceMode;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getMaxQueueLength() {
		return maxQueueLength;
	}
	public void setMaxQueueLength(long maxQueueLength) {
		this.maxQueueLength = maxQueueLength;
	}
	public long getMaxQueueLengthBytes() {
		return maxQueueLengthBytes;
	}
	public void setMaxQueueLengthBytes(long maxQueueLengthBytes) {
		this.maxQueueLengthBytes = maxQueueLengthBytes;
	}
	public long getMaxMessageTtl() {
		return maxMessageTtl;
	}
	public void setMaxMessageTtl(long maxMessageTtl) {
		this.maxMessageTtl = maxMessageTtl;
	}
	public boolean isAllowMirrorQueues() {
		return allowMirrorQueues;
	}
	public void setAllowMirrorQueues(boolean allowMirrorQueues) {
		this.allowMirrorQueues = allowMirrorQueues;
	}
	public int getMaxSlaves() {
		return maxSlaves;
	}
	public void setMaxSlaves(int maxSlaves) {
		this.maxSlaves = maxSlaves;
	}
	public String getHaSyncMode() {
		return haSyncMode;
	}
	public void setHaSyncMode(String haSyncMode) {
		this.haSyncMode = haSyncMode;
	}
	public String getQueueMasterLocator() {
		return queueMasterLocator;
	}
	public void setQueueMasterLocator(String queueMasterLocator) {
		this.queueMasterLocator = queueMasterLocator;
	}
	
	public String getHaPromoteOnShutdown() {
		return haPromoteOnShutdown;
	}
	public void setHaPromoteOnShutdown(String haPromoteOnShutdown) {
		this.haPromoteOnShutdown = haPromoteOnShutdown;
	}
	public String getEnforceMode() {
		return enforceMode;
	}
	public void setEnforceMode(String enforceMode) {
		this.enforceMode = enforceMode;
	}
			
}
