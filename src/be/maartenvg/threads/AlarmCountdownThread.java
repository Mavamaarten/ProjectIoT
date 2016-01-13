package be.maartenvg.threads;

import be.maartenvg.core.AlarmStatus;
import be.maartenvg.core.AlarmSystemCore;
import be.maartenvg.core.logging.ActionLogger;
import be.maartenvg.core.logging.LogAction;
import be.maartenvg.io.arduino.Arduino;
import be.maartenvg.io.arduino.ArduinoCommand;
import be.maartenvg.io.parse.PushMessageAPI;
import com.pi4j.component.lcd.LCDTextAlignment;
import com.pi4j.component.lcd.impl.I2CLcdDisplay;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

public class AlarmCountdownThread extends Thread {
    private static final int LCD_ROW_1 = 0;
    private static final int LCD_ROW_2 = 1;

    private final AlarmSystemCore alarmSystemCore;
    private final I2CLcdDisplay lcd;
    private final Arduino arduino;
    private final PushMessageAPI pushMessageAPI;
    private final List<String> triggeredSensors;
    private final int delay;
    private final Log log = LogFactory.getLog(AlarmCountdownThread.class);
    private final ActionLogger actionLogger = ActionLogger.getInstance();

    public AlarmCountdownThread(AlarmSystemCore alarmSystemCore, I2CLcdDisplay lcd, Arduino arduino, PushMessageAPI pushMessageAPI, List<String> triggeredSensors, int delay) {
        this.alarmSystemCore = alarmSystemCore;
        this.lcd = lcd;
        this.arduino = arduino;
        this.pushMessageAPI = pushMessageAPI;
        this.triggeredSensors = triggeredSensors;
        this.delay = delay;
    }

    @Override
    public void run() {
        int countdown = delay / 1000;
        arduino.sendCommand(ArduinoCommand.ENABLE_WARNING_LED);
        alarmSystemCore.setStatus(AlarmStatus.COUNTDOWN);

        log.warn("Sensor(s) activated: countdown initiated. Triggered sensor(s): " + triggeredSensors.stream().collect(Collectors.joining(",")));
        actionLogger.log(LogAction.COUNTDOWN_ACTIVATED, "Sensor(s) that triggered countdown: " + triggeredSensors.stream().collect(Collectors.joining(",")));

        JSONObject object = new JSONObject();
        object.put("activeSensors", triggeredSensors);
        pushMessageAPI.sendPushMessage("Intrusion alert!", "Countdown activated.", object.toString());

        while(!interrupted() && countdown > 0){
            lcd.writeln(LCD_ROW_1, "COUNTDOWN");
            lcd.write(LCD_ROW_1, String.valueOf(countdown), LCDTextAlignment.ALIGN_RIGHT);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.warn("Countdown aborted");
                break;
            }
            countdown--;
        }

        if(!interrupted()){
            arduino.sendCommand(ArduinoCommand.ENABLE_SIREN);
            alarmSystemCore.setStatus(AlarmStatus.SIRENS_ON);
            object = new JSONObject();
            object.put("activeSensors", triggeredSensors);
            pushMessageAPI.sendPushMessage("Intrusion alert!", "Alarm sirens activated.", object.toString());
            log.warn("Alarm sirens activated: countdown ended");
            actionLogger.log(LogAction.SIRENS_ACTIVATED);
        }
    }
}
