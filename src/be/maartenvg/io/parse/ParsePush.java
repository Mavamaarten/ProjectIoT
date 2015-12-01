package be.maartenvg.io.parse;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class ParsePush implements PushMessageAPI {
    private final String applicationId, restApiKey;

    public ParsePush(String applicationId, String restApiKey) {
        this.applicationId = applicationId;
        this.restApiKey = restApiKey;
    }

    @Override
    public void sendPushMessage(String title, String message){
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpResponse<JsonNode> response = Unirest.post("https://api.parse.com/1/push")
                            .header("x-parse-application-id", applicationId)
                            .header("x-parse-rest-api-key", restApiKey)
                            .header("content-type", "application/json")
                            .header("cache-control", "no-cache")
                            .body("{\"where\":{\"deviceType\":\"android\"}, \"data\":{\"alert\":\"" + message +  "\",\"title\":\"" + title + "\"}}")
                            .asJson();
                    if(!response.getBody().getObject().getBoolean("result")) System.out.println("PushMessageResult was false");
                } catch (UnirestException e) {
                    System.out.println("Something went wrong while sending push message");
                }
            }
        }.start();
    }
}