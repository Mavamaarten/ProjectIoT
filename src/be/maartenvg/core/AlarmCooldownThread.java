package be.maartenvg.core;

import be.maartenvg.io.arduino.Arduino;
import be.maartenvg.io.arduino.ArduinoCommand;
import be.maartenvg.io.parse.PushMessageAPI;
import org.json.JSONObject;

import java.util.ArrayList;

public class AlarmCooldownThread extends Thread {
    private final AlarmSystemCore alarmSystemCore;
    private final Arduino arduino;
    private final int cooldownDuration;
    private final PushMessageAPI pushMessageAPI;

    public AlarmCooldownThread(AlarmSystemCore alarmSystemCore, Arduino arduino, int cooldownDuration, PushMessageAPI pushMessageAPI) {
        this.alarmSystemCore = alarmSystemCore;
        this.arduino = arduino;
        this.cooldownDuration = cooldownDuration;
        this.pushMessageAPI = pushMessageAPI;
    }

    @Override
    public void run() {
        try {
            System.out.println("Cooldown started... (" + cooldownDuration + " ms)");

            Thread.sleep(cooldownDuration);
            arduino.sendCommand(ArduinoCommand.DISABLE_SIREN);
            arduino.sendCommand(ArduinoCommand.DISABLE_WARNING_LED);
            alarmSystemCore.setStatus(AlarmStatus.ARMED);

            JSONObject object = new JSONObject();
            object.put("activeSensors", new ArrayList());
            pushMessageAPI.sendPushMessage("Alarm status OK", "Alarm status OK", object.toString());

            System.out.println("Cooldown ended!");
        } catch (InterruptedException e) {
            System.out.println("Cooldown aborted.");
            return;
        }
    }
}