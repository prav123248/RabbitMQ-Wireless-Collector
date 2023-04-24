package com.neonatal.rabbitMQCollector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
@Profile("Controller")
@DependsOn("dataSource")
public class DatabaseOperator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemp;

    @Value("${spring.datasource.tableName}")
    private String tableName;

    private String headers;
    private String headerAsLine = "";
    private boolean tablesCreated = false;

    public DatabaseOperator() {}

    public void createTable(String line) {
        headerAsLine = line;
        String[] columnNames = line.split(",");
        columnNames[0] = "PatientCode";
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
        catch(BadSqlGrammarException e) {
            System.out.println("SQL Grammer issue " + e.getMessage());
        }
    }

    public int insert(String line) {
        if (!tablesCreated && !tableExists()) {
            createTable(line);
            tablesCreated = true;
            return 1;
        }

        //Ensures headers are not rewritten into table
        if (line.equals(headerAsLine)) {
            return 0;
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

    private boolean tableExists() {;
        String tableExistSQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + tableName + "';";
        Integer freq = jdbcTemp.queryForObject(tableExistSQL, Integer.class, tableName);
        tablesCreated = freq != null || freq > 0;
        return tablesCreated;
    }
}
