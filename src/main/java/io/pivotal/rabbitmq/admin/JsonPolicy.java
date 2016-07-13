package io.pivotal.rabbitmq.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class JsonPolicy {

	String name;
	JsonPolicyDefinition definition; // = new JsonPolicyDefinition();
	String vhost;
	String pattern;
	String applyTo;
	int priority;
	
	
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

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

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	@JsonProperty("apply-to")
	public String getApplyTo() {
		return applyTo;
	}

	public void setApplyTo(String applyTo) {
		this.applyTo = applyTo;
	}

	public boolean appliesToQueues() {
		return "queues".equals(applyTo) || "all".equals(applyTo);
	}
	
	public boolean hasEmptyDefinition() {
		return definition.isEmpty();
	}

	@Override
	public String toString() {
		return "JsonPolicy [name=" + name + ", definition=" + definition + ", vhost=" + vhost + ", pattern=" + pattern
				+ ", applyTo=" + applyTo + ", priority=" + priority + "]";
	}

	
	
}