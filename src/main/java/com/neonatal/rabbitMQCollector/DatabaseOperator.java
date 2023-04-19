package com.neonatal.rabbitMQCollector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Profile("Controller")
@DependsOn("dataSource")
public class DatabaseOperator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemp;

    private String tableName = "myTable";
    private String headers;
    private boolean tableCreated = false;


    public DatabaseOperator() {}

    public void createTable(String line) {
        String[] columnNames = line.split(",");
        StringBuilder command = new StringBuilder();
        StringBuilder headerCommand = new StringBuilder();
        command.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        headerCommand.append(" (");
        for (String name : columnNames) {
            headerCommand.append(name).append(", ");
            command.append(name).append(" VARCHAR(255), ");
        }
        command.setLength(command.length() - 2);
        headerCommand.setLength(headerCommand.length() - 2);
        headerCommand.append(") ");
        command.append(")");
        headers = headerCommand.toString();
        String createSql = command.toString();
        try {
            jdbcTemp.execute(createSql);
        }
        catch(CannotGetJdbcConnectionException e) {
            System.out.println("Error connecting to database " + e.getMessage());
        }
    }

    public int insert(String line) {
        if (!tableCreated) {
            createTable(line);
            tableCreated=true;
            return 1;
        }
        String[] columnNames = line.split(",");
        StringBuilder command = new StringBuilder();
        command.append("INSERT INTO ").append(tableName).append(headers).append("VALUES (");
        for (String columnName : columnNames) {
            command.append("'").append(columnName).append("'").append(", ");
        }
        command.setLength(command.length()-2);
        command.append(")");
        String createSql = command.toString();
        return jdbcTemp.update(createSql);
    }

}
