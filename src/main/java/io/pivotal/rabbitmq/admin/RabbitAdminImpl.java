package io.pivotal.rabbitmq.admin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.rabbitmq.http.client.domain.NodeInfo;
import com.rabbitmq.http.client.domain.UserPermissions;

@Service
public class RabbitAdminImpl implements RabbitAdmin {

	private RestTemplate restTemplate;
//	private HttpHeaders putHeaders;
//	
	@Value("${spring.rabbitmq.admin}")
	private String adminUrl;

//	private ThreadLocal<HttpHeaders> headers = new ThreadLocal<>();
	
	public RabbitAdminImpl() {
		restTemplate = new RestTemplate();
		
	}
		
	
	protected HttpHeaders getHttpHeaders() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		requestHeaders.set("Authorization", "Basic " + getHttpCredentials());
		return requestHeaders;
	}
	protected HttpHeaders getPUTHttpHeaders() {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Authorization", "Basic " + getHttpCredentials());
		requestHeaders.setContentType(MediaType.APPLICATION_JSON);
		return requestHeaders;
	}

	protected String getHttpCredentials() {
		SecurityContext ctx = SecurityContextHolder.getContext();
		Authentication auth = ctx.getAuthentication();
		
		String plainCreds = auth.getPrincipal() + ":" + auth.getCredentials();
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		return base64Creds;
	}
	
	protected String getCurrentUser() {
		SecurityContext ctx = SecurityContextHolder.getContext();
		Authentication auth = ctx.getAuthentication();
		return String.valueOf(auth.getPrincipal());
	}

	
	private ParameterizedTypeReference<List<JsonNode>> NodeInfoList = new ParameterizedTypeReference<List<JsonNode>>() {};

	@Override
	public List<JsonNode> listNodes() {
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		URI uri = buildURI("/api/nodes").query("columns=name,run_queue,mem_alarm,disk_free_alarm,partitions,msg_store_read_count,msg_store_write_count,io_reopen_count,running,cluster_links").build().toUri();
		
		ResponseEntity<List<JsonNode>> connectionResponse = restTemplate.exchange(
				uri, HttpMethod.GET, request, NodeInfoList);
		List<JsonNode> channels = connectionResponse.getBody();
		return channels;
	}

	private ParameterizedTypeReference<List<JsonChannel>> JsonChannelList = new ParameterizedTypeReference<List<JsonChannel>>() {};

	@Override
	public List<JsonChannel> listChannels() {
		
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		URI uri = buildURI("/api/channels?columns=vhost,message_stats.publish").build().toUri();
		
		ResponseEntity<List<JsonChannel>> connectionResponse = restTemplate.exchange(
				uri, HttpMethod.GET, request, JsonChannelList);
		List<JsonChannel> channels = connectionResponse.getBody();
		return channels;
	}
	private ParameterizedTypeReference<List<JsonVhost>> JsonVhostList = new ParameterizedTypeReference<List<JsonVhost>>() {};

	@Override
	public List<JsonVhost> listVhosts() {
		
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		URI uri = buildURI("/api/vhosts").query("columns=name,message_stats.publish").build().toUri();
		
		ResponseEntity<List<JsonVhost>> connectionResponse = restTemplate.exchange(
				uri, HttpMethod.GET, request, JsonVhostList);
		List<JsonVhost> vhosts = connectionResponse.getBody();
		return vhosts;
	}
	@Override
	public List<JsonConnection> listConnections() {
		
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		URI uri = buildURI("/api/connections").query("columns=vhost,user,channels,node").build().toUri();
		
		ResponseEntity<List<JsonConnection>> connectionResponse = restTemplate.exchange(
				uri, HttpMethod.GET, request,
				new ParameterizedTypeReference<List<JsonConnection>>() {
				});
		List<JsonConnection> connections = connectionResponse.getBody();
		return connections;
	}
	private ParameterizedTypeReference<List<JsonPolicy>> jsonPolicyList = new ParameterizedTypeReference<List<JsonPolicy>>() {};

	@Override
	public List<JsonPolicy> listPolicies() {
		
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		URI uri = buildURI("/api/policies").build().toUri();

		ResponseEntity<List<JsonPolicy>> connectionResponse = restTemplate.exchange(uri
				, HttpMethod.GET, request, jsonPolicyList);
		List<JsonPolicy> policies = connectionResponse.getBody();
		return policies;
	}
	private ParameterizedTypeReference<List<JsonQueue>> jsonQueueList = new ParameterizedTypeReference<List<JsonQueue>>() {};
	@Override
	public List<JsonQueue> listQueues() {
		
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		URI uri = buildURI("/api/queues").build().toUri();

		ResponseEntity<List<JsonQueue>> connectionResponse = restTemplate.exchange(uri
				, HttpMethod.GET, request, jsonQueueList);
		List<JsonQueue> queues = connectionResponse.getBody();
		return queues;
	}
	@Override
	public List<JsonQueue> listQueues(String vhost) throws IOException {
		vhost = UriUtils.encodePathSegment(vhost, "utf-8");

		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		URI uri = buildURI("/api/queues").pathSegment(vhost).build(true).toUri();

		ResponseEntity<List<JsonQueue>> connectionResponse = restTemplate.exchange(uri
				, HttpMethod.GET, request, jsonQueueList);
		List<JsonQueue> queues = connectionResponse.getBody();
		return queues;
	}

	@Override
	public int removeConfigureAndWritePermissions(String vhost) {
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		ResponseEntity<List<UserPermissions>> users = restTemplate.exchange(
				"http://" + adminUrl + "api/vhosts/" + vhost + "/permissions", HttpMethod.GET, request, 
				new ParameterizedTypeReference<List<UserPermissions>>() {
				});
		users.getBody().stream().forEach(c -> removeUserFromVhost(c.getVhost(), c.getUser()));
		
		return 0;
	}
	
	private void removeUserFromVhost(String vhost, String user) {
		
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		URI uri = buildURI("/api/vhosts").pathSegment(vhost).path("/permissions").build(true).toUri();
		
		restTemplate.postForEntity(uri, request, UserPermissions.class); 				
	}

	@Override
	public void updatePolicy(JsonPolicy policy) throws IOException {
		String vhost = UriUtils.encodePathSegment(policy.getVhost(), "utf-8");
		String name = UriUtils.encodePathSegment(policy.getName(), "utf-8");
		
		URI uri = buildURI("/api/policies").pathSegment(vhost, name).build(true).toUri();
		
		HttpEntity<JsonPolicy> entity = new HttpEntity<>(policy, getPUTHttpHeaders());
		restTemplate.put(uri, entity);
	}

	@Override
	public void deletePolicy(JsonPolicy policy) throws IOException {
		deletePolicy(policy.vhost, policy.name);
	}
	
	@Override
	public void deletePolicy(String vhost, String name) throws IOException {
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		
		vhost = UriUtils.encodePathSegment(vhost, "utf-8");
		name = UriUtils.encodePathSegment(name, "utf-8");
		
		URI uri = buildURI("/api/policies").pathSegment(vhost, name).build(true).toUri();
		
		restTemplate.exchange(uri, HttpMethod.DELETE, request, String.class);
	}
	
	@Override
	public JsonPolicy find(String vhost, String name) throws IOException {
		HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		
		vhost = UriUtils.encodePathSegment(vhost, "utf-8");
		name = UriUtils.encodePathSegment(name, "utf-8");
		
		URI uri = buildURI("/api/policies").pathSegment(vhost, name).build(true).toUri();
		
		ResponseEntity<JsonPolicy> policyResponse = restTemplate.exchange(uri, HttpMethod.GET, request, JsonPolicy.class);
		return policyResponse.getBody();
	}

	private UriComponentsBuilder buildURI(String path) {
		return UriComponentsBuilder.fromHttpUrl(adminUrl).path(path);
	}

	
	@Override
	public void addThisUserToVhost(String vhost) throws IOException {
		vhost = UriUtils.encodePathSegment(vhost, "utf-8");
		
		URI uri = buildURI("/api/permissions").pathSegment(vhost, getCurrentUser()).build(true).toUri();
	
		JsonPermission permission = new JsonPermission(".*", "", "");
		
		HttpEntity<JsonPermission> entity = new HttpEntity<>(permission, getPUTHttpHeaders());
		restTemplate.put(uri, entity);
		
	}

	@Override
	public JsonUser findUser(String name) throws IOException {
	HttpEntity<String> request = new HttpEntity<String>(getHttpHeaders());
		
		name = UriUtils.encodePathSegment(name, "utf-8");
		
		URI uri = buildURI("/api/users").pathSegment(name).build(true).toUri();
		
		ResponseEntity<JsonUser> policyResponse = restTemplate.exchange(uri, HttpMethod.GET, request, JsonUser.class);
		return policyResponse.getBody();
	}

	@Override
	public boolean isThisUserAdministrator() throws IOException {
		return findUser(getCurrentUser()).isAdministrator();
	}
	
	
}
