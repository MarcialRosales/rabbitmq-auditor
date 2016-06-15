package io.pivotal.demo;

import java.io.IOException;
import java.util.List;

public interface RabbitAdmin {
	List<JsonChannel> listChannels();
	List<JsonVhost> listVhosts();
	List<JsonPolicy> listPolicies();
	int removeConfigureAndWritePermissions(String vhost);
	void deletePolicy(JsonPolicy policy) throws IOException;
	void updatePolicy(JsonPolicy policy) throws IOException;
	JsonPolicy find(String vhost, String name) throws IOException;
	
	void addThisUserToVhost(String vhost) throws IOException;
	boolean isThisUserAdministrator() throws IOException;
	
	JsonUser findUser(String name) throws IOException;
	
}