package be.maartenvg.io.parse;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

public class ParsePush implements PushMessageAPI {
    private final String applicationId, restApiKey;

    public ParsePush(String applicationId, String restApiKey) {
        this.applicationId = applicationId;
        this.restApiKey = restApiKey;
    }

    @Override
    public void sendPushMessage(String title, String message, String extraData){
        new Thread(){
            @Override
            public void run() {
                try {
                    JSONObject json = new JSONObject();

                    JSONObject where = new JSONObject();
                    where.put("deviceType", "android");
                    json.put("where", where);

                    JSONObject data = new JSONObject();
                    data.put("alert", message);
                    data.put("title", title);
                    data.put("extraData", extraData);
                    json.put("data", data);

                    HttpResponse<JsonNode> response = Unirest.post("https://api.parse.com/1/push")
                            .header("x-parse-application-id", applicationId)
                            .header("x-parse-rest-api-key", restApiKey)
                            .header("content-type", "application/json")
                            .header("cache-control", "no-cache")
                            .body(json.toString())
                            .asJson();
                    if(!response.getBody().getObject().getBoolean("result")) System.out.println("PushMessageResult was false");
                } catch (UnirestException e) {
                    System.out.println("Something went wrong while sending push message");
                }
            }
        }.start();
    }
}