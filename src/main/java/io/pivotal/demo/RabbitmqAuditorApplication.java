package io.pivotal.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * User Story: Rate limit
 * Every N seconds, it gets the list of opened channels and reads the number of messages sent by each vhost. For each vhost, it 
 * reads from in-memory store, the last stat and calcutime when it last read the channel stats,  
 * 
 * 
 * It should read from Redis the number of messages  connections today. If there are none, it should get the full list of connections and determine the count of opened
 * connections per vhost and store that number in redis.
 * Then it should listen for connection.created and connection.closed events and increment/decrement the number of connections in redis.
 * 
 * 
 * 
 * @author mrosales
 *
 */
@SpringBootApplication
@EnableAsync
public class RabbitmqAuditorApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(RabbitmqAuditorApplication.class, args);
		
	}
}
