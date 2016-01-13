package be.maartenvg.core;

import be.maartenvg.core.logging.ActionLogger;
import be.maartenvg.core.logging.LogAction;
import be.maartenvg.core.menu.MenuItem;
import be.maartenvg.io.api.JsonHttpAPIHandler;
import be.maartenvg.io.api.JsonHttpLogsHandler;
import be.maartenvg.io.api.JsonHttpStatusHandler;
import be.maartenvg.io.arduino.Arduino;
import be.maartenvg.io.arduino.ArduinoCommand;
import be.maartenvg.io.arduino.ArduinoListenerAdapter;
import be.maartenvg.io.network.LocalIPUtility;
import be.maartenvg.io.parse.PushMessageAPI;
import be.maartenvg.io.peripherals.RotaryDirection;
import be.maartenvg.io.peripherals.RotaryEncoder;
import be.maartenvg.io.peripherals.RotaryEncoderListener;
import be.maartenvg.settings.Settings;
import be.maartenvg.threads.AlarmCooldownThread;
import be.maartenvg.threads.AlarmCountdownThread;
import be.maartenvg.threads.LcdBacklightThread;
import com.pi4j.component.lcd.LCDTextAlignment;
import com.pi4j.component.lcd.impl.I2CLcdDisplay;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AlarmSystemCore extends ArduinoListenerAdapter implements RotaryEncoderListener, Runnable {
    private final static int LCD_ROW_1 = 0;
    private final static int LCD_ROW_2 = 1;

    private final Log log = LogFactory.getLog(AlarmSystemCore.class);
    private final ActionLogger actionLogger = ActionLogger.getInstance();

    private final Arduino arduino;
    private final I2CLcdDisplay lcd;
    private final PushMessageAPI pushMessageAPI;
    private final String[] sensorNames;
    private final Settings settings = Settings.getInstance();
    private final String ipAddress;

    private Thread workerThread;
    private Thread alarmCountdownThread;
    private Thread alarmCooldownThread;
    private Thread lcdBacklightThread;

    private AlarmStatus status;
    private MenuItem highlightedMenuItem = MenuItem.SHOW_IP_ADDRESS;
    private MenuItem selectedMenuItem = null;
    private List<String> activeSensorNames = new ArrayList<>();

    public AlarmSystemCore(Arduino arduino, I2CLcdDisplay lcd, RotaryEncoder rotaryEncoder, PushMessageAPI pushMessageAPI, String[] sensorNames) throws IOException {
        this.arduino = arduino;
        this.lcd = lcd;
        this.pushMessageAPI = pushMessageAPI;
        this.sensorNames = sensorNames;
        this.status = AlarmStatus.ARMED;

        arduino.addListener(this);
        rotaryEncoder.addListener(this);

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/status", new JsonHttpStatusHandler(this));
        server.createContext("/api", new JsonHttpAPIHandler(this));
        server.createContext("/logs", new JsonHttpLogsHandler());
        server.setExecutor(null);
        server.start();

        this.ipAddress = LocalIPUtility.getLocalIp();

        log.info("SmartAlarm started successfully");
        actionLogger.log(LogAction.OTHER, "SmartAlarm started successfully");
        lcd.clear();
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

        lcdBacklightThread = new LcdBacklightThread(lcd);
        lcdBacklightThread.start();

        log.info("Core WorkerThread started");
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
        enableBacklight();
        pushMessageAPI.sendPushMessage("Alarm disarmed", "SmartAlarm", new JSONObject().put("status", "disarmed").toString());
        log.info("Alarm disarmed");
        actionLogger.log(LogAction.DISARMED);
    }

    public void arm(){
        if(alarmCountdownThread != null && alarmCountdownThread.isAlive()) alarmCountdownThread.interrupt();
        if(alarmCooldownThread != null && alarmCooldownThread.isAlive()) alarmCooldownThread.interrupt();
        arduino.sendCommand(ArduinoCommand.DISABLE_WARNING_LED);
        arduino.sendCommand(ArduinoCommand.DISABLE_SIREN);
        setStatus(AlarmStatus.ARMED);
        enableBacklight();
        pushMessageAPI.sendPushMessage("Alarm armed", "SmartAlarm", new JSONObject().put("status", "armed").toString());
        log.info("Alarm armed");
        actionLogger.log(LogAction.ARMED);
    }

    public void enableBacklight(){
        if(lcdBacklightThread != null && lcdBacklightThread.isAlive()){
            lcdBacklightThread.interrupt();
        }
        lcdBacklightThread = new LcdBacklightThread(lcd);
        lcdBacklightThread.start();
    }

    @Override
    public void onSensorReadingsChanged(int[] values) {
        int sum = IntStream.of(values).reduce(0 ,(a, b) -> a + b);
        enableBacklight();
        if(sum > 0){ // A sensor is activated

            if(alarmCooldownThread != null) alarmCooldownThread.interrupt();

            if(status == AlarmStatus.ARMED && (alarmCountdownThread == null || !alarmCountdownThread.isAlive())){
                alarmCountdownThread = new AlarmCountdownThread(
                        this,               // AlarmSystemCore
                        lcd,                // LcdDisplay
                        arduino,            // Arduino
                        pushMessageAPI,     // PushMessageAPI
                        activeSensorNames,  // Sensors that triggered the countdown
                        settings.getCountdownDuration()  // Countdown length
                );
                alarmCountdownThread.start();
                updateActiveSensorNames();
            }

            log.warn("One or more sensors activated: " + activeSensorNames.stream().collect(Collectors.joining(", ")));
            actionLogger.log(LogAction.SENSOR_ACTIVATED, activeSensorNames.stream().collect(Collectors.joining(", ")));

        } else { // No sensors are activated

            if(status == AlarmStatus.SIRENS_ON || status == AlarmStatus.COUNTDOWN){
                alarmCooldownThread = new AlarmCooldownThread(
                        this,
                        arduino,
                        settings.getCooldownDuration() + settings.getCountdownDuration(),
                        pushMessageAPI
                );
                alarmCooldownThread.start();
            }

            log.info("No more sensors active.");
            actionLogger.log(LogAction.OTHER, "No more sensors active");
        }
    }

    @Override
    public void onRotaryEncoderRotated(RotaryDirection rotaryDirection) {
        enableBacklight();

        if(status == AlarmStatus.SETTINGS && selectedMenuItem == null){
            int currentValue = highlightedMenuItem.getValue();
            if(rotaryDirection == RotaryDirection.LEFT) currentValue--; else currentValue++;
            if(currentValue >= MenuItem.count()) currentValue = 0;
            if(currentValue < 0) currentValue = MenuItem.count() - 1;
            highlightedMenuItem = MenuItem.findByValue(currentValue);
        } else if (selectedMenuItem != null){

            switch(selectedMenuItem){
                case EDIT_COUNTDOWN:
                    int countdownValue = settings.getCountdownDuration() / 1000;
                    if(rotaryDirection == RotaryDirection.LEFT) countdownValue--; else countdownValue++;
                    if(countdownValue < 0) countdownValue = 0;
                    settings.setCountdownDuration(countdownValue * 1000);
                    settings.save();
                    break;
                case EDIT_COOLDOWN:
                    int cooldownValue = settings.getCooldownDuration() / 1000;
                    if(rotaryDirection == RotaryDirection.LEFT) cooldownValue--; else cooldownValue++;
                    if(cooldownValue < 0) cooldownValue = 0;
                    settings.setCooldownDuration(cooldownValue * 1000);
                    settings.save();
                    break;
                case EDIT_BACKLIGHT:
                    int backlightValue = settings.getBacklightTimeout() / 1000;
                    if(rotaryDirection == RotaryDirection.LEFT) backlightValue--; else backlightValue++;
                    if(backlightValue < 0) backlightValue = 0;
                    settings.setBacklightTimeout(backlightValue * 1000);
                    settings.save();
                    break;
            }
        }
    }

    @Override
    public void onRotaryEncoderClicked() {
        enableBacklight();

        switch(status){
            case ARMED:
                disarm();
                break;
            case DISARMED:
                setStatus(AlarmStatus.SETTINGS);
                log.info("Entered settings menu");
                break;
            case COUNTDOWN:
                disarm();
                break;
            case SIRENS_ON:
                disarm();
                break;
            case SETTINGS:
                if(highlightedMenuItem == MenuItem.BACK){
                    highlightedMenuItem = MenuItem.findByValue(0);
                    selectedMenuItem = null;
                    arm();
                    return;
                }
                if(highlightedMenuItem == MenuItem.SHUTDOWN){
                    lcd.writeln(LCD_ROW_1, "Shutting down...");
                    log.info("Shutting down system");

                    try {
                        Runtime.getRuntime().exec("halt");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }

                if(selectedMenuItem == null){
                    selectedMenuItem = highlightedMenuItem;
                    log.info("Menu item selected: " + selectedMenuItem.getMenuTitle());
                } else {
                    selectedMenuItem = null;
                    log.info("Back to main menu");
                }
                break;
        }
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
                        lcd.setBacklight(true);
                        lcd.writeln(LCD_ROW_2, sensorName, LCDTextAlignment.ALIGN_CENTER);
                        break;

                    case SIRENS_ON:
                        lcd.setBacklight(true);
                        lcd.writeln(LCD_ROW_1, "INTRUSION  ALERT");
                        lcd.writeln(LCD_ROW_2, sensorName, LCDTextAlignment.ALIGN_CENTER);
                        break;

                    case SETTINGS:
                        if(selectedMenuItem == null){
                            lcd.writeln(LCD_ROW_1, "Settings");
                            lcd.writeln(LCD_ROW_2, highlightedMenuItem.getMenuTitle());
                        } else {
                            switch(selectedMenuItem){
                                case SHOW_IP_ADDRESS:
                                    lcd.writeln(LCD_ROW_1, highlightedMenuItem.getMenuTitle());
                                    lcd.writeln(LCD_ROW_2, ipAddress, LCDTextAlignment.ALIGN_CENTER);
                                    break;
                                case EDIT_COUNTDOWN:
                                    lcd.writeln(LCD_ROW_1, highlightedMenuItem.getMenuTitle());
                                    lcd.writeln(LCD_ROW_2, String.valueOf(settings.getCountdownDuration() / 1000) + " seconds", LCDTextAlignment.ALIGN_CENTER);
                                    break;
                                case EDIT_COOLDOWN:
                                    lcd.writeln(LCD_ROW_1, highlightedMenuItem.getMenuTitle());
                                    lcd.writeln(LCD_ROW_2, String.valueOf(settings.getCooldownDuration() / 1000) + " seconds", LCDTextAlignment.ALIGN_CENTER);
                                    break;
                                case EDIT_BACKLIGHT:
                                    lcd.writeln(LCD_ROW_1, highlightedMenuItem.getMenuTitle());
                                    lcd.writeln(LCD_ROW_2, String.valueOf(settings.getBacklightTimeout() / 1000) + " seconds", LCDTextAlignment.ALIGN_CENTER);
                                    break;
                                case BACK:
                                    lcd.writeln(LCD_ROW_1, highlightedMenuItem.getMenuTitle());
                                    lcd.writeln(LCD_ROW_2, ipAddress, LCDTextAlignment.ALIGN_CENTER);
                                    break;
                            }
                        }
                        break;
                }

                Thread.sleep(600); // Give the LCD some time to rest

                sensorIndex++;
            } catch (InterruptedException e) {
                log.info("Main thread aborted.");
                break;
            }
        }
    }
}
