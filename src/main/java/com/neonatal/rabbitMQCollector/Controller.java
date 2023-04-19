package com.neonatal.rabbitMQCollector;

import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

//Controller class responsible for processing received data and controlling the communication schedule
//Controller represents the central point in the system where data is sent to.
@Profile("Controller")
@Service
@DependsOn("adminInitialisation")
public class Controller {

    @Autowired
    private DirectExchange exchange;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.name}")
    private String name;

    @Autowired
    private DatabaseOperator connector;

    //Data on Connected Nodes
    private Map<String, String> nodeIdentity = new HashMap<>();
    private Map<String, String> nodeSecretKeys = new HashMap<>();
    private Map<String, String> currentPatientID = new HashMap<>();

    //Schedule Manager
    private scheduleManager scheduler = new scheduleManager();

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
        String requestType = messageArray[0];
        String nodeName = messageArray[1];
        String id = messageArray[2];

        //L for Leaving/Disconnection
        if (requestType.equals("L")) {
            if (nodeIdentity.containsKey(id)) {
                if (messageArray[3].equals(nodeSecretKeys.get(id))) {
                    System.out.println("Secret Key verified - Node " + id + " (" + nodeIdentity.get(id) + ") is disconnecting.");
                    nodeIdentity.remove(id);
                }
                else {
                    System.out.println("Secret Key does not match - Node stays in the system");
                }
            }
            else {
                System.out.println("Node " + id + " with name " + nodeName + " is not connected but requested to disconnect.");
            }
        }
        else if (requestType.equals("A")) {

            //Could be a possible security risk - but you need to fake your IP address for this and have credentials for RabbitMQ Server
            //Need this if Node reconnects after already authenticating in the past
            if (nodeIdentity.containsKey(id)) {
                System.out.println(id + " with name " + nodeName + " already connected before.");
                String successResponse = "A,T," + nodeSecretKeys.get(id) + ",data-" + this.name;
                rabbitTemplate.convertAndSend(nodeName + "-" + id, successResponse);
                return;
            }

            System.out.println("Node "  + nodeName + " with ID " + id + " is attempting to connect to controller " + this.name);
            System.out.println("Approve this connection? Type Y for Yes");
            Scanner scanner = new Scanner(System.in);

            String approval = (scanner.nextLine()).toUpperCase();
            if (approval.equals("Y")) {
                //Key is ID, Value is Name
                nodeIdentity.put(id, nodeName);
                nodeSecretKeys.put(id, String.valueOf(new Random().nextInt()));
                String successResponse = "A,T," + nodeSecretKeys.get(id) + ",data-" + this.name;
                rabbitTemplate.convertAndSend(nodeName + "-" + id, successResponse);
            }
            else {
                String failedResponse = "A,F";
                rabbitTemplate.convertAndSend(nodeName + "-" + id, failedResponse);
            }
        }
        else {
            System.out.println("Received an invalid authentication request.");
        }
    }

    public void dataHandler(Message message) {

        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        String nodeName = (String)headers.get("nodeName");
        String nodeID = (String)headers.get("nodeID");
        String exportNumber = (String)headers.get("exportNumber");

        System.out.println("Received data by " + nodeName + " with the ID " + nodeID);
        byte[] data = message.getBody();

        System.out.println("Added " + saveToDB(data, exportNumber, nodeID) + " lines to the database successfully");

    }

    //DB Export
    private int saveToDB(byte[] data, String exportNumber, String nodeID) {
        BufferedReader byteReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
        String line;
        String idCheck;
        Integer count = 0;

        if (!currentPatientID.containsKey(nodeID)) {
            currentPatientID.put(nodeID, "");
        }

        try {
            while ((line = byteReader.readLine()) != null) {
                idCheck = line.split(",")[0];
                if (!idCheck.equals("")) {
                    currentPatientID.put(nodeID, idCheck);
                }
                count += connector.insert(currentPatientID.get(nodeID) + "," + line.substring(line.indexOf(",")+1));
            }
        }
        catch(IOException e) {
            System.out.println("An error occurred while writing to the database " + e.getMessage());
        }
        return count;
    }

    //CSV Export (Unused at the moment)
    private static void processByteArray(byte[] data, String exportNumber) {
        try (OutputStream outputStream = new FileOutputStream("collectedData/received/receivedAS3DataExport" + exportNumber + ".csv")) {
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

    public void schedulePullInterval(String ipAddress, String name, long start, long interval) {
        scheduler.schedulePull(this, ipAddress,name,start,interval);
    }

    public void cancelSchedule(String ipAddress, String name, boolean periodic) {
        scheduler.cancelSchedule(ipAddress,name,periodic);
    }

    public Map<String,String> getNodeNames() {
        return nodeIdentity;
    }

    public void disconnect() {
        //Send transfer request to all connected nodes
        for (String nodeID : nodeIdentity.keySet()) {
            rabbitTemplate.convertAndSend(nodeIdentity.get(nodeID) + "-" + nodeID, "T," + nodeSecretKeys.get(nodeID));
        }
        System.out.println("Sent transfer requests to all connected nodes.");
        try {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate);
            rabbitAdmin.deleteQueue("authentication-" + name);
            rabbitAdmin.deleteQueue("data-" + name);
            System.out.println("Deleted all of the controller's queues");
        }
        catch(Exception e) {
            System.out.println("There was an issue deleting the controller's queues. Please use the management interface on the server to delete it." + e.getMessage());
        }
        System.exit(0);
    }



















}