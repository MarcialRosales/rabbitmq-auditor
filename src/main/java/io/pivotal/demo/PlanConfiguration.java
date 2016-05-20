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
	private long maxMessageTTL;
	
	private boolean allowMirrorQueues;
	
	@Min(0)
	private int maxSlaves;
	
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
	public long getMaxMessageTTL() {
		return maxMessageTTL;
	}
	public void setMaxMessageTTL(long maxMessageTTL) {
		this.maxMessageTTL = maxMessageTTL;
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
		if (maxMessageTTL > 0) policy.getDefinition().setMessageTtl(maxMessageTTL);
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
		if (maxMessageTTL > 0) {
			policy.getDefinition().setMessageTtl(maxMessageTTL);
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
		}
		return policy;
	}
	
	public boolean isCompliant(JsonPolicy policy) {
		if (name.equals(policy.getName()) && (!".*".equals(policy.getPattern()) || !"queues".equals(policy.getApplyTo()))) {
			return false;
		}
		if (maxQueueLength > 0 && (policy.getDefinition().getMaxLengthBytes() == null || policy.getDefinition().getMaxLengthBytes() < maxQueueLengthBytes)) {
			return false;
		}
		if (maxQueueLength > 0 && (policy.getDefinition().getMaxLength() == null || policy.getDefinition().getMaxLength() < maxQueueLength)) {
			return false;
		}
		if (maxMessageTTL > 0 && (policy.getDefinition().getMessageTtl() == null || policy.getDefinition().getMessageTtl() < maxMessageTTL)) {
			return false;
		}
		if (!allowMirrorQueues && policy.getDefinition().getHaMode() != null) {
			return false;
		}
		if (allowMirrorQueues && maxSlaves > 0 && "all".equals(policy.getDefinition().getHaMode())) {
			return false;
		}
		if (allowMirrorQueues && maxSlaves > 0 && "exactly".equals(policy.getDefinition().getHaMode()) && policy.getDefinition().getHaParams() != null
				&& policy.getDefinition().getHaParams() > maxSlaves ) {
			return false;
		}
		return true;
	}
	
}
