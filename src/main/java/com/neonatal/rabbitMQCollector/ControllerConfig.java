package com.neonatal.rabbitMQCollector;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="cInput")
public class ControllerConfig {

    private String username;
    private String password;
    private String brokerIP;
    private String brokerPort;
    private String deviceName;
    private String datasourceURL;
    private String datasourceUsername;
    private String datasourcePassword;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setBrokerIP(String brokerIP) {
        this.brokerIP = brokerIP;
    }

    public void setBrokerPort(String brokerPort) {
        this.brokerPort = brokerPort;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDatasourceURL(String datasourceURL) {
        this.datasourceURL = datasourceURL;
    }

    public void setDatasourceUsername(String datasourceUsername) {
        this.datasourceUsername = datasourceUsername;
    }

    public void setDatasourcePassword(String datasourcePassword) {
        this.datasourcePassword = datasourcePassword;
    }

}
