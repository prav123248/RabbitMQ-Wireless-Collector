package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

//Receiver class responsible for processing received data and controlling the communication schedule
//Receiver represents the central point in the system where data is sent to.
@Profile("Receiver")
@Service
public class Receiver {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues="data")
    public void processMessage(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        String nodeName = (String)headers.get("nodeName");
        String nodeID = (String)headers.get("nodeID");

        System.out.println("Received data by " + nodeName + " with the ID " + nodeID);
        byte[] data = message.getBody();
        processByteArray(data);
        System.out.println("Completed processing data and saved it back to a CSV file");
    }

    public void sendSchedule(String message) {
        System.out.println("Sent to schedule queue");
        rabbitTemplate.convertAndSend("schedule", message);
    }

    public static void processByteArray(byte[] data) {
        try (OutputStream outputStream = new FileOutputStream("collectedData/received/receivedAS3DataExport.csv")) {
            outputStream.write(data);
        }
        catch(IOException e) {
            System.out.println("An error occurred");
        }
    }
}