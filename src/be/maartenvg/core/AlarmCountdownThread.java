package be.maartenvg.core;

import be.maartenvg.io.arduino.Arduino;
import be.maartenvg.io.arduino.ArduinoCommand;
import com.pi4j.component.lcd.LCDTextAlignment;
import com.pi4j.component.lcd.impl.GpioLcdDisplay;

public class AlarmCountdownThread extends Thread {
    private static final int LCD_ROW_1 = 0;
    private static final int LCD_ROW_2 = 1;

    private final AlarmSystemCore alarmSystemCore;
    private final GpioLcdDisplay lcd;
    private final Arduino arduino;
    private final int delay;

    public AlarmCountdownThread(AlarmSystemCore alarmSystemCore, GpioLcdDisplay lcd, Arduino arduino, int delay) {
        this.alarmSystemCore = alarmSystemCore;
        this.lcd = lcd;
        this.arduino = arduino;
        this.delay = delay;
    }

    @Override
    public void run() {
        int countdown = delay / 1000;
        arduino.sendCommand(ArduinoCommand.ENABLE_WARNING_LED);
        alarmSystemCore.setStatus(AlarmStatus.COUNTDOWN);

        while(!interrupted() && countdown > 0){
            lcd.writeln(LCD_ROW_1, "COUNTDOWN");
            lcd.write(LCD_ROW_1, String.valueOf(countdown), LCDTextAlignment.ALIGN_RIGHT);
            System.out.println("Countdown... " + countdown);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Countdown aborted (!)");
                break;
            }
            countdown--;
        }

        if(!interrupted()){
            arduino.sendCommand(ArduinoCommand.ENABLE_SIREN);
            alarmSystemCore.setStatus(AlarmStatus.SIRENS_ON);
        }
    }
}
