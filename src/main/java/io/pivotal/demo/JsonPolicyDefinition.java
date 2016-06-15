package io.pivotal.demo;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
class JsonPolicyDefinition {
	Map<String, Object> definition = new HashMap<>();
	
	@JsonProperty("ha-mode")
	public String getHaMode() {
		return (String)definition.get("haMode");
	}

	public void setHaMode(String haMode) {
		this.definition.put("haMode", haMode);
	}
	public void clearHaMode() {
		this.definition.remove("haMode");
		this.definition.remove("haParams");
		this.definition.remove("ha-sync-mode");
	}

	@JsonProperty("ha-params")
	public Integer getHaParams() {
		return (Integer) definition.get("haParams");
	}

	public void setHaParams(int haParams) {
		this.definition.put("haParams", haParams);
	}
	

	@JsonProperty("federation-upstream-set")
	public String getFederationUpstreamSet() {
		return (String)definition.get("federationUpstreamSet");
	}

	public void setFederationUpstreamSet(String federationUpstreamSet) {
		this.definition.put("federationUpstreamSet", federationUpstreamSet);
	}

	@JsonProperty("federation-upstream")
	public String getFederationUpstream() {
		return (String)definition.get("federationUpstream");
	}

	public void setFederationUpstream(String federationUpstream) {
		this.definition.put("federationUpstream", federationUpstream);
	}

	@JsonProperty("message-ttl")
	public Long getMessageTtl() {
		return (Long) definition.get("messageTtl");		
	}

	public void setMessageTtl(Long messageTtl) {
		this.definition.put("messageTtl", messageTtl);
	}

	@JsonProperty("dead-letter-exchange")
	public String getDeadLetterExchange() {
		return (String)definition.get("deadLetterExchange");
	}

	public void setDeadLetterExchange(String deadLetterExchange) {
		this.definition.put("deadLetterExchange", deadLetterExchange);
	}

	@JsonProperty("queue-mode")
	public String getQueueMode() {
		return (String)definition.get("queueMode");
	}

	public void setQueueMode(String queueMode) {
		this.definition.put("queueMode", queueMode);
	}

	@JsonProperty("alternate-exchange")
	public String getAlternateExchange() {
		return (String)definition.get("alternateExchange");
	}

	@JsonProperty("alternate_exchange")
	public void setAlternateExchange(String alternateExchange) {
		this.definition.put("alternateExchange", alternateExchange);
	}

	@JsonProperty("ha-sync-mode")
	public String getHaSyncMode() {
		return (String)definition.get("haSyncMode");
	}

	public void setHaSyncMode(String haSyncMode) {
		this.definition.put("haSyncMode", haSyncMode);
	}

	@JsonProperty("expires")
	public Long getExpires() {
		return (Long) definition.get("expires");
	
	}

	public void setExpires(Long expires) {
		this.definition.put("expires", expires);
	}

	@JsonProperty("max-length-bytes")
	public Long getMaxLengthBytes() {
		return (Long) definition.get("maxLengthBytes");
		
	}

	public void setMaxLengthBytes(Long maxLengthBytes) {
		this.definition.put("maxLengthBytes", maxLengthBytes);
	}

	@JsonProperty("max-length")
	public Long getMaxLength() {
		return (Long) definition.get("maxLength");
		
	}

	public void setMaxLength(Long maxLength) {
		this.definition.put("maxLength", maxLength);
	}
	boolean mergeTo(JsonPolicyDefinition policy) {
		return false;
	}
	
	public String toString() {
		return definition.toString();
	}
}