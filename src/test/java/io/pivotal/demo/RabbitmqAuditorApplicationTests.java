package io.pivotal.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.pivotal.rabbitmq.RabbitmqAuditorApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RabbitmqAuditorApplication.class)
public class RabbitmqAuditorApplicationTests {

	@Test
	public void contextLoads() {
	}

}
