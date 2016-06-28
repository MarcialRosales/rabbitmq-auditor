package io.pivotal.demo;

import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("plan")
public class PlanConfiguration {
	
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
	

	public JsonPolicy buildPolicyFor(String vhost) {
		JsonPolicy policy = buildPolicy();
		policy.setVhost(vhost);
		return policy;
	}
	public JsonPolicy buildPolicy() {
		JsonPolicy policy = new JsonPolicy();
		policy.setName(name);
		policy.setApplyTo("queues");
		policy.setPattern(".*");
		policy.setPriority(0);
		
		policy.setDefinition(new JsonPolicyDefinition());
		
		return applyPlanToPolicy(policy);
	}
	public JsonPolicy applyPlanToPolicy(JsonPolicy policy) {
		if (maxMessageTtl > 0) policy.getDefinition().setMessageTtl(maxMessageTtl);
		if (maxQueueLength > 0) policy.getDefinition().setMaxLength(maxQueueLength);
		if (maxQueueLengthBytes > 0) policy.getDefinition().setMaxLengthBytes(maxQueueLengthBytes);
		
		return policy;
	}
	public JsonPolicy enforce(JsonPolicy policy) {
		// enforce
		if (name.equals(policy.getName())) {
			policy.setPattern(".*");
			policy.setApplyTo("queues");
		}
		
		if (maxQueueLengthBytes > 0) {
			policy.getDefinition().setMaxLengthBytes(maxQueueLengthBytes);
		}
		if (maxQueueLength > 0) {
			policy.getDefinition().setMaxLength(maxQueueLength);
		}
		if (maxMessageTtl > 0) {
			policy.getDefinition().setMessageTtl(maxMessageTtl);
		}
		if (queueMasterLocator != null) {
			policy.getDefinition().setQueueMasterLocator(queueMasterLocator);
		}
		
		// check allowed policy parameters
		if (!allowMirrorQueues && policy.getDefinition().getHaMode() != null) {
			policy.getDefinition().clearHaMode();
		}else if (allowMirrorQueues && policy.getDefinition().getHaMode() != null && maxSlaves > 0) {
			
			switch(policy.getDefinition().getHaMode()) {
			case "exactly": 
				policy.getDefinition().setHaParams(maxSlaves);
				break;
			case "nodes": // for nodes, ideally, we should allow them to select at most maxSlaves
			case "all":
				policy.getDefinition().setHaMode("exactly");
				policy.getDefinition().setHaParams(maxSlaves);
				break;
			}
			
			if (haSyncMode != null) {
				policy.getDefinition().setHaSyncMode(haSyncMode);
			}
			
		}
		return policy;
	}
	
	public boolean isCompliant(JsonPolicy policy) {
		if (name.equals(policy.getName()) && (!".*".equals(policy.getPattern()) || "exchanges".equals(policy.getApplyTo()))) {
			return false;
		}
		if (maxQueueLengthBytes > 0 && (policy.getDefinition().getMaxLengthBytes() == null || policy.getDefinition().getMaxLengthBytes() < maxQueueLengthBytes)) {
			return false;
		}
		if (maxQueueLength > 0 && (policy.getDefinition().getMaxLength() == null || policy.getDefinition().getMaxLength() < maxQueueLength)) {
			return false;
		}
		if (maxMessageTtl > 0 && (policy.getDefinition().getMessageTtl() == null || policy.getDefinition().getMessageTtl() < maxMessageTtl)) {
			return false;
		}
		if (queueMasterLocator != null && !queueMasterLocator.equals(policy.getDefinition().getQueueMasterLocator())) {
			return false;
		}
		
		if (!allowMirrorQueues && policy.getDefinition().getHaMode() != null) {
			return false;
		}
		if (allowMirrorQueues) {
			if (maxSlaves > 0 && "all".equals(policy.getDefinition().getHaMode())) {
				return false;
			}
			if (maxSlaves > 0 && "exactly".equals(policy.getDefinition().getHaMode()) && policy.getDefinition().getHaParams() != null
					&& policy.getDefinition().getHaParams() > maxSlaves ) {
				return false;
			}
			if (haSyncMode != null && !haSyncMode.equals(policy.getDefinition().getHaSyncMode())) {
				return false;
			}
			if (haPromoteOnShutdown != null && !haPromoteOnShutdown.equals(policy.getDefinition().getHaPromoteOnShutdown())) {
				return false;
			}
			
			
		}
		return true;
	}
	
	public boolean matches(JsonPolicy policy) {
		if (name.equals(policy.getName()) && (!".*".equals(policy.getPattern()) || "exchanges".equals(policy.getApplyTo()))) {
			return false;
		}
		if (maxQueueLengthBytes != (policy.getDefinition().getMaxLengthBytes() != null ? policy.getDefinition().getMaxLengthBytes() : 0)) {
			return false;
		}
		if (maxQueueLength != (policy.getDefinition().getMaxLength() != null ? policy.getDefinition().getMaxLength() : 0)) {
			return false;
		}
		if (maxMessageTtl != (policy.getDefinition().getMessageTtl() != null ? policy.getDefinition().getMessageTtl(): 0)) {
			return false;
		}
		if ((queueMasterLocator == null && policy.getDefinition().getQueueMasterLocator() != null) ||
				(queueMasterLocator != null && !queueMasterLocator.equals(policy.getDefinition().getQueueMasterLocator()))) {
			return false;
		}

		if (!allowMirrorQueues && policy.getDefinition().getHaMode() != null) {
			return false;
		}
		
		return true;
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
	
}
