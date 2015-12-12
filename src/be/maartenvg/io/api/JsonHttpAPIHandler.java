package be.maartenvg.io.api;

import be.maartenvg.core.AlarmSystemCore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class JsonHttpAPIHandler implements HttpHandler {
    private final AlarmSystemCore alarmSystemCore;

    public JsonHttpAPIHandler(AlarmSystemCore alarmSystemCore) {
        this.alarmSystemCore = alarmSystemCore;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        if (!t.getRequestMethod().equals("POST")){
            t.close();
            return;
        }

        String queryUrl = t.getRequestURI().getQuery();
        Map<String, String> queryParams = queryToMap(queryUrl);

        String PIN = queryParams.get("pin");
        String action = queryParams.get("action");

        if(PIN.equals("1234")){ //TODO make setting rather than hardcoded pin
            if(action.equals("arm")){
                alarmSystemCore.arm();
            }
            else{
                alarmSystemCore.disarm();
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", "OK");

            StringWriter stringWriter = new StringWriter();
            jsonObject.write(stringWriter);
            String response = stringWriter.toString();
            t.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
            t.close();
        } else {
            t.sendResponseHeaders(401, 0);
            t.close();
        }
    }

    public Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }
}