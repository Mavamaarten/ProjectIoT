package be.maartenvg;

import be.maartenvg.core.AlarmSystemCore;
import be.maartenvg.core.CoreException;
import be.maartenvg.io.arduino.Arduino;
import be.maartenvg.io.parse.ParsePush;
import be.maartenvg.io.parse.PushMessageAPI;
import be.maartenvg.io.peripherals.RotaryEncoder;
import com.pi4j.component.lcd.impl.I2CLcdDisplay;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CBus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Scanner;

public class Main {
    public final static int LCD_ROWS = 2;
    public final static int LCD_COLUMNS = 16;
    public final static Log log = LogFactory.getLog(Main.class);

    public static void main(String[] args) throws Exception, CoreException {
        log.info("Initializing LCD...         ");
        I2CLcdDisplay lcd = new I2CLcdDisplay(LCD_ROWS, LCD_COLUMNS, I2CBus.BUS_1, 0x3f, 3, 0, 1, 2, 7, 6, 5, 4);
        lcd.clear();
        lcd.writeln(0, "SmartAlarm");
        lcd.writeln(1, "Initializing...");

        log.info("Initializing Arduino communication...     ");
        Arduino arduino = new Arduino(
                I2CBus.BUS_1,       // I²C Bus
                0x04,               // I²C Address
                2,                  // Amount of channels (reed switch, motion sensor)
                RaspiPin.GPIO_00    // Signal Pin
        );

        log.info("Initializing peripherals... ");
        RotaryEncoder encoder = new RotaryEncoder(
                RaspiPin.GPIO_02,   // Clock
                RaspiPin.GPIO_03,   // Data
                RaspiPin.GPIO_07    // Switch
        );
        log.info("Initializing push message API... ");
        PushMessageAPI parsePush = new ParsePush("A3WoyobOV1mCpn9yJdF9k47mnTrjnfjJhkLgLXzm", "NzppDyI1i6RTWHWjsC5ml5OKljQyTlb2DOv3pNsm");

        log.info("Initializing core...        ");
        AlarmSystemCore alarmSystemCore = new AlarmSystemCore(
                arduino,            // Arduino
                lcd,                // GpioLcdDisplay
                encoder,            // RotaryEncoder
                parsePush,          // PushMessageAPI
                new String[]{"Venster living", "Beweging living"}   // Sensor names
        );
        alarmSystemCore.start();

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        alarmSystemCore.stop();
    }

}