package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitMqConfig {

    @Bean(name="dataQueue")
    Queue dataQueue() {return new Queue("data", false);}

    @Bean(name="scheduleQueue")
    Queue scheduleQueue() {return new Queue("schedule", false);}

    @Bean
    DirectExchange exchange() {return new DirectExchange("exchange");}

    @Bean
    List<Binding> binding(@Qualifier("dataQueue") Queue data, @Qualifier("scheduleQueue") Queue schedule, DirectExchange exchange) {
        List<Binding> bindings = new ArrayList<>();
        bindings.add(BindingBuilder.bind(data).to(exchange).with("data"));
        bindings.add(BindingBuilder.bind(schedule).to(exchange).with("schedule"));
        return bindings;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        return connectionFactory;
    }
}