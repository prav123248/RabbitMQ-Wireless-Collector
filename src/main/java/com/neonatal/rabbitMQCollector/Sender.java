package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

//Sender class responsible for sending data according to the communication schedule
//Sender represents a node in the system that collects data.
@Profile("Sender")
@Service
public class Sender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        System.out.println("Sent to data queue");
        rabbitTemplate.convertAndSend("data", message);
    }

    @RabbitListener(queues="schedule")
    public void processSchedule(String message) {
        System.out.println("Received Schedule message: " + message);
    }
}

