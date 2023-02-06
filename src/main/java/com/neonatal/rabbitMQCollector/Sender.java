package com.neonatal.rabbitMQCollector;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
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

    private String ID;

    @Value("${rabbitmq.path}")
    private String csvPath;

    public Sender() {
        try {
            InetAddress inet = InetAddress.getLocalHost();
            ID = inet.getHostAddress();
        }
        catch(UnknownHostException e) {
            System.out.println("Could not retrieve host information");
            ID = String.valueOf(new Random().nextInt());
        }
    }

    public void sendMessage() {

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
            System.out.println("File wasn't found (sender).");
        }
        catch(InvalidPathException e) {
            System.out.print("Path is invalid (sender).");
        }
        catch(IOException e) {
            System.out.println("File error occurred (sender).");
        }

        System.out.println("Successfully sent data");
    }

    @RabbitListener(queues="schedule")
    public void processSchedule(String message) {
        System.out.println("Received Schedule message: " + message);
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
