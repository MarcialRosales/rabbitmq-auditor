package io.pivotal.rabbitmq.admin;

import java.io.IOException;
import java.util.List;

import org.osgi.service.permissionadmin.PermissionInfo;

import com.rabbitmq.http.client.domain.NodeInfo;

public interface RabbitAdmin {
	List<JsonNode> listNodes();
	List<JsonChannel> listChannels();
	List<JsonConnection> listConnections();
	List<JsonVhost> listVhosts();
	List<JsonPolicy> listPolicies();
	List<JsonPermission> listPermissions(String user);
	List<JsonPolicy> listPolicies(String vhost);
	List<JsonQueue> listQueues();
	List<JsonQueue> listQueues(String vhost) throws IOException;
		
	int removeConfigureAndWritePermissions(String vhost);
	void deletePolicy(JsonPolicy policy) throws IOException;
	void deletePolicy(String vhost, String policy) throws IOException;
	void updatePolicy(JsonPolicy policy) throws IOException;
	JsonPolicy find(String vhost, String name) throws IOException;
	
	void addThisUserToVhost(String vhost) throws IOException;
	boolean isThisUserAdministrator() throws IOException;
	
	JsonUser findUser(String name) throws IOException;
	
}
