package io.pivotal.demo;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
class JsonPolicyDefinition {
	Map<String, Object> definition = new HashMap<>();
	
	public static final String HA_MODE = "ha-mode";
	public static final String HA_PARAMS = "ha-params";
	public static final String FEDERATION_UPSTREAM_SET = "federation-upstream-set";
	public static final String FEDERATION_UPSTREAM = "federation-upstream";
	public static final String MESSAGE_TTL = "message-ttl";
	public static final String DEAD_LETTER_EXCHANGE = "dead-letter-exchange";
	public static final String QUEUE_MODE = "queue_mode";
	public static final String ALTERNATE_EXCHANGE = "alternate-exchange";
	public static final String MAX_LENGTH = "max-length";
	public static final String MAX_LENGTH_BYTES = "max-length-bytes";
	public static final String HA_SYNC_MODE = "ha-sync-mode";
	public static final String EXPIRES = "expires";
	public static final String DEAD_LETTER_ROUTING_KEY = "dead-letter-routing-key";
	
	
	@JsonProperty(HA_MODE)
	public String getHaMode() {
		return (String)definition.get(HA_MODE);
	}

	public void setHaMode(String haMode) {
		this.definition.put(HA_MODE, haMode);
	}
	public void clearHaMode() {
		this.definition.remove(HA_MODE);
		this.definition.remove(HA_PARAMS);
		this.definition.remove(HA_SYNC_MODE);
	}

	@JsonProperty(HA_PARAMS)
	public Integer getHaParams() {
		return (Integer) definition.get(HA_PARAMS);
	}

	public void setHaParams(int haParams) {
		this.definition.put(HA_PARAMS, haParams);
	}
	

	@JsonProperty(FEDERATION_UPSTREAM_SET)
	public String getFederationUpstreamSet() {
		return (String)definition.get(FEDERATION_UPSTREAM_SET);
	}

	public void setFederationUpstreamSet(String federationUpstreamSet) {
		this.definition.put(FEDERATION_UPSTREAM_SET, federationUpstreamSet);
	}

	@JsonProperty(FEDERATION_UPSTREAM)
	public String getFederationUpstream() {
		return (String)definition.get(FEDERATION_UPSTREAM);
	}

	public void setFederationUpstream(String federationUpstream) {
		this.definition.put(FEDERATION_UPSTREAM, federationUpstream);
	}

	@JsonProperty(MESSAGE_TTL)
	public Long getMessageTtl() {
		return (Long) definition.get(MESSAGE_TTL);		
	}

	public void setMessageTtl(Long messageTtl) {
		this.definition.put(MESSAGE_TTL, messageTtl);
	}

	@JsonProperty(DEAD_LETTER_EXCHANGE)
	public String getDeadLetterExchange() {
		return (String)definition.get(DEAD_LETTER_EXCHANGE);
	}

	public void setDeadLetterExchange(String deadLetterExchange) {
		this.definition.put(DEAD_LETTER_EXCHANGE, deadLetterExchange);
	}
	
	@JsonProperty(DEAD_LETTER_ROUTING_KEY)
	public String getDeadLetterRoutingKey() {
		return (String)definition.get(DEAD_LETTER_ROUTING_KEY);
	}

	public void setDeadLetterRoutingKey(String deadletterRoutingKey) {
		this.definition.put(DEAD_LETTER_ROUTING_KEY, deadletterRoutingKey);
	}


	@JsonProperty(QUEUE_MODE)
	public String getQueueMode() {
		return (String)definition.get(QUEUE_MODE);
	}

	public void setQueueMode(String queueMode) {
		this.definition.put(QUEUE_MODE, queueMode);
	}

	@JsonProperty(ALTERNATE_EXCHANGE)
	public String getAlternateExchange() {
		return (String)definition.get(ALTERNATE_EXCHANGE);
	}

	@JsonProperty(ALTERNATE_EXCHANGE)
	public void setAlternateExchange(String alternateExchange) {
		this.definition.put(ALTERNATE_EXCHANGE, alternateExchange);
	}

	@JsonProperty(HA_SYNC_MODE)
	public String getHaSyncMode() {
		return (String)definition.get(HA_SYNC_MODE);
	}

	public void setHaSyncMode(String haSyncMode) {
		this.definition.put(HA_SYNC_MODE, haSyncMode);
	}

	@JsonProperty(EXPIRES)
	public Long getExpires() {
		return (Long) definition.get(EXPIRES);
	
	}

	public void setExpires(Long expires) {
		this.definition.put(EXPIRES, expires);
	}

	@JsonProperty(MAX_LENGTH_BYTES)
	public Long getMaxLengthBytes() {
		return (Long) definition.get(MAX_LENGTH_BYTES);
		
	}

	public void setMaxLengthBytes(Long maxLengthBytes) {
		this.definition.put(MAX_LENGTH_BYTES, maxLengthBytes);
	}

	@JsonProperty(MAX_LENGTH)
	public Long getMaxLength() {
		return (Long) definition.get(MAX_LENGTH);
		
	}

	public void setMaxLength(Long maxLength) {
		this.definition.put(MAX_LENGTH, maxLength);
	}
	
	@JsonIgnore
	public boolean isEmpty() {
		return definition.isEmpty();
	}
	
	public String toString() {
		return definition.toString();
	}
}
