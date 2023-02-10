package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
public class RabbitMqConfig {

    @Value("${rabbitmq.host}")
    private String hostIP;

    @Value("${rabbitmq.port}")
    private int serverPort;

    @Profile("Node")
    @Bean(name="ID")
    String uniqueID() {
        String ID;
        //Uses IP Address or Random number to uniquely identify Node
        try {
            InetAddress inet = InetAddress.getLocalHost();
            ID = inet.getHostAddress();
        }
        catch(UnknownHostException e) {
            System.out.println("Could not retrieve host information");
            ID = String.valueOf(new Random().nextInt());
        }
        return ID;
    }

    @Profile("Controller")
    @Bean(name="dataQueue")
    Queue dataQueue() {
        return new Queue("data", false);}

    @Profile("Controller")
    @Bean(name="authenticationQueue")
    Queue authenticationQueue() {return new Queue("authentication", false);}

    @Profile("Controller")
    @Bean
    DirectExchange exchange() {
        DirectExchange myExchange = new DirectExchange("exchange");
        return new DirectExchange("exchange");
    }

    @Profile("Controller")
    @Bean
    Declarables binding() {
        return new Declarables(
                BindingBuilder.bind(dataQueue()).to(exchange()).with("data"),
                BindingBuilder.bind(authenticationQueue()).to(exchange()).with("authentication")
        );
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rTemplate = new RabbitTemplate(connectionFactory);
        rTemplate.setExchange("exchange");
        return rTemplate;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(hostIP);
        connectionFactory.setUsername("node");
        connectionFactory.setPassword("password");
        connectionFactory.setVirtualHost("/");
        connectionFactory.setPort(serverPort);
        return connectionFactory;
    }
}