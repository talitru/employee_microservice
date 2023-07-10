package main.webapp;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CurrencyConverter {
    private static final String API_URL_PREFIX = "https://free.currconv.com/api/v7/convert?q=";
    private static final String API_URL_SUFFIX ="&compact=ultra&apiKey=";
    private static final String API_KEY = "1732cd3b11fc58de39a7";
    private static final Double ERROR = -1.0;

    public static void main(String[] args) {
        try {
            Double res = rate(Currency.USD, Currency.ILS);
            System.out.println("res: " + res);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Double rate(Currency from, Currency to) throws IOException, ParseException {

        String API_URL = API_URL_PREFIX+from+"_"+to+API_URL_SUFFIX+API_KEY;
        URL url = new URL(API_URL);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        int responseCode = httpURLConnection.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_OK){
            BufferedReader reader = new BufferedReader(new InputStreamReader((httpURLConnection.getInputStream())));
            JSONObject json = EmployeeServlet.extractJson(reader);
            Double exchangeRate = (Double) (json.get(from.toString()+"_"+to.toString()));

            System.out.println("exchangeRate: "+exchangeRate);
            return exchangeRate;
        }
        else{
            System.out.println("GET Request failed");
            return ERROR;
        }
    }
}

