package be.maartenvg.io.api;

import be.maartenvg.core.AlarmSystemCore;
import be.maartenvg.core.logging.ActionLogger;
import be.maartenvg.core.logging.LogAction;
import be.maartenvg.settings.Settings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class JsonHttpAPIHandler implements HttpHandler {
    private final AlarmSystemCore alarmSystemCore;
    private final Settings settings;
    private final Log log = LogFactory.getLog(JsonHttpAPIHandler.class);
    private final ActionLogger actionLogger = ActionLogger.getInstance();

    public JsonHttpAPIHandler(AlarmSystemCore alarmSystemCore) {
        this.alarmSystemCore = alarmSystemCore;
        settings = Settings.getInstance();
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

        if(action.equals("setpin")){
            settings.setPin(PIN);
            settings.save();
            sendOKResponse(t);
            log.warn("PIN changed to " + PIN + " by " + t.getRemoteAddress().toString());
            actionLogger.log(LogAction.INFORMATION, "PIN changed to " + PIN + " by " + t.getRemoteAddress().toString().replace("/", ""));
        }
        else if(action.equals("arm") && PIN.equals(Settings.getInstance().getPin())){
            alarmSystemCore.arm();
            sendOKResponse(t);
        }
        else if (action.equals("disarm") && PIN.equals(Settings.getInstance().getPin())){
            alarmSystemCore.disarm();
            sendOKResponse(t);
        } else {
            log.warn("Incorrect PIN entered by " + t.getRemoteAddress().toString() + " (action: " + action + ", entered PIN:" + PIN + ")");
            actionLogger.log(LogAction.INCORRECT_PIN, "Incorrect PIN entered by " + t.getRemoteAddress().toString().replace("/", "") + " when trying to " + action + ".");
            t.sendResponseHeaders(401, 0);
            t.close();
        }
    }

    private void sendOKResponse(HttpExchange t) throws IOException {
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