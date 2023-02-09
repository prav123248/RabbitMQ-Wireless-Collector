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
import java.util.HashMap;
import java.util.Map;

//Controller class responsible for processing received data and controlling the communication schedule
//Controller represents the central point in the system where data is sent to.
@Profile("Controller")
@Service
public class Controller {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    //Temporary Storage - will store in DB later
    private Map<String, String> nodeNames = new HashMap<>();
    private Map<String, Integer> nodeSchedule = new HashMap<>();

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

    @RabbitListener(queues="authentication")
    public void processNewNode(String message) {
        String[] messageArray = message.split(",");

        if (nodeNames.containsKey(messageArray[1])) {
            System.out.println(messageArray[1] + " with name " + messageArray[0] + " already connected before.");
            return;
        }

        nodeNames.put(messageArray[1], messageArray[0]);
        nodeSchedule.put(messageArray[1], 3500);

        System.out.println("Users are :");
        for (String name : nodeNames.keySet()) {
            System.out.println(name + " with name : " + nodeNames.get(name) + " with schedule " + nodeSchedule.get(name));
        }
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