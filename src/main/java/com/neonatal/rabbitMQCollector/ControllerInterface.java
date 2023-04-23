package com.neonatal.rabbitMQCollector;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Text;

import java.util.Map;
import java.util.Properties;

@SpringBootApplication
public class ControllerInterface extends Application {

    @Override
    public void start(Stage stage) {
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
        layout.getChildren().addAll(username, password, brokerIP, brokerPort, deviceName, datasourceURL,datasourceUsername,datasourcePassword);


        Button submit = new Button("Submit");
        submit.setOnAction(event -> {
            Properties props = new Properties();
            props.put("rabbitmq.serverIP",brokerIP.getText());
            props.put("rabbitmq.serverPort",brokerPort.getText());
            props.put("rabbitmq.name",deviceName.getText());
            props.put("spring.datasource.url",datasourceURL.getText());
            props.put("spring.datasource.username",datasourceUsername.getText());
            props.put("spring.datasource.password",datasourcePassword.getText());
            props.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");

            for(Map.Entry<Object, Object> e : props.entrySet()) {
                System.out.println(e);
            }

            ApplicationContext context = new SpringApplicationBuilder(RabbitMqCollectorApplication.class).properties(props).run();


            //SpringApplication controller = new SpringApplication(ControllerInterface.class);
            //controller.setDefaultProperties(props);
            //ControllerConfig properties
            //ApplicationContext context = controller.run(ControllerInterface.class);
            System.out.println("Done");
            //Controller myConsumer = context.getBean(Controller.class);

        });
        //Change var names to something useful
        layout.getChildren().add(submit);
        layout.setSpacing(10);
        layout.setPrefSize(640,480);
        Scene scene = new Scene(layout);

        stage.setScene(scene);
        stage.show();
    }


}
