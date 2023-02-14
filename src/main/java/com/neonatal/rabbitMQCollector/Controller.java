package com.neonatal.rabbitMQCollector;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

//Controller class responsible for processing received data and controlling the communication schedule
//Controller represents the central point in the system where data is sent to.
@Profile("Controller")
@Service
public class Controller {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.name}")
    private String name;

    //Data on Connected Nodes
    private Map<String, String> nodeIdentity = new HashMap<>();
    private Map<String, String> nodeSecretKeys = new HashMap<>();

    @PostConstruct
    public void setupQueues() {
        createQueue("authentication-" + name);
        createQueue("data-" + name);
        createQueueListener("authentication-" + name, "authenticationHandler");
        createQueueListener("data-" + name, "dataHandler");
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

    private void createQueueListener(String queueName, String methodListener) {
        SimpleMessageListenerContainer controllerListener = new SimpleMessageListenerContainer();
        controllerListener.setConnectionFactory(rabbitTemplate.getConnectionFactory());
        controllerListener.setQueueNames(queueName);
        if (methodListener.equals("dataHandler")) {
            controllerListener.setMessageListener(msg -> dataHandler(msg));
        }
        //Handles String instead of Messages
        else {
            MessageListenerAdapter adapter = new MessageListenerAdapter();
            adapter.setDefaultListenerMethod(methodListener);
            adapter.setDelegate(this);
            adapter.setMessageConverter(new SimpleMessageConverter());
            controllerListener.setMessageListener(adapter);
        }
        controllerListener.start();
    }

    public void authenticationHandler(String message) {
        String[] messageArray = message.split(",");

        //Could be a possible security risk - but you need to fake your IP address for this and have credentials for RabbitMQ Server
        //Need this if Node reconnects after already authenticating in the past
        if (nodeIdentity.containsKey(messageArray[1])) {
            System.out.println(messageArray[1] + " with name " + messageArray[0] + " already connected before.");
            String successResponse = "A,T," + nodeSecretKeys.get(messageArray[1]) + ",data-" + name;
            rabbitTemplate.convertAndSend(messageArray[0] + "-" + messageArray[1], successResponse);
            return;
        }

        System.out.println("Node "  + messageArray[0] + " with ID " + messageArray[1] + " is attempting to connect to controller " + name);
        System.out.println("Approve this connection? Type Y for Yes");
        Scanner scanner = new Scanner(System.in);

        String approval = (scanner.nextLine()).toUpperCase();
        if (approval.equals("Y")) {
            //Key is ID, Value is Name
            nodeIdentity.put(messageArray[1], messageArray[0]);
            nodeSecretKeys.put(messageArray[1], String.valueOf(new Random().nextInt()));
            String successResponse = "A,T," + nodeSecretKeys.get(messageArray[1]) + ",data-" + name;
            rabbitTemplate.convertAndSend(messageArray[0] + "-" + messageArray[1], successResponse);
        }
        else {
            String failedResponse = "A,F";
            rabbitTemplate.convertAndSend(messageArray[0] + "-" + messageArray[1], failedResponse);
        }

    }

    public void dataHandler(Message message) {

        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        String nodeName = (String)headers.get("nodeName");
        String nodeID = (String)headers.get("nodeID");

        System.out.println("Received data by " + nodeName + " with the ID " + nodeID);
        byte[] data = message.getBody();

        processByteArray(data);
        System.out.println("Completed processing data and saved it back to a CSV file");
    }


    private static void processByteArray(byte[] data) {
        try (OutputStream outputStream = new FileOutputStream("collectedData/received/receivedAS3DataExport.csv")) {
            outputStream.write(data);
        }
        catch(IOException e) {
            System.out.println("An error occurred");
        }
    }


    public boolean sendPullRequest(String ipAddress, String name) {
        if (nodeIdentity.containsKey(ipAddress) && nodeIdentity.get(ipAddress).equals(name)) {
            rabbitTemplate.convertAndSend(name + "-" + ipAddress, "P," + nodeSecretKeys.get(ipAddress) + ",N");
            System.out.println("Successfully sent a pull request to " + ipAddress + " with name " + name);
            return true;
        }
        else {
            System.out.println("Could not find : " + ipAddress + " with name " + name);
            return false;
        }

    }

    public Map<String,String> getNodeNames() {
        return nodeIdentity;
    }



















}