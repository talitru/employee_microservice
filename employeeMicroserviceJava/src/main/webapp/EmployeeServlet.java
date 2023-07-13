package main.webapp;

import java.io.*;
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

public class EmployeeServlet extends HttpServlet {
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/";
    private static final String CREATE_TABLE_FILE = "createEmployeeTable.sql";
    private static final String username = "tali";
    private static final String password = "dtfd123";
    private static final String databaseName = "employees_microservice";
    private Connection databaseConnection;

    public EmployeeServlet(){
        createDatabase();
        createTables();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        JSONObject employees;
        PrintWriter out = response.getWriter();

        try {
            String requestURI = request.getRequestURI();
            if (requestURI.endsWith("/employees")) {

                employees = retrieveEmployees();
                out.println(employees);
            } else if (requestURI.contains("employees/")) {
                String employeeID = requestURI.substring(requestURI.lastIndexOf("/") + 1);
                employees = getEmployee(employeeID);
                out.println(employees);
            }
            else {
                sendResponse(response,"Invalid URL", HttpServletResponse.SC_BAD_REQUEST);

                /* CurrencyConverter dummy test

                Double usd_ils = CurrencyConverter.rate(Currency.USD,Currency.ILS);
                Double ils_usd = CurrencyConverter.rate(Currency.ILS, Currency.USD);
                out.println("1 USD = "+usd_ils+" ILS");
                out.println("1 ILS = "+ils_usd +" USD");

                 */
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JSONObject employeeJson = extractJson(request.getReader());
            if(isJsonValidate(employeeJson)) {
                insertEmployeeToDatabase(employeeJson);
                sendResponse(response,"employee successfully added", HttpServletResponse.SC_ACCEPTED);
            }
            else{
                sendResponse(response,"Error while retrieving data", HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (ParseException | SQLException e) {
            sendResponse(response,"Internal server error", HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject jsonError = new JSONObject();
        response.setContentType("application/json");
        String employeeId;
        PrintWriter out = response.getWriter();
        try {
            JSONObject updateEmployee = extractJson(request.getReader());
            employeeId = (String) (updateEmployee.get("employee_id"));
            if (employeeId == null) {
                sendResponse(response,"no id is given", HttpServletResponse.SC_BAD_REQUEST);
            } else {
                for (Object key : updateEmployee.keySet()) {
                    if(!(key.equals("employee_id"))) {
                        Object value = updateEmployee.get(key);

                        String updateEmployeeQuery = "UPDATE Employees SET " + key + " = ? WHERE employee_id = ?";
                        try (PreparedStatement statement = databaseConnection.prepareStatement(updateEmployeeQuery)) {
                            statement.setObject(1, value);
                            statement.setInt(2, Integer.parseInt(employeeId));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            sendResponse(response,"one of the keys are wrong", HttpServletResponse.SC_BAD_REQUEST);
                        }
                    }
                }
                sendResponse(response,"Employee updated successfully", HttpServletResponse.SC_ACCEPTED);

            }
        } catch (ParseException e) {
            sendResponse(response,"server interval error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI();
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (requestURI.endsWith("employees")) {
            if (deleteAllEmployees()) {
                sendResponse(response, "All employees deleted successfully", HttpServletResponse.SC_ACCEPTED);
            } else {
                sendResponse(response,"An error occurred while deleting employees",HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else if (requestURI.contains("employees/")) {
            // Extract the employee ID from the URL
            String employeeID = requestURI.substring(requestURI.lastIndexOf("/") + 1);

            if (deleteSpecificEmployee(employeeID)) {
                sendResponse(response,"Employee deleted successfully", HttpServletResponse.SC_ACCEPTED);
            } else {
                sendResponse(response,"An error occurred while deleting the employee", HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            // Invalid URL
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void createTables() {
        ScriptRunner runner = new ScriptRunner(databaseConnection);
        try {
            runner.runScript(new FileReader(CREATE_TABLE_FILE));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDatabase(){
        try {
            // Load the JDBC driver
            Class.forName("com.mysql.jdbc.Driver");
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponse(HttpServletResponse response, String message, int status) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();

        jsonResponse.put("message", message);
        out.println(jsonResponse);
        response.setStatus(status);
    }
    private boolean deleteAllEmployees() {
        try {
            Statement statement = databaseConnection.createStatement();
            // Execute the SQL statement to delete all employees
            statement.executeUpdate("DELETE FROM Employees");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean deleteSpecificEmployee(String employeeID) {
        try {
            String deleteEmployeeQuery = "DELETE FROM Employees WHERE employee_id = ?";
            PreparedStatement statement = databaseConnection.prepareStatement( deleteEmployeeQuery);
            statement.setString(1, employeeID);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private JSONObject retrieveEmployees() {
        JSONObject jsonRes = new JSONObject();
        JSONObject jsonErr = new JSONObject();
        String getEmployeesQuery = "SELECT * FROM Employees";

        try {
            Statement statement = databaseConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(getEmployeesQuery );

            JSONArray employeesArray = createJsonArrayFromResultSet(resultSet);
            if(employeesArray == null) {
                jsonErr.put("error", "there are no employees");
            }
            else{
                jsonRes.put("employees", employeesArray);
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jsonRes;
    }

    private JSONObject getEmployee(String employeeID) {
        String getEmployeeQuery = "SELECT * FROM Employees WHERE employee_id = ?";
        JSONObject jsonErr = new JSONObject();
        JSONObject jsonRes;
        try {
            PreparedStatement statement = databaseConnection.prepareStatement(getEmployeeQuery);
            statement.setString(1, employeeID);
            ResultSet resultSet = statement.executeQuery();
            jsonRes = createJsonObjectFromResultSet(resultSet);
            if (jsonRes == null) {
                jsonErr.put("error", "id not exist");
                return jsonErr;
            }

        } catch (SQLException e) {
            jsonErr.put("error", "internal server error");
            return jsonErr;
        }
        return jsonRes;
    }

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

    private boolean isJsonValidate(JSONObject employee) {
        Object name = employee.get("name");
        Object country = employee.get("country");
        Object city = employee.get("city");
        Object salary = employee.get("salary");

        if (!(name instanceof String) || !(country instanceof String) ||
                !(city instanceof String) || !(salary instanceof Number)) {
            return false;
        }
        return true;
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

    public static JSONObject extractJson(BufferedReader reader) throws IOException, ParseException {
        StringBuilder jsonContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonContent.append(line);
        }
        return (JSONObject) new JSONParser().parse(jsonContent.toString());
    }
}
