package be.maartenvg.io.arduino;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static be.maartenvg.io.arduino.ArduinoCommand.ACKNOWLEDGE;

/**
 * Created by Maarten on 4/07/2015.
 */
public class Arduino {
    private List<ArduinoListener> listeners = new ArrayList();
    private int[] previousValues;
    private int[] values;
    private I2CDevice i2CDevice;
    private final GpioController gpio;
    private final GpioPinDigitalInput signalPin;

    public Arduino(int bus, int address, int channels, Pin signalpin) throws IOException {
        this.previousValues = new int[channels];
        this.values = new int[channels];
        this.gpio = GpioFactory.getInstance();

        I2CBus i2cbus = I2CFactory.getInstance(bus);
        this.i2CDevice = i2cbus.getDevice(address);

        signalPin = gpio.provisionDigitalInputPin(signalpin, PinPullResistance.PULL_DOWN);
        signalPin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(event.getState() == PinState.HIGH) readValues();
            }
        });
    }

    public void addListener(ArduinoListener listener){
        listeners.add(listener);
    }

    private void notifyListeners(int channel, int value){
        for(ArduinoListener listener : listeners){
            listener.onArduinoValueChanged(channel, value);
        }
    }

    private void notifyListeners(int[] values){
        for(ArduinoListener listener : listeners){
            listener.onArduinoValuesChanged(values);
        }
    }



    public int[] getValues(){
        return values;
    }

    public synchronized void sendCommand(ArduinoCommand command){
        try {
            i2CDevice.write(command.getValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void acknowledge(){
        try {
            i2CDevice.write(ACKNOWLEDGE.getValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void readValues(){
        try {
            for(int i=0; i<values.length; i++){
                i2CDevice.write((byte) (i + 5)); // 1 - 5 are reserved for arduino control messages
                Thread.sleep(30);
                values[i] = i2CDevice.read();
                if(values[i] != previousValues[i]) notifyListeners(i, values[i]);
                previousValues[i] = values[i];
            }

            acknowledge();
            notifyListeners(values);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}