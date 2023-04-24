package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

@Configuration
public class RabbitMqConfig {

    @Value("${rabbitmq.username}")
    private String username;

    @Value("${rabbitmq.password}")
    private String password;

    @Value("${rabbitmq.serverIP}")
    private String serverIP;

    @Value("${rabbitmq.serverPort}")
    private int serverPort;

    @Bean(name="ID")
    public String uniqueID() {
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

    @Bean(name="adminInitialisation")
    public RabbitAdmin rabbitAdmin(ConnectionFactory connect, DirectExchange exchange) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connect);
        rabbitAdmin.setAutoStartup(true);
        rabbitAdmin.declareExchange(exchange);
        return rabbitAdmin;
    }

    @Bean
    public DirectExchange exchange() {
        DirectExchange newExchange = new DirectExchange("exchange", true, false);
        return newExchange;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rTemplate = new RabbitTemplate(connectionFactory);
        rTemplate.setExchange("exchange");
        return rTemplate;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(serverIP);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost("/");
        connectionFactory.setPort(serverPort);
        return connectionFactory;
    }
}