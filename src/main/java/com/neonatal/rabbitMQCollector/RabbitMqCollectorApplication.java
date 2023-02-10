package com.neonatal.rabbitMQCollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class RabbitMqCollectorApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(RabbitMqCollectorApplication.class, args);

		String profile = System.getProperty("spring.profiles.active");
		if (profile.equals("Node")) {
			Node myProducer = context.getBean(Node.class);

		}
		else {
			Controller myConsumer = context.getBean(Controller.class);

		}
	}

}
