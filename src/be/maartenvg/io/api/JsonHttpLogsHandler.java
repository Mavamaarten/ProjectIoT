package be.maartenvg.io.api;

import be.maartenvg.core.logging.ActionLogger;
import be.maartenvg.core.logging.LogItem;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonHttpLogsHandler implements HttpHandler {
    private final ActionLogger actionLogger = ActionLogger.getInstance();

    @Override
    public void handle(HttpExchange t) throws IOException {
        if (!t.getRequestMethod().equals("GET")) return;

        List<LogItem> reversedLogs = new ArrayList<>(actionLogger.getLogs());
        Collections.reverse(reversedLogs);

        JSONArray jsonArray = new JSONArray(reversedLogs);
        StringWriter stringWriter = new StringWriter();
        jsonArray.write(stringWriter);
        String response = stringWriter.toString();
        t.sendResponseHeaders(200, response.getBytes("UTF-8").length);
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes("UTF-8"));
        os.close();
    }
}