package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.Random;

//Sender class responsible for sending data according to the communication schedule
//Sender represents a node in the system that collects data.
@Profile("Sender")
@Service
public class Sender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //Understandable identifier
    @Value("${rabbitmq.name}")
    private String name;

    //Combined with name to uniquely identify Sender
    private int ID = Math.abs(new Random().nextInt());

    public void sendMessage(String message) {
        System.out.println("Sent to data queue by " + name + " " + ID);
        rabbitTemplate.convertAndSend("data", message);
    }

    @RabbitListener(queues="schedule")
    public void processSchedule(String message) {
        System.out.println("Received Schedule message: " + message);
    }
}

