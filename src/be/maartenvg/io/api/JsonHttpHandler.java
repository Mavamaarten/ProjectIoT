package be.maartenvg.io.api;

import be.maartenvg.core.AlarmSystemCore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

public class JsonHttpHandler implements HttpHandler {
    private final AlarmSystemCore alarmSystemCore;

    public JsonHttpHandler(AlarmSystemCore alarmSystemCore) {
        this.alarmSystemCore = alarmSystemCore;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", alarmSystemCore.getStatus().name());
        jsonObject.put("sensors", alarmSystemCore.getSensorNames());
        jsonObject.put("activeSensors", alarmSystemCore.getActiveSensorNames());
        StringWriter stringWriter = new StringWriter();
        jsonObject.write(stringWriter);
        String response = stringWriter.toString();
        t.sendResponseHeaders(200, response.getBytes("UTF-8").length);
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes("UTF-8"));
        os.close();
    }
}