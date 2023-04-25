package com.neonatal.rabbitMQCollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;


import java.time.LocalDateTime;

import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RabbitMqCollectorApplication {

	public static void main(String[] args) {
        String profile = System.getProperty("spring.profiles.active");

        if (args.length == 0) {
            if (profile.equals("Controller")) {
                ControllerInterface.launch(ControllerInterface.class);
            }
            else {
                NodeInterface.launch(NodeInterface.class);
            }
        }
        else {
            ApplicationContext context = SpringApplication.run(RabbitMqCollectorApplication.class, args);
            Scanner scanner = new Scanner(System.in);
            if (profile.equals("Node")) {
                Node myProducer = context.getBean(Node.class);
                while (true) {
                    System.out.println("<Node> Enter an action :");
                    String request = scanner.nextLine();
                    if (request.equals("Disconnect")) {
                        myProducer.disconnect();
                    } else if (request.equals("Connect")) {
                        System.out.println("Enter the name of the controller to connect to : ");
                        String chosen = scanner.nextLine();
                        myProducer.connect(chosen);
                    } else if (request.equals("Pause")) {
                        myProducer.pauseControl(true);
                    } else if (request.equals("Resume")) {
                        myProducer.pauseControl(false);
                    } else if (request.equals("Switch Patient")) {
                        myProducer.switchPatient();
                        System.out.println("Patient switched");
                    } else if (request.equals("Shutdown")) {
                        myProducer.shutdown();
                        System.exit(0);
                    }
                }
            } else {
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
                    } else if (request.equals("Cancel")) {
                        cancelSchedule(myConsumer);
                    } else if (request.equals("Disconnect")) {
                        myConsumer.disconnect();
                    } else {
                        System.out.println("Invalid input, retry");
                    }
                }
            }
        }
	}

    public static void pullSpecificNode(Controller consumer) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP address :");
        String IP = scanner.nextLine();
        System.out.println("Enter name :");
        String name = scanner.nextLine();
        System.out.println("Sending pull request to " + IP + ", " + name);
        consumer.sendPullRequest(IP, name);

    }

    public static void cancelSchedule(Controller consumer) {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP address : ");
        String IP = scanner.nextLine();
        System.out.println("Enter name : ");
        String name = scanner.nextLine();
        System.out.println("Is this a periodic task? (Y for yes and N for no)");
        String periodic = scanner.nextLine();
        boolean periodicBool = false;
        if (periodic.equals("Y") || periodic.equals("y")) {
            periodicBool = true;
        }
        consumer.cancelSchedule(IP, name, periodicBool);

    }

    public static void pullAllNodes(Controller consumer) {
        Map<String, String> connectedNodes = consumer.getNodeNames();

        if (connectedNodes.size() == 0) {
            System.out.println("There are 0 nodes connected to this controller so no pull requests were sent.");
        }

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
        System.out.println("What periodic interval should this pull request occur at? Enter 0 for non-periodic/single run in milliseconds");
        String interval = scanner.nextLine();

        String[] hourMinArray = givenTime.split(":");
        LocalDateTime scheduledTime = LocalDateTime.now().withHour(Integer.parseInt(hourMinArray[0]))
                .withMinute(Integer.parseInt(hourMinArray[1]))
                .withSecond(0);

        LocalDateTime now = LocalDateTime.now();
        long secondsTillScheduled = now.until(scheduledTime, java.time.temporal.ChronoUnit.MILLIS);
        System.out.println("Sending pull request in " + secondsTillScheduled/1000 + " seconds");

        long intervalPeriod = 0;
        try {
            intervalPeriod = Long.parseLong(interval);
        }
        catch(NumberFormatException e) {
            System.out.println("Interval value is invalid, not proceeding with the schedule. Please make sure it is numeric.");
        }

        if (interval.equals("0")) {
            System.out.println("Performing single run schedule");
            consumer.schedulePullInterval(IP, name, secondsTillScheduled, 0);
        }
        else {
            System.out.println("Performing periodic run schedule");
            consumer.schedulePullInterval(IP, name, secondsTillScheduled, intervalPeriod);
        }
    }
    public static void listNodes(Controller consumer) {
        Map<String, String> connectedNodes = consumer.getNodeNames();

        System.out.println(connectedNodes.size() + " node(s) currently connected to this controller");

        for (String ipAddress : connectedNodes.keySet()) {
            System.out.println("Connected : " + ipAddress + " (" + connectedNodes.get(ipAddress) + ")");
        }
    }



}
