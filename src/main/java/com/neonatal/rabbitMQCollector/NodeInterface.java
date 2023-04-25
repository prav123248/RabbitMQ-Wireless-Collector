package com.neonatal.rabbitMQCollector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Text;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;


public class NodeInterface extends Application {

    private String nodeName = "";
    private boolean exportLoc = false;
    private boolean filterLoc = false;


    @Override
    public void start(Stage stage) {
        VBox vertLayout = new VBox();
        vertLayout.setAlignment(Pos.CENTER);

        Label title = new Label("Node");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setAlignment(Pos.CENTER);

        //Connection factory username and password
        TextField username = new TextField();
        PasswordField password = new PasswordField();
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
        deviceName.setPromptText("Node Name");

        //RabbitMQ Controller Name
        TextField controllerName = new TextField();
        controllerName.setMaxWidth(400);
        controllerName.setPromptText("Controller Name (Controller to connect to)");

        //AS3 Export Path
        Label exportPath = new Label("No path currently selected, please click 'Browse Export Path' below and select the VSCapture export file.");
        exportPath.setTextFill(Color.RED);
        Button exportBrowse = new Button("Browse Export Path");
        exportBrowse.setOnAction(event -> {
           FileChooser browseFiles = new FileChooser();
           File csvFile = browseFiles.showOpenDialog(new Stage());
           if (csvFile != null) {
               exportPath.setText(csvFile.getAbsolutePath());
               exportPath.setTextFill(Color.GREEN);
               exportLoc = true;
           }
        });

        //Parameters
        TextField parameters = new TextField();
        parameters.setPromptText("Enter comma separated indexes of the desired parameters to filter. Example : 1,2,6");
        parameters.setMaxWidth(400);

        //Filtered Export Path
        Label filterPath = new Label("No path currently selected, please click 'Browse Filter Path' below and select a folder.");
        filterPath.setTextFill(Color.RED);
        Button filterBrowse = new Button("Browse Filter Path");
        filterBrowse.setOnAction(event -> {
            DirectoryChooser filterFolders = new DirectoryChooser();
            File filterLocation = filterFolders.showDialog(new Stage());
            if (filterLocation != null) {
                filterPath.setText(filterLocation.getAbsolutePath());
                filterPath.setTextFill(Color.GREEN);
                filterLoc = true;
            }
        });

        vertLayout.getChildren().addAll(title,username, password, brokerIP, brokerPort, deviceName, controllerName, exportPath, exportBrowse, parameters, filterPath, filterBrowse);
        Button submit = new Button("Submit");
        submit.setOnAction(event -> {

            if (!exportLoc || !exportPath.getText().endsWith(".csv")) {
                ControllerInterface.errorDialog("Export Input Error", "Export Path Selection Error","Failed to select a valid VSCapture export path. Please retry and use the Browse Export button until the text changes from red to green. Make sure to select a csv file.");
                return;
            }

            if (!filterLoc) {
                ControllerInterface.errorDialog("Filter File Input Error", "Filter path Selection Error", "Failed to select a valid folder to store the filtered csv files. Please retry and use the Browse Filter Path button until the text changes from red to green. Make sure to select a folder with write permissions.");
                return;
            }


            Properties propsSource = new Properties();
            propsSource.put("rabbitmq.username", username.getText());
            propsSource.put("rabbitmq.password", password.getText());
            propsSource.put("rabbitmq.guiMode", "true");
            propsSource.put("rabbitmq.serverIP",brokerIP.getText());
            propsSource.put("rabbitmq.serverPort",brokerPort.getText());
            propsSource.put("rabbitmq.name",deviceName.getText());
            propsSource.put("rabbitmq.controllerName", controllerName.getText());
            propsSource.put("rabbitmq.filterPath",filterPath.getText());
            propsSource.put("rabbitmq.captureParameters",parameters.getText());
            propsSource.put("rabbitmq.exportPath",exportPath.getText());

            for (Map.Entry<Object, Object> entry : propsSource.entrySet()) {
                if (entry.getValue() == "" || entry.getValue() == null) {
                    ControllerInterface.errorDialog("Empty field Error", "Input Error","Error : " + entry.getKey() + " Field is not filled.");
                    return;
                }
            }

            try {
                Integer.parseInt(brokerPort.getText());
            }
            catch(NumberFormatException e) {
                ControllerInterface.errorDialog("Type error", "Input Error", "Broker port is not an integer value.");
                return;
            }

            if (!ControllerInterface.brokerConnectionTest(brokerIP, username, password, brokerPort)) {
                return;
            };

            ApplicationContext context;

            try {
                context = new SpringApplicationBuilder(RabbitMqCollectorApplication.class).properties(propsSource).run();
                nodeScene(stage, context);
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
        vertLayout.getChildren().add(submit);
        vertLayout.setSpacing(10);
        vertLayout.setPrefSize(640,480);
        Scene window = new Scene(vertLayout);

        stage.setScene(window);
        stage.show();
    }

    public void nodeScene(Stage window, ApplicationContext context) {
        Node nodeBean = context.getBean(Node.class);

        GridPane actionsLayout = new GridPane();
        actionsLayout.setPrefSize(640,480);
        actionsLayout.setHgap(10);
        actionsLayout.setVgap(5);

        //Custom Console
        TextArea feedbackConsole = new TextArea();
        PrintStream outputStream = new PrintStream(new consoleStream(feedbackConsole));
        System.setOut(outputStream);
        feedbackConsole.setEditable(false);
        actionsLayout.add(feedbackConsole,3,3,5,5);

        //General non-node-specific Buttons
        VBox generalButtonContainer = new VBox();
        Button switchPatient = new Button("Switch Patient");
        Button pause = new Button("Pause");
        Button disconnect = new Button("Disconnect");
        Button shutdown = new Button("Shutdown");
        generalButtonContainer.getChildren().addAll(switchPatient,pause, disconnect, shutdown);
        generalButtonContainer.setAlignment(Pos.CENTER);
        generalButtonContainer.setSpacing(10);
        actionsLayout.add(generalButtonContainer, 9,3,1,5);

        switchPatient.setOnAction(event -> {
            nodeBean.switchPatient();
            System.out.println("Switched Patient");
        });

        pause.setOnAction(event -> {
            boolean currentlyPaused = nodeBean.reversePause();
            if (currentlyPaused) {
                pause.setText("Resume");
                pause.setTextFill(Color.RED);
            }
            else {
                pause.setText("Pause");
                pause.setTextFill(Color.GREEN);
            }
        });

        disconnect.setOnAction(event -> {
            nodeBean.disconnect();
        });

        shutdown.setOnAction(event -> {
            nodeBean.shutdown();
            System.exit(0);
        });

        HBox connectContainer = new HBox();
        TextField controllerName = new TextField();
        controllerName.setPromptText("Enter the Controller name to connect to");
        Button connect = new Button("Connect");
        connectContainer.getChildren().addAll(controllerName, connect);
        connectContainer.setAlignment(Pos.CENTER);
        connectContainer.setBorder(new Border(new BorderStroke(null, BorderStrokeStyle.SOLID, null, null)));
        actionsLayout.add(connectContainer, 3,11,5,5);

        connect.setOnAction(event -> {
           String controller = controllerName.getText();
           if (controller.equals("")) {
               System.out.println("Please enter a controller name to connect to.");
               return;
           }
           nodeBean.connect(controller);
        });

        //Helpful information Container
        VBox infoContainer = new VBox();
        String helpfulInfoTitle = "Information";
        String helpfulInfo = "Please disconnect from any connected node before attempting to connect to a new node.";
        String helpfulInfo2 = "If an authentication request has been sent, the node cannot request connection again without restarting to avoid network congestion.";
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
        System.out.println(nodeName + " Successfully Connected to Broker.");

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

    public static boolean emptyText(TextField text, String name) {
        String result = text.getText();
        if (result == null || result.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Input Error");
            alert.setContentText("Error : " + name + " Field is not filled");
            alert.showAndWait();
            return true;
        }
        return false;
    }

}
