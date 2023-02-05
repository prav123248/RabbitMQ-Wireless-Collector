package com.neonatal.rabbitMQCollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class RabbitMqCollectorApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(RabbitMqCollectorApplication.class, args);
		Sender myProducer = context.getBean(Sender.class);
		for (int i=0; i<10; i++) {
			myProducer.sendMessage("Hello from the Doctor " + i);
		}
		//myProducer.sendMessage("Hello Universe!");
		//Receiver myConsumer = context.getBean(Receiver.class);
		//myConsumer.sendSchedule("Welcome!");
	}

}
