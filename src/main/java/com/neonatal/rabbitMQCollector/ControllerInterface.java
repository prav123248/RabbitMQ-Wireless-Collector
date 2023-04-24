package com.neonatal.rabbitMQCollector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;


public class ControllerInterface extends Application {

    private String controllerName = "";

    @Override
    public void start(Stage stage) {
        Label title = new Label("Controller");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setAlignment(Pos.CENTER);

        VBox layout = new VBox();
        layout.setAlignment(Pos.CENTER);

        //Connection factory username and password
        TextField username = new TextField();
        TextField password = new TextField();
        username.setMaxWidth(250);
        password.setMaxWidth(250);
        username.setPromptText("RabbitMQ Username (Admin account)");
        password.setPromptText("RabbitMQ Password (Admin account)");

        //RabbitMQ Broker details
        TextField brokerIP = new TextField();
        TextField brokerPort = new TextField();
        brokerIP.setMaxWidth(300);
        brokerPort.setMaxWidth(300);
        brokerIP.setPromptText("RabbitMQ Broker Server IP");
        brokerPort.setPromptText("RabbitMQ Broker Server Port");

        //RabbitMQ Device name
        TextField deviceName = new TextField();
        deviceName.setMaxWidth(350);
        deviceName.setPromptText("Controller Name");

        //Backend MySQL server details
        TextField datasourceURL = new TextField();
        TextField datasourceUsername = new TextField();
        TextField datasourcePassword = new TextField();
        datasourceURL.setMaxWidth(400);
        datasourceUsername.setMaxWidth(400);
        datasourcePassword.setMaxWidth(400);
        datasourceURL.setPromptText("Datasource/SQL Server URL");
        datasourceUsername.setPromptText("Datasource/SQL Server Username");
        datasourcePassword.setPromptText("Datasource/SQL Server Password");
        layout.getChildren().addAll(title,username, password, brokerIP, brokerPort, deviceName, datasourceURL,datasourceUsername,datasourcePassword);


        Button submit = new Button("Submit");
        submit.setOnAction(event -> {
            Properties propsSource = new Properties();

            controllerName = deviceName.getText();
            propsSource.put("rabbitmq.username",username.getText());
            propsSource.put("rabbitmq.password",password.getText());
            propsSource.put("rabbitmq.guiMode", "true");
            propsSource.put("rabbitmq.serverIP",brokerIP.getText());
            propsSource.put("rabbitmq.serverPort",brokerPort.getText());
            propsSource.put("rabbitmq.name",deviceName.getText());
            propsSource.put("spring.datasource.url",datasourceURL.getText());
            propsSource.put("spring.datasource.username",datasourceUsername.getText());
            propsSource.put("spring.datasource.password",datasourcePassword.getText());
            propsSource.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");

            for (Map.Entry<Object, Object> entry : propsSource.entrySet()) {
                if (entry.getValue() == "" || entry.getValue() == null) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Input Error");
                    alert.setContentText("Error : " + entry.getKey() + " Field is not filled.");
                    alert.showAndWait();
                    return;
                }
            }

            ApplicationContext context;
            try {
                context = new SpringApplicationBuilder(RabbitMqCollectorApplication.class).properties(propsSource).run();
                controllerScene(stage, context);
            }
            catch(BeanCreationException e) {
                System.out.println("Error creating beans. Ensure inputs are correct and retry." + e.getMessage());
                System.exit(0);
            }
            catch(BeanInstantiationException e) {
                System.out.println("Error instantiation beans. Ensure inputs are correct and retry." + e.getMessage());
                System.exit(0);
            }
            catch(AmqpIOException e) {
                System.out.println("Error reaching network or timeout."  + e.getMessage());
                System.exit(0);
            }
        });

        //Change var names to something useful
        layout.getChildren().add(submit);
        layout.setSpacing(10);
        layout.setPrefSize(640,480);
        Scene scene = new Scene(layout);

        stage.setScene(scene);
        stage.show();
    }

    public void controllerScene(Stage window, ApplicationContext context) {
        Controller controllerBean = context.getBean(Controller.class);

        GridPane actionsLayout = new GridPane();
        actionsLayout.setPrefSize(640,480);
        actionsLayout.setHgap(10);
        actionsLayout.setVgap(5);

        //Custom Console
        TextArea feedbackConsole = new TextArea();
        //PrintStream outputStream = new PrintStream(new consoleStream(feedbackConsole));
        //System.setOut(outputStream);
        feedbackConsole.setEditable(false);
        actionsLayout.add(feedbackConsole,3,3,5,5);

        //General non-node-specific Buttons
        VBox generalButtonContainer = new VBox();
        Button listAll = new Button("List all nodes");
        Button pullAll = new Button("Pull all nodes");
        Button disconnect = new Button("Disconnect");
        generalButtonContainer.getChildren().addAll(listAll,pullAll,disconnect);
        generalButtonContainer.setAlignment(Pos.CENTER);
        generalButtonContainer.setSpacing(10);
        actionsLayout.add(generalButtonContainer, 9,3,1,5);

        listAll.setOnAction(event -> {
            RabbitMqCollectorApplication.listNodes(controllerBean);
        });

        pullAll.setOnAction(event -> {
            RabbitMqCollectorApplication.pullAllNodes(controllerBean);
        });

        disconnect.setOnAction(event -> {
            controllerBean.disconnect();
        });

        //Pull specific node
        HBox pullSpecificContainer = new HBox();
        Label selectNodeMsg = new Label("Select a Node : ");
        TextField ipAddress = new TextField();
        TextField nodeName = new TextField();
        ipAddress.setPromptText("Node IP Address");
        nodeName.setPromptText("Node Name");
        pullSpecificContainer.getChildren().addAll(selectNodeMsg, ipAddress, nodeName);
        pullSpecificContainer.setAlignment(Pos.CENTER);
        pullSpecificContainer.setBorder(new Border(new BorderStroke(null, BorderStrokeStyle.SOLID, null, null)));
        Button pullSpecific = new Button("Pull node");
        actionsLayout.add(pullSpecificContainer,3,11,5,5);
        actionsLayout.add(pullSpecific, 9,13);

        pullSpecific.setOnAction(event -> {
           String IP = ipAddress.getText();
           String name = nodeName.getText();
           if (IP.equals("") || name.equals("")) {
               System.out.println("Please select a node by entering a valid IP address and name. List the nodes to view possible options");
           }
           else {
               pullSpecificNode(controllerBean, IP, name);
           }
        });



        //Pull on schedule
        HBox pullScheduleContainer = new HBox();
        Label scheduleNodeMsg = new Label("Schedule Pull : ");
        TextField startTime = new TextField();
        startTime.setPrefWidth(100);
        startTime.setPromptText("Start (HH:MM)");
        TextField periodicInterval = new TextField();
        periodicInterval.setPrefWidth(280);
        periodicInterval.setPromptText("Periodic Interval (MilliSeconds, 0 is non-periodic)");
        pullScheduleContainer.getChildren().addAll(scheduleNodeMsg, startTime, periodicInterval);
        pullScheduleContainer.setAlignment(Pos.CENTER);
        pullScheduleContainer.setBorder(new Border(new BorderStroke(null, BorderStrokeStyle.SOLID, null, null)));
        actionsLayout.add(pullScheduleContainer, 3, 17, 5,5);
        Button pullSchedule = new Button("Schedule Pull");
        actionsLayout.add(pullSchedule, 9,19);

        pullSchedule.setOnAction(event -> {
            String IP = ipAddress.getText();
            String name = nodeName.getText();
            String start = startTime.getText();
            String interval = periodicInterval.getText();

            if (IP.equals("") || name.equals("") || start.equals("") || interval.equals("")) {
                System.out.println("Please ensure all necessary fields are filled - node name, IP, start time and periodic interval.");
            }
            else {
                pullOnSchedule(controllerBean, IP, name, start, interval);
            }
        });

        //Cancel Schedule
        HBox cancelContainer = new HBox();
        Label cancelNodeMsg = new Label("Cancel schedule : ");
        ToggleGroup selectedScheduleType = new ToggleGroup();
        RadioButton periodic = new RadioButton("Periodic Schedules");
        RadioButton single = new RadioButton("Non-Periodic Schedules");
        periodic.setToggleGroup(selectedScheduleType);
        single.setToggleGroup(selectedScheduleType);
        cancelContainer.getChildren().addAll(cancelNodeMsg, periodic, single);
        cancelContainer.setAlignment(Pos.CENTER);
        cancelContainer.setSpacing(30);
        cancelContainer.setBorder(new Border(new BorderStroke(null, BorderStrokeStyle.SOLID, null, null)));
        actionsLayout.add(cancelContainer, 3, 23, 5,5);
        Button cancelSchedule = new Button("Cancel Schedule");
        actionsLayout.add(cancelSchedule, 9, 25);

        cancelSchedule.setOnAction(event -> {
            String IP = ipAddress.getText();
            String name = nodeName.getText();
            boolean periodicBool = periodic.isSelected();

            if (IP.equals("") || name.equals("") || (!periodic.isSelected() && !single.isSelected())) {
                System.out.println("Please ensure a node is selected by entering a valid name and IP. Also ensure either periodic or non-periodic is selected.");
            }
            else {
                cancelSchedule(controllerBean, IP, name, periodicBool);
            }
        });

        //Helpful information Container
        VBox infoContainer = new VBox();
        String helpfulInfoTitle = "Information";
        String helpfulInfo = "To 'Pull specific node', 'Schedule Pull' and 'Cancel Schedule' - make sure the 'Select a Node' fields are filled in.";
        String helpfulInfo2 = "When scheduling a pull, the periodic interval determines if a pull request should be sent every X milliseconds after the start time (HH:MM). Enter 0 if the pull request should happen at the specified time only.";
        infoContainer.setSpacing(10);
        infoContainer.setAlignment(Pos.CENTER);
        Label title = new Label(helpfulInfoTitle);
        Label description1 = new Label(helpfulInfo);
        description1.setWrapText(true);
        Label description2 = new Label(helpfulInfo2);
        description2.setWrapText(true);
        infoContainer.getChildren().addAll(title,description1,description2);
        actionsLayout.add(infoContainer,3,29,5,8);

        Scene scene = new Scene(actionsLayout, 640, 480);
        window.setScene(scene);
        window.show();
        System.out.println(controllerName + " Successfully Connected to Broker.");





    }

    private class consoleStream extends OutputStream {

        TextArea newConsole;

        public consoleStream(TextArea txt) {
            newConsole = txt;
        }

        @Override
        public void write(int b) throws IOException {
            Platform.runLater(() -> newConsole.appendText(String.valueOf((char)b)));
        }
    }

    public static void pullSpecificNode(Controller consumer, String IP, String name) {
        System.out.println("Sending pull request to " + name + "-" + IP);
        consumer.sendPullRequest(IP, name);
    }

    public static void cancelSchedule(Controller consumer, String IP, String name, boolean periodic) {
        if (periodic) {
            System.out.println("Sending cancel request to " + name +"-" + IP + " for Periodic schedules");
        }
        else {
            System.out.println("Sending cancel request to " + name +"-" + IP + " for Non-Periodic schedules");
        }
        consumer.cancelSchedule(IP, name, periodic);
    }


    public static void pullOnSchedule(Controller consumer, String IP, String name, String givenTime, String interval) {
        String[] hourMinArray = givenTime.split(":");
        LocalDateTime scheduledTime;
        try {
            scheduledTime = LocalDateTime.now().withHour(Integer.parseInt(hourMinArray[0]))
                    .withMinute(Integer.parseInt(hourMinArray[1]))
                    .withSecond(0);
        }
        catch(NumberFormatException e) {
            System.out.println("Please enter the start time (HH:MM) in the correct format. There should be two digits, a colon, and two digits." + e.getMessage());
            return;
        }
        catch(DateTimeException e) {
            System.out.println("The given time is not valid. Please check and ensure it is provided in the right format " + e.getMessage());
            return;
        }

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

}
