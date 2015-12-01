package be.maartenvg;

import be.maartenvg.core.AlarmSystemCore;
import be.maartenvg.core.CoreException;
import be.maartenvg.io.arduino.Arduino;
import be.maartenvg.io.parse.ParsePush;
import be.maartenvg.io.parse.PushMessageAPI;
import be.maartenvg.io.peripherals.RotaryEncoder;
import com.pi4j.component.lcd.impl.GpioLcdDisplay;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CBus;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    public final static int LCD_ROWS = 2;
    public final static int LCD_COLUMNS = 16;

    public static void main(String[] args) throws IOException, InterruptedException, CoreException {
        System.out.print("Initializing... ");
        Arduino arduino = new Arduino(
                I2CBus.BUS_1,       // I²C Bus
                0x04,               // I²C Address
                2,                  // Amount of channels (reed switch, motion sensor)
                RaspiPin.GPIO_00    // Signal Pin
        );
        GpioLcdDisplay lcd = new GpioLcdDisplay(
                LCD_ROWS,          // number of row supported by LCD
                LCD_COLUMNS,       // number of columns supported by LCD
                RaspiPin.GPIO_11,  // LCD RS pin
                RaspiPin.GPIO_10,  // LCD strobe pin
                RaspiPin.GPIO_06,  // LCD data bit 1
                RaspiPin.GPIO_05,  // LCD data bit 2
                RaspiPin.GPIO_04,  // LCD data bit 3
                RaspiPin.GPIO_01   // LCD data bit 4
        );
        RotaryEncoder encoder = new RotaryEncoder(
                RaspiPin.GPIO_02,   // Clock
                RaspiPin.GPIO_03,   // Data
                RaspiPin.GPIO_07    // Switch
        );
        PushMessageAPI parsePush = new ParsePush("A3WoyobOV1mCpn9yJdF9k47mnTrjnfjJhkLgLXzm", "NzppDyI1i6RTWHWjsC5ml5OKljQyTlb2DOv3pNsm");

        AlarmSystemCore alarmSystemCore = new AlarmSystemCore(
                arduino,            // Arduino
                lcd,                // GpioLcdDisplay
                encoder,            // RotaryEncoder
                parsePush,          // PushMessageAPI
                new String[]{"Venster living", "Beweging living"}   // Sensor names
        );
        alarmSystemCore.start();
        System.out.println("Done!");

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        alarmSystemCore.stop();
    }

}