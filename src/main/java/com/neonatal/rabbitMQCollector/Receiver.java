package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

//Receiver class responsible for processing received data and controlling the communication schedule
//Receiver represents the central point in the system where data is sent to.
@Profile("Receiver")
@Service
public class Receiver {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues="data")
    public void processMessage(String message) {
        System.out.println("Received message: " + message);
    }

    public void sendSchedule(String message) {
        System.out.println("Sent to schedule queue");
        rabbitTemplate.convertAndSend("schedule", message);
    }
}