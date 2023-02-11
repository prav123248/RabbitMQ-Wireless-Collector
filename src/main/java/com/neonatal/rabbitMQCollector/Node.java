package com.neonatal.rabbitMQCollector;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.util.Map;
import java.util.function.Consumer;


//Node class responsible for sending data according to the communication schedule
//Node represents a node in the system that collects data.
@Profile("Node")
@Service
public class Node {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //Understandable identifier
    @Value("${rabbitmq.name}")
    private String name;

    //IP Address / Unique identifier
    @Autowired
    private String ID;

    @Value("${rabbitmq.path}")
    private String csvPath;

    @Value("${rabbitmq.scheduleInterval}")
    private int scheduleInterval;

    public Node() {
        if (name == null) {
            name = ID;
        }
    }

    @PostConstruct
    public void notifyServer() {
        String nodeIdentity = name + "," + ID;
        System.out.println("Sent authentication request");
        rabbitTemplate.convertAndSend("authentication", nodeIdentity);
    }


    @PostConstruct
    public void controllerListenerContainer() {
        SimpleMessageListenerContainer controllerListener = new SimpleMessageListenerContainer();
        controllerListener.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        controllerListener.setQueueNames(name + "-" + ID);
        MessageListenerAdapter converter = new MessageListenerAdapter();
        converter.setDefaultListenerMethod("pullRequest");
        converter.setDelegate(this);
        controllerListener.setMessageListener(converter);
        controllerListener.start();
    }


    private void pullRequest(String message) {
        System.out.println("Received request to pull by controller");
        sendData();
    }

    public void sendData() {
        //Set headers to pass metadata with the message
        MessageProperties props = new MessageProperties();
        props.setHeader("nodeName", name);
        props.setHeader("nodeID", ID);

        //Convert CSV into bytearray
        try {
            File csvFile = new File(csvPath);
            byte[] data = toByteArray(csvFile);
            Message message = new Message(data, props);
            rabbitTemplate.convertAndSend("data", message);
        }
        catch(FileNotFoundException e)  {
            System.out.println("File wasn't found (node).");
        }
        catch(InvalidPathException e) {
            System.out.print("Path is invalid (node).");
        }
        catch(IOException e) {
            System.out.println("File error occurred (node).");
        }

        System.out.println("Successfully sent data");
    }

    public static byte[] toByteArray(File csvFile) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = new FileInputStream(csvFile)) {
            byte[] chunk = new byte[1024];
            int size;
            while ((size = inputStream.read(chunk)) != -1) {
                outputStream.write(chunk, 0, size);
            }
            return outputStream.toByteArray();

        }
    }



}
