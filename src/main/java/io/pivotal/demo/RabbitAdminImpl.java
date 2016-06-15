package io.pivotal.demo;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.rabbitmq.http.client.domain.UserPermissions;

@Component
public class RabbitAdminImpl implements RabbitAdmin {

	private RestTemplate restTemplate;
	private HttpHeaders headers;
	private HttpHeaders putHeaders;
	
	@Value("${spring.rabbitmq.admin}")
	String adminUrl;

	@Autowired 
	RabbitProperties rabbit;

	
	@PostConstruct
	void initHeaders() {
		restTemplate = new RestTemplate();
		
		headers = getHttpHeaders();
		putHeaders = getPUTHttpHeaders();
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
		String plainCreds = rabbit.getUsername() + ":" + rabbit.getPassword();
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		return base64Creds;
	}

	@Override
	public List<JsonChannel> listChannels() {
		
		HttpEntity<String> request = new HttpEntity<String>(headers);
		URI uri = buildURI("/api/channels?columns=vhost,message_stats.publish").build().toUri();
		
		ResponseEntity<List<JsonChannel>> connectionResponse = restTemplate.exchange(
				uri, HttpMethod.GET, request,
				new ParameterizedTypeReference<List<JsonChannel>>() {
				});
		List<JsonChannel> channels = connectionResponse.getBody();
		return channels;
	}
	@Override
	public List<JsonVhost> listVhosts() {
		
		HttpEntity<String> request = new HttpEntity<String>(headers);
		URI uri = buildURI("/api/vhosts").query("columns=name,message_stats.publish").build().toUri();
		
		ResponseEntity<List<JsonVhost>> connectionResponse = restTemplate.exchange(
				uri, HttpMethod.GET, request,
				new ParameterizedTypeReference<List<JsonVhost>>() {
				});
		List<JsonVhost> vhosts = connectionResponse.getBody();
		return vhosts;
	}
	@Override
	public List<JsonPolicy> listPolicies() {
		
		HttpEntity<String> request = new HttpEntity<String>(headers);
		URI uri = buildURI("/api/policies").build().toUri();

		ResponseEntity<List<JsonPolicy>> connectionResponse = restTemplate.exchange(uri
				, HttpMethod.GET, request,
				new ParameterizedTypeReference<List<JsonPolicy>>() {
				});
		List<JsonPolicy> policies = connectionResponse.getBody();
		return policies;
	}

	@Override
	public int removeConfigureAndWritePermissions(String vhost) {
		HttpEntity<String> request = new HttpEntity<String>(headers);
		ResponseEntity<List<UserPermissions>> users = restTemplate.exchange(
				"http://" + adminUrl + "api/vhosts/" + vhost + "/permissions", HttpMethod.GET, request, 
				new ParameterizedTypeReference<List<UserPermissions>>() {
				});
		users.getBody().stream().forEach(c -> removeUserFromVhost(c.getVhost(), c.getUser()));
		
		return 0;
	}
	
	private void removeUserFromVhost(String vhost, String user) {
		HttpEntity<String> request = new HttpEntity<String>(headers);
		URI uri = buildURI("/api/vhosts").pathSegment(vhost).path("/permissions").build(true).toUri();
		
		restTemplate.postForEntity(uri, request, UserPermissions.class); 				
	}

	@Override
	public void updatePolicy(JsonPolicy policy) throws IOException {
		String vhost = UriUtils.encodePathSegment(policy.getVhost(), "utf-8");
		String name = UriUtils.encodePathSegment(policy.getName(), "utf-8");
		
		URI uri = buildURI("/api/policies").pathSegment(vhost, name).build(true).toUri();
		
		HttpEntity<JsonPolicy> entity = new HttpEntity<>(policy, putHeaders);
		restTemplate.put(uri, entity);
	}

	@Override
	public void deletePolicy(JsonPolicy policy) throws IOException {
		HttpEntity<String> request = new HttpEntity<String>(headers);
		
		String vhost = UriUtils.encodePathSegment(policy.getVhost(), "utf-8");
		String name = UriUtils.encodePathSegment(policy.getName(), "utf-8");
		
		URI uri = buildURI("/api/policies").pathSegment(vhost, name).build(true).toUri();
		
		restTemplate.exchange(uri, HttpMethod.DELETE, request, String.class);
	}
	
	@Override
	public JsonPolicy find(String vhost, String name) throws IOException {
		HttpEntity<String> request = new HttpEntity<String>(headers);
		
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
		
		URI uri = buildURI("/api/permissions").pathSegment(vhost, rabbit.getUsername()).build(true).toUri();
	
		JsonPermission permission = new JsonPermission(".*", "", "");
		
		HttpEntity<JsonPermission> entity = new HttpEntity<>(permission, putHeaders);
		restTemplate.put(uri, entity);
		
	}

	@Override
	public JsonUser findUser(String name) throws IOException {
	HttpEntity<String> request = new HttpEntity<String>(headers);
		
		name = UriUtils.encodePathSegment(name, "utf-8");
		
		URI uri = buildURI("/api/users").pathSegment(name).build(true).toUri();
		
		ResponseEntity<JsonUser> policyResponse = restTemplate.exchange(uri, HttpMethod.GET, request, JsonUser.class);
		return policyResponse.getBody();
	}

	@Override
	public boolean isThisUserAdministrator() throws IOException {
		return findUser(rabbit.getUsername()).isAdministrator();
	}
	
	
}
