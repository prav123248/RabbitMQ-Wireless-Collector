package com.neonatal.rabbitMQCollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;



import java.time.LocalDateTime;

import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

@SpringBootApplication

public class RabbitMqCollectorApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(RabbitMqCollectorApplication.class, args);
        Scanner scanner = new Scanner(System.in);
		String profile = System.getProperty("spring.profiles.active");
		System.out.println(profile);
        if (profile.equals("Node")) {
			Node myProducer = context.getBean(Node.class);
            while (true) {
                System.out.println("<Node> Enter an action :");
                String request = scanner.nextLine();
                if (request.equals("Disconnect")) {
                    myProducer.disconnect();
                }
            }
		}
		else {
			Controller myConsumer = context.getBean(Controller.class);

            while (true) {
                System.out.println("<Controller> Enter an action :");
                String request = scanner.nextLine();
                if (request.equals("Pull all nodes")) {
                    pullAllNodes(myConsumer);
                } else if (request.equals("Pull specific node")) {
                    pullSpecificNode(myConsumer);
                } else if (request.equals("Pull on schedule")) {
                    pullOnSchedule(myConsumer);
                } else if (request.equals("List")) {
                    listNodes(myConsumer);
                }
                else if (request.equals("Disconnect")) {
                    myConsumer.disconnect();
                }
                else {
                    System.out.println("Invalid input, retry");
                }
            }
        }
	}

    public static void pullSpecificNode(Controller consumer) {
        Map<String, String> connectedNodes = consumer.getNodeNames();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP address :");
        String IP = scanner.nextLine();
        System.out.println("Enter name :");
        String name = scanner.nextLine();
        System.out.println("Sending pull request to " + IP + ", " + name);
        consumer.sendPullRequest(IP, name);

    }

    public static void pullAllNodes(Controller consumer) {
        Map<String, String> connectedNodes = consumer.getNodeNames();
        for (String ipAddress : connectedNodes.keySet()) {
            consumer.sendPullRequest(ipAddress, connectedNodes.get(ipAddress));
            System.out.println("Pulling data from " + ipAddress + " (" + connectedNodes.get(ipAddress) + ")");
        }
    }

    public static void pullOnSchedule(Controller consumer) {
        listNodes(consumer);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP address : ");
        String IP = scanner.nextLine();
        System.out.println("Enter name : ");
        String name = scanner.nextLine();
        System.out.println("When should the pull request happen (HH:MM)?");
        String givenTime = scanner.nextLine();
        String[] hourMinArray = givenTime.split(":");
        LocalDateTime scheduledTime = LocalDateTime.now().withHour(Integer.parseInt(hourMinArray[0]))
                .withMinute(Integer.parseInt(hourMinArray[1]))
                .withSecond(0);

        LocalDateTime now = LocalDateTime.now();
        long secondsTillScheduled = now.until(scheduledTime, java.time.temporal.ChronoUnit.MILLIS);
        System.out.println("Sending pull request in " + secondsTillScheduled + " seconds");

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                System.out.println("Sending scheduled pull request now (" + givenTime + ")");
                consumer.sendPullRequest(IP, name);
            }
        };

        timer.schedule(task, secondsTillScheduled);
    }

    public static void listNodes(Controller consumer) {
        Map<String, String> connectedNodes = consumer.getNodeNames();
        for (String ipAddress : connectedNodes.keySet()) {
            System.out.println("Connected : " + ipAddress + " (" + connectedNodes.get(ipAddress) + ")");
        }
    }

    //Controller pullRequest tester - pulls all connected nodes data every 5 seconds
    public static void pullTester(Controller consumer) {
        Map<String,String> connectedNodes = consumer.getNodeNames();
        while (true) {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 5000) {};
            System.out.println("Pulling nodes now.");
            for (String ipAddress : connectedNodes.keySet()) {
                consumer.sendPullRequest(ipAddress, connectedNodes.get(ipAddress));
                System.out.println("Pulled the data from " + ipAddress);
            }
        }

    }

}
