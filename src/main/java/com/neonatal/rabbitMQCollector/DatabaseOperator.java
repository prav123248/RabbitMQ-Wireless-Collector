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
import java.util.List;

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

        //Table formed by making Create SQL command string
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

        String[] columnNames = line.split(",");
        String[] headersArray = headerAsLine.split(",");

        //Table with more than just patient code
        if (headersArray.length > 1) {
            //Check if first header matches - if line is a header
            if (headersArray[1].equals(columnNames[1])) {
                return 0;
            }
        }

        StringBuilder command = new StringBuilder();
        command.append("INSERT INTO ").append(tableName).append(" VALUES (");
        for (String columnName : columnNames) {
            command.append("'").append(columnName).append("'").append(", ");
        }
        command.setLength(command.length()-2);
        command.append(")");
        String insertData = command.toString();
        try {
            return jdbcTemp.update(insertData);
        }
        catch(BadSqlGrammarException e) {
            System.out.println("Error inserting data into the database. Verify that the schema defined in the database is correct. An error may have occurred during table creation." + e.getMessage());
            return 0;
        }
    }

    private boolean tableExists() {;
        String tableExistSQL = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
        Integer freq = jdbcTemp.queryForObject(tableExistSQL, Integer.class, tableName);
        tablesCreated = freq > 0;

        //Storing headers of the table to ensure when future nodes connect, the headers are not rewritten to the database.
        if (tablesCreated && (headerAsLine==null || headerAsLine=="")) {
            String columnsSQL = "SELECT column_name FROM information_schema.columns WHERE table_name = ?";
            List<String> columnNames = jdbcTemp.queryForList(columnsSQL, String.class, tableName);
            StringBuilder headerCommand = new StringBuilder();
            headerCommand.append(" (");
            for (String column : columnNames) {
                headerCommand.append(column).append(", ");
            }
            headerCommand.setLength(headerCommand.length() - 2);
            headerCommand.append(") ");
            headerAsLine = headerCommand.toString();
        }
        return tablesCreated;
    }
}
