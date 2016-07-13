package io.pivotal.rabbitmq.policy;

import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class NotEraseableUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotEraseableUsernamePasswordAuthenticationToken(Authentication auth) {
		super(auth.getPrincipal(), auth.getCredentials(), Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));

	}

	public NotEraseableUsernamePasswordAuthenticationToken(String username, String credentials) {
		super(username, credentials, Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Override
	public void eraseCredentials() {

	}

}