package main.webapp;

import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.ibatis.jdbc.ScriptRunner;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.*;


import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



public class EmployeeServlet extends HttpServlet {

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/";

    private static final String username = "tali";
    private static final String password = "dtfd123";
    private final String databaseName = "employees_microservice";
    private static final String CREATE_TABLE_PATH = "/home/tali/Desktop/employeeMicroservice/employeeMicroservice/src/main/webapp" +
            "createEmployeeTable.sql";
    Connection databaseConnection;

    public EmployeeServlet(){
        createDatabase();
        createTables();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {
            JSONObject employeeJson = extractJson(request);
            insertEmployeeToDatabase(employeeJson);
        } catch (ParseException | SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void insertEmployeeToDatabase(JSONObject employee) throws SQLException {
        String query = "INSERT INTO Employees (name, country, city, salary) VALUES (?, ?, ?, ?)";
        PreparedStatement statement = databaseConnection.prepareStatement(query);
        statement.setString(1, (String) employee.get("name"));
        statement.setString(2, (String) employee.get("country"));
        statement.setString(3, (String) employee.get("city"));
        statement.setDouble(4, (double) employee.get("salary"));
        statement.executeUpdate();
        statement.close();
    }

    private void createTables() {
        ScriptRunner runner = new ScriptRunner(databaseConnection);
        try {
            runner.runScript(new FileReader(CREATE_TABLE_PATH));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDatabase(){
        try {
            Connection connection = DriverManager.getConnection(MYSQL_URL, username, password);

            Statement stmt = connection.createStatement();
            String createDatabaseQuery = "CREATE DATABASE IF NOT EXISTS " + databaseName;

            //Executing the query
            stmt.execute(createDatabaseQuery);
            System.out.println("Database " + databaseName +" created");
            connection.close();

            //save the connection to database
            databaseConnection = DriverManager.getConnection
                    (MYSQL_URL + databaseName, username, password);
            databaseConnection.setAutoCommit(false);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private JSONObject extractJson(HttpServletRequest request) throws IOException, ParseException {
        // Read the JSON content from the request body
        BufferedReader reader = request.getReader();
        StringBuilder jsonContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonContent.append(line);
        }

        return (JSONObject) new JSONParser().parse(jsonContent.toString());
    }

    /*

    private JSONArray createJsonArrayFromResultSet(ResultSet resultSet){
        JSONArray employeesArray = new JSONArray();
        JSONObject jsonObject;
        while((jsonObject = createJsonObjectFromResultSet(resultSet) )!= null){
            employeesArray.add(jsonObject);
        }
        return employeesArray;
    }
    private JSONObject createJsonObjectFromResultSet(ResultSet resultSet){
        JSONObject jsonRes = new JSONObject();

        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            if (!resultSet.next()) {
                return null;
            }
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(i);
                jsonRes.put(columnName, value);
            }

        } catch (SQLException e) {
            throw new RuntimeException();
        }
        return jsonRes;

    }


    */


}

/*
class HttpUtils{
    //response status:
    public static final int OK = 200;
    public static final int MESSAGE_ERROR_STATUS = 404;
    public static final int INTERNAL_SERVER_ERROR_STATUS = 500;
    public static final String ERROR_MESSAGE = "Error while retrieving data";
    public static  final String INTERNAL_ERROR_MESSAGE = "Internal server error";

    public static void sendResponse(HttpServletResponse response,JSONObject json, int responseStatus){
        try {
            httpExchange.sendResponseHeaders(responseStatus, json.toString().getBytes().length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(json.toString().getBytes());
            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
*/