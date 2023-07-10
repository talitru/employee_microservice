package main.webapp;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmployeeServletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter stringWriter;
    private PrintWriter writer;
    private EmployeeServlet employeeServlet;

    @Before
    public void setup() {
        // Create mock objects for HttpServletRequest and HttpServletResponse
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        // Create a StringWriter to capture the response output
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        try {
            when(response.getWriter()).thenReturn(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create an instance of your servlet
        employeeServlet = new EmployeeServlet();
    }

    @Test
    public void testDoPostValidJson() throws IOException {
        BufferedReader reader = new BufferedReader
                (new StringReader("{\"name\":\"John\", \"country\":\"Israel\",\"city\":\"tel-aviv\",\"salary\":5000.5}"));
        when(request.getReader()).thenReturn(reader);
        when(response.getWriter()).thenReturn(writer);

        // Call the doPost method
        employeeServlet.doPost(request, response);

        // Verify the output
        writer.flush();
        String output = stringWriter.toString();
        assertEquals("{\"message\":\"employee successfully added\"}", output.trim());
    }

    @Test
    public void testDoPostInvalidJson() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader("{\"name\":\"Tali\"}")); // Invalid JSON
        when(request.getReader()).thenReturn(reader);

        when(response.getWriter()).thenReturn(writer);

        employeeServlet.doPost(request, response);

        writer.flush();
        String output = stringWriter.toString();
        assertEquals("{\"message\":\"Error while retrieving data\"}", output.trim());
    }

    @Test
    public void testDoGetValidUrl() throws IOException {
        // Set the request URI to "/employees"
        when(request.getRequestURI()).thenReturn("/employees");

        employeeServlet.doGet(request, response);

        writer.flush();
        String jsonResponse = stringWriter.toString();

        assertEquals("{\"employees\":[{\"country\":\"Israel\",\"city\":\"tel-aviv\"," +
                "\"employee_id\":7,\"name\":\"kidi Truneh\",\"salary\":10000.5}]}", jsonResponse.trim());
    }

    @Test
    public void testDoGetValidUrlWithID() throws IOException {
        when(request.getRequestURI()).thenReturn("/employees/7");

        employeeServlet.doGet(request, response);

        writer.flush();
        String jsonResponse = stringWriter.toString();

        assertEquals("{\"country\":\"Israel\",\"city\":\"tel-aviv\"," +
                "\"employee_id\":7,\"name\":\"kidi Truneh\",\"salary\":10000.5}", jsonResponse.trim());
    }

    @Test
    public void testDoGetInvalidUrl() throws IOException {
        when(request.getRequestURI()).thenReturn("/invalid");

        employeeServlet.doGet(request, response);

        writer.flush();
        String output = stringWriter.toString();

        assertEquals("{\"message\":\"Invalid URL\"}", output.trim());
    }

    @Test
    public void testDoDeleteInvalidUrl() throws IOException, ServletException {
        when(request.getRequestURI()).thenReturn("/invalid");

        // Set up the response writer
        when(response.getWriter()).thenReturn(writer);

        employeeServlet.doDelete(request, response);

        // Verify that the response sends an error with the expected status code
        Mockito.verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testDoDeleteValidUrl() throws IOException, ServletException {

        when(request.getRequestURI()).thenReturn("/employees/123");

        when(response.getWriter()).thenReturn(writer);

        employeeServlet.doDelete(request, response);

        writer.flush();
        String output = stringWriter.toString();

        assertEquals("{\"message\":\"Employee deleted successfully\"}", output.trim());
    }


    @Test
    public void testDoPutInvalidInput() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader("{\"name\":\"John\"}"));
        when(request.getReader()).thenReturn(reader);

        employeeServlet.doPut(request, response);

        writer.flush();
        String output = stringWriter.toString();

        assertEquals("{\"message\":\"no id is given\"}", output.trim());
    }

    @Test
    public void testDoPutValidInput() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader("{\"employee_id\":\"7\",\"name\":\"Alex\"}"));
        when(request.getReader()).thenReturn(reader);

        employeeServlet.doPut(request, response);
        writer.flush();
        String output = stringWriter.toString();

        assertEquals("{\"message\":\"Employee updated successfully\"}", output.trim());
    }
}
