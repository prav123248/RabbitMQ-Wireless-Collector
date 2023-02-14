package com.neonatal.rabbitMQCollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class RabbitMqCollectorApplication {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(RabbitMqCollectorApplication.class, args);

		String profile = System.getProperty("spring.profiles.active");
		if (profile.equals("Node")) {
			Node myProducer = context.getBean(Node.class);

		}
		else {
			Controller myConsumer = context.getBean(Controller.class);

            while (true) {
                Scanner scanner = new Scanner(System.in);
                String request = scanner.nextLine();
                System.out.println("<Controller> Enter an action :");
                if (request.equals("Pull all nodes")) {
                    pullAllNodes(myConsumer);
                } else if (request.equals("Pull specific node")) {
                    pullSpecificNode(myConsumer);
                } else if (request.equals("Pull on schedule")) {
                    pullOnSchedule(myConsumer);
                } else if (request.equals("List")) {
                    listNodes(myConsumer);
                } else {
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
        consumer.sendPullRequest(IP, name);
        System.out.println("Sent pull request to " + IP + ", " + name);
    }

    public static void pullAllNodes(Controller consumer) {
        Map<String, String> connectedNodes = consumer.getNodeNames();
        for (String ipAddress : connectedNodes.keySet()) {
            consumer.sendPullRequest(ipAddress, connectedNodes.get(ipAddress));
            System.out.println("Pulling data from " + ipAddress + " (" + connectedNodes.get(ipAddress) + ")");
        }
    }

    public static void pullOnSchedule(Controller consumer) {
        System.out.println("To DO");
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
