package be.maartenvg.io.parse;

public interface PushMessageAPI {
    void sendPushMessage(String title, String message, String data);
}
