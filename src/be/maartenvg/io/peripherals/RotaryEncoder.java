package be.maartenvg.io.peripherals;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.util.ArrayList;
import java.util.List;

public class RotaryEncoder {
    private Pin clk, data, sw;
    private final GpioController gpio = GpioFactory.getInstance();
    private final GpioPinDigitalInput clkPin, dataPin, swPin;
    private final List<RotaryEncoderListener> listeners = new ArrayList<>();

    public RotaryEncoder(Pin clk, Pin data, Pin sw) {
        this.clk = clk;
        this.data = data;
        this.sw = sw;

        clkPin = gpio.provisionDigitalInputPin(clk, PinPullResistance.OFF);
        dataPin = gpio.provisionDigitalInputPin(data, PinPullResistance.OFF);
        swPin = gpio.provisionDigitalInputPin(sw, PinPullResistance.OFF);

        clkPin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(event.getState().isHigh()){
                    PinState data = dataPin.getState();

                    if(data.isHigh()){
                        notifyListeners(RotaryDirection.RIGHT);
                    } else {
                        notifyListeners(RotaryDirection.LEFT);
                    }
                }
            }
        });

        swPin.setDebounce(30);
        swPin.addListener(new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(event.getState().isLow()) notifyListeners();
            }
        });
    }

    private void notifyListeners(RotaryDirection direction){
        for(RotaryEncoderListener listener : listeners){
            listener.onRotaryEncoderRotated(direction);
        }
    }

    private void notifyListeners(){
        for(RotaryEncoderListener listener : listeners){
            listener.onRotaryEncoderClicked();
        }
    }

    public void addListener(RotaryEncoderListener listener){
        listeners.add(listener);
    }

    public void removeListeners(){
        listeners.clear();
    }

}
