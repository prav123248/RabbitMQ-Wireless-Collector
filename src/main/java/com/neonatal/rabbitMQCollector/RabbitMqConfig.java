package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitMqConfig {

    @Value("${rabbitmq.host}")
    private String hostIP;

    @Value("${rabbitmq.port}")
    private int serverPort;

    @Profile("Receiver")
    @Bean(name="dataQueue")
    Queue dataQueue() {return new Queue("data", false);}

    @Profile("Receiver")
    @Bean(name="scheduleQueue")
    Queue scheduleQueue() {return new Queue("schedule.#", false);}

    @Profile("Receiver")
    @Bean(name="initialContact")
    Queue contactQueue() {return new Queue("initialContact", false);}

    @Profile("Receiver")
    @Bean
    DirectExchange exchange() {return new DirectExchange("exchange");}

    @Profile("Receiver")
    @Bean
    List<Binding> binding(@Qualifier("dataQueue") Queue data, @Qualifier("scheduleQueue") Queue schedule, @Qualifier("initialContact") Queue contact, DirectExchange exchange) {
        List<Binding> bindings = new ArrayList<>();
        bindings.add(BindingBuilder.bind(data).to(exchange).with("data"));
        bindings.add(BindingBuilder.bind(schedule).to(exchange).with("schedule.*"));
        bindings.add(BindingBuilder.bind(contact).to(exchange).with("initialContact"));
        return bindings;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
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