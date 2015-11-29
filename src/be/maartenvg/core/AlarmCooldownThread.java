package be.maartenvg.core;

import be.maartenvg.io.arduino.Arduino;
import be.maartenvg.io.arduino.ArduinoCommand;

public class AlarmCooldownThread extends Thread {
    private final AlarmSystemCore alarmSystemCore;
    private final Arduino arduino;
    private final int cooldownDuration;

    public AlarmCooldownThread(AlarmSystemCore alarmSystemCore, Arduino arduino, int cooldownDuration) {
        this.alarmSystemCore = alarmSystemCore;
        this.arduino = arduino;
        this.cooldownDuration = cooldownDuration;
    }

    @Override
    public void run() {
        try {
            System.out.println("Cooldown started... (" + cooldownDuration + " ms)");

            Thread.sleep(cooldownDuration);
            arduino.sendCommand(ArduinoCommand.DISABLE_SIREN);
            arduino.sendCommand(ArduinoCommand.DISABLE_WARNING_LED);
            alarmSystemCore.setStatus(AlarmStatus.ARMED);

            System.out.println("Cooldown ended!");
        } catch (InterruptedException e) {
            System.out.println("Cooldown aborted.");
            return;
        }
    }
}