package be.maartenvg.core;

import be.maartenvg.io.api.JsonHttpAPIHandler;
import be.maartenvg.io.api.JsonHttpStatusHandler;
import be.maartenvg.io.arduino.Arduino;
import be.maartenvg.io.arduino.ArduinoCommand;
import be.maartenvg.io.arduino.ArduinoListenerAdapter;
import be.maartenvg.io.parse.PushMessageAPI;
import be.maartenvg.io.peripherals.RotaryDirection;
import be.maartenvg.io.peripherals.RotaryEncoder;
import be.maartenvg.io.peripherals.RotaryEncoderListener;
import com.pi4j.component.lcd.LCDTextAlignment;
import com.pi4j.component.lcd.impl.GpioLcdDisplay;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

public class AlarmSystemCore extends ArduinoListenerAdapter implements RotaryEncoderListener, Runnable {
    private final static int LCD_ROW_1 = 0;
    private final static int LCD_ROW_2 = 1;

    private final Arduino arduino;
    private final GpioLcdDisplay lcd;
    private final RotaryEncoder rotaryEncoder;
    private final PushMessageAPI pushMessageAPI;
    private final String[] sensorNames;

    private Thread workerThread;
    private Thread alarmCountdownThread;
    private Thread alarmCooldownThread;

    private AlarmStatus status;
    private int menuValue = 0;
    private List<String> activeSensorNames = new ArrayList<>();

    public AlarmSystemCore(Arduino arduino, GpioLcdDisplay lcd, RotaryEncoder rotaryEncoder, PushMessageAPI pushMessageAPI, String[] sensorNames) throws IOException {
        this.arduino = arduino;
        this.lcd = lcd;
        this.rotaryEncoder = rotaryEncoder;
        this.pushMessageAPI = pushMessageAPI;
        this.sensorNames = sensorNames;
        this.status = AlarmStatus.ARMED;

        arduino.addListener(this);
        rotaryEncoder.addListener(this);
        lcd.clear();

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/status", new JsonHttpStatusHandler(this));
        server.createContext("/api", new JsonHttpAPIHandler(this));
        server.setExecutor(null);
        server.start();
    }

    public String[] getSensorNames() {
        return sensorNames;
    }

    public List<String> getActiveSensorNames() {
        return activeSensorNames;
    }

    public void setActiveSensorNames(List<String> activeSensorNames) {
        this.activeSensorNames = activeSensorNames;
    }

    public AlarmStatus getStatus() {
        return status;
    }

    public void setStatus(AlarmStatus status) {
        this.status = status;
    }

    public void start() throws CoreException {
        if(workerThread != null) throw new CoreException("Alarm already running");

        workerThread = new Thread(this);
        workerThread.start();
    }

    public void stop() throws CoreException {
        if(workerThread == null) throw new CoreException("Alarm not running");
        workerThread.interrupt();
    }

    public void disarm(){
        if(alarmCountdownThread != null && alarmCountdownThread.isAlive()) alarmCountdownThread.interrupt();
        if(alarmCooldownThread != null && alarmCooldownThread.isAlive()) alarmCooldownThread.interrupt();
        arduino.sendCommand(ArduinoCommand.DISABLE_WARNING_LED);
        arduino.sendCommand(ArduinoCommand.DISABLE_SIREN);
        setStatus(AlarmStatus.DISARMED);
        pushMessageAPI.sendPushMessage("Alarm disarmed", "SmartAlarm", new JSONObject().put("status", "disarmed").toString());
    }

    public void arm(){
        if(alarmCountdownThread != null && alarmCountdownThread.isAlive()) alarmCountdownThread.interrupt();
        if(alarmCooldownThread != null && alarmCooldownThread.isAlive()) alarmCooldownThread.interrupt();
        arduino.sendCommand(ArduinoCommand.DISABLE_WARNING_LED);
        arduino.sendCommand(ArduinoCommand.DISABLE_SIREN);
        setStatus(AlarmStatus.ARMED);
        pushMessageAPI.sendPushMessage("Alarm armed", "SmartAlarm", new JSONObject().put("status", "armed").toString());
    }

    @Override
    public void onArduinoValuesChanged(int[] values) {
        int sum = IntStream.of(values).reduce(0 ,(a, b) -> a + b);

        if(sum > 0){ // A sensor is activated

            if(alarmCooldownThread != null) alarmCooldownThread.interrupt();

            if(status == AlarmStatus.ARMED && (alarmCountdownThread == null || !alarmCountdownThread.isAlive())){
                alarmCountdownThread = new AlarmCountdownThread(
                        this,               // AlarmSystemCore
                        lcd,                // GpioLcdDisplay
                        arduino,            // Arduino
                        pushMessageAPI,     // PushMessageAPI
                        activeSensorNames,  // Sensors that triggered the countdown
                        10000               // Countdown length
                );
                alarmCountdownThread.start();
                updateActiveSensorNames();
            }

        } else { // No sensors are activated

            if(status == AlarmStatus.SIRENS_ON || status == AlarmStatus.COUNTDOWN){
                alarmCooldownThread = new AlarmCooldownThread(
                        this,
                        arduino,
                        2 * 60000,
                        pushMessageAPI
                );
                alarmCooldownThread.start();
            }
        }
    }

    @Override
    public void onRotaryEncoderRotated(RotaryDirection rotaryDirection) {
        if(rotaryDirection == RotaryDirection.LEFT) menuValue--; else menuValue++;
    }

    @Override
    public void onRotaryEncoderClicked() {
        if(status == AlarmStatus.MENU)
            setStatus(AlarmStatus.ARMED);
        else if(status == AlarmStatus.ARMED)
            setStatus(AlarmStatus.DISARMED);
        else if(status == AlarmStatus.DISARMED)
            setStatus(AlarmStatus.MENU);
    }

    private void updateActiveSensorNames(){
        activeSensorNames.clear();
        int[] arduinoValues = arduino.getValues();
        for(int i=0; i< arduinoValues.length; i++){
            if(arduinoValues[i] > 0) activeSensorNames.add(sensorNames[i]);
        }
    }

    @Override
    public void run() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        int sensorIndex = 0;

        while(!Thread.interrupted()) {
            try {
                updateActiveSensorNames();
                if(sensorIndex >= activeSensorNames.size()) sensorIndex = 0;
                String sensorName = "No active sensor";
                boolean sensorActive = false;
                if(activeSensorNames.size() > 0){
                    sensorName = activeSensorNames.get(sensorIndex);
                    sensorActive = true;
                }

                switch(status){
                    case ARMED:
                        lcd.writeln(LCD_ROW_1, "SmartAlarm ARMED");
                        lcd.writeln(LCD_ROW_2, formatter.format(new Date()), LCDTextAlignment.ALIGN_CENTER);
                        break;

                    case DISARMED:
                        lcd.writeln(LCD_ROW_1, "Alarm DISARMED", LCDTextAlignment.ALIGN_CENTER);
                        if(sensorActive)
                            lcd.writeln(LCD_ROW_2, sensorName, LCDTextAlignment.ALIGN_CENTER);
                        else
                            lcd.writeln(LCD_ROW_2, formatter.format(new Date()), LCDTextAlignment.ALIGN_CENTER);
                        break;

                    case COUNTDOWN:
                        lcd.writeln(LCD_ROW_2, sensorName, LCDTextAlignment.ALIGN_CENTER);
                        break;

                    case SIRENS_ON:
                        lcd.writeln(LCD_ROW_1, "INTRUSION  ALERT");
                        lcd.writeln(LCD_ROW_2, sensorName, LCDTextAlignment.ALIGN_CENTER);
                        break;

                    case MENU:
                        lcd.writeln(LCD_ROW_1, "Settings");
                        lcd.writeln(LCD_ROW_2, "Countdown: " + menuValue);
                }

                if(status != AlarmStatus.MENU) Thread.sleep(1000);
                    else Thread.sleep(500);

                sensorIndex++;
            } catch (InterruptedException e) {
                System.out.println("Main thread aborted.");
                break;
            }
        }
    }
}
