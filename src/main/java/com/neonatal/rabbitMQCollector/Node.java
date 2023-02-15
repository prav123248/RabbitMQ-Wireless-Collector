package com.neonatal.rabbitMQCollector;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.AmqpMessageReturnedException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.InvalidPathException;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;


//Node class responsible for sending data according to the communication schedule
//Node represents a node in the system that collects data.
@Profile("Node")
@Service
@DependsOn("adminInitialisation")
public class Node {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private String ID;

    @Value("${rabbitmq.name}")
    private String name;

    @Value("${rabbitmq.controllerName}")
    private String controllerName;

    @Value("${rabbitmq.path}")
    private String csvPath;

    private boolean sentAuthentication = false;
    private boolean authenticated = false;

    private String secretKey;
    private String dataQueueName;

    public Node() {
        if (name == null) {
            name = ID;
        }
    }

    @PostConstruct
    public void connectToController() {
        String nodeIdentity = "A," + name + "," + ID;
        System.out.println("Sent authentication request");
        createQueue(name + "-" + ID);
        controllerListenerContainer();
        authenticationRequest(nodeIdentity);
    }

    private void controllerListenerContainer() {
        SimpleMessageListenerContainer controllerListener = new SimpleMessageListenerContainer();
        controllerListener.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        controllerListener.setQueueNames(name + "-" + ID);
        MessageListenerAdapter converter = new MessageListenerAdapter();
        converter.setDefaultListenerMethod("controllerRequest");
        converter.setDelegate(this);
        controllerListener.setMessageListener(converter);
        controllerListener.start();
    }


    private void controllerRequest(String message) {

        String[] messageArray = message.split(",");

        //Authentication
        if (messageArray[0].equals("A")) {
            System.out.println("Authentication response received");

            if (!sentAuthentication || authenticated) {
                System.out.println("Discarding old authentication remnants");
                return;
            }
            //Second index contains T or F for True or False
            else if (messageArray[1].equals("T")) {
                System.out.println("Connected accepted by controller");
                authenticated = true;
                secretKey = messageArray[2];
                dataQueueName = messageArray[3];
                System.out.println("Data queue name is : " + dataQueueName);
            }
            else {
                System.out.println("Connection refused by controller.");

            }
        }
        //Pull Request
        else if (messageArray[0].equals("P")) {
            System.out.println("Received request to pull by controller.");
            //Checks if pull request has the secret key
            if (messageArray[1].equals(secretKey)) {
                System.out.println("Secret ID matches");
                //Third index contains N or S for Now or Scheduled
                if (messageArray[2].equals("N")) {
                    System.out.println("Pull request now - sending data");
                    sendData();
                }
                //Fourth index has time
                else if (messageArray[2].equals("S") && messageArray.length == 4) {
                    //Schedule
                    System.out.println("Pull request scheduled - " + messageArray[3]);
                }
            }

            else {
                System.out.println("Controller request not understood by node");
            }

        }
        //Transfer Request
        else if (messageArray[0].equals("T") && messageArray[1].equals(secretKey)) {
            System.out.println("Received request to transfer by controller");
            sentAuthentication = false;
            authenticated = false;
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter the controller to transfer to - ");
            controllerName = scanner.nextLine();
            authenticationRequest("A," + name + "," + ID);
        }
        else {
            System.out.println("Received a response that is unrecognised");
        }
    }

    public void authenticationRequest(String nodeIdentity) {
        try {
            rabbitTemplate.convertAndSend("authentication-" + controllerName, nodeIdentity);
            sentAuthentication = true;
        } catch (AmqpMessageReturnedException e) {
            System.out.println("Message returned error - controller likely doesn't exist as the routing key was invalid");
        }
    }

    public void sendData() {
        //Set headers to pass metadata with the message
        MessageProperties props = new MessageProperties();
        props.setHeader("nodeName", name);
        props.setHeader("nodeID", ID);
        props.setContentType(MessageProperties.CONTENT_TYPE_BYTES);
        //Convert CSV into bytearray
        try {
            File csvFile = new File(csvPath);
            byte[] data = toByteArray(csvFile);
            Message message = new Message(data, props);
            rabbitTemplate.convertAndSend(dataQueueName, message);
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

    private void createQueue(String queueName) {
        ChannelCallback<Void> queueDeclare = new ChannelCallback<Void>() {
            @Override
            public Void doInRabbit(Channel channel) throws Exception {
                channel.queueDeclare(queueName, true, false, false, null);
                channel.queueBind(queueName, rabbitTemplate.getExchange(), queueName);
                return null;
            }
        };

        rabbitTemplate.execute(queueDeclare);
    }

    public void disconnect() {
        if (authenticated == true) {
            String removalMessage = "L," + name + "," + ID + "," + secretKey;
            rabbitTemplate.convertAndSend("authentication-" + controllerName, removalMessage);
            System.out.println("Leave notice sent to controller.");
            try {
                RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate);
                rabbitAdmin.deleteQueue(name + "-" + ID);
                System.out.println("Deleted the node's queues.");
            }
            catch(Exception e) {
                System.out.println("There was an issue deleting the node's queue. Please use the management interface on the server to delete it." + e.getMessage());
            }
        }
        else {
            System.out.println("Node is not connected to a controller already.");
        }
        System.exit(0);
    }
}
