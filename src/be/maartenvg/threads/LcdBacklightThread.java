package be.maartenvg.threads;

import be.maartenvg.settings.Settings;
import com.pi4j.component.lcd.impl.I2CLcdDisplay;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LcdBacklightThread extends Thread {
    private final I2CLcdDisplay lcd;
    private final Settings settings = Settings.getInstance();
    private final Log log = LogFactory.getLog(LcdBacklightThread.class);

    public LcdBacklightThread(I2CLcdDisplay lcd) {
        this.lcd = lcd;
    }

    @Override
    public void run() {
        try{
            lcd.setBacklight(true);
            Thread.sleep(settings.getBacklightTimeout());
            lcd.setBacklight(false);
            log.info("Backlight thread finished: backlight turned off.");
        } catch(InterruptedException e){
            log.info("Backlight thread interrupted");
        }
    }
}