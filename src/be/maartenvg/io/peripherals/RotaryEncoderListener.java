package be.maartenvg.io.peripherals;

public interface RotaryEncoderListener {
    void onRotaryEncoderRotated(RotaryDirection rotaryDirection);
    void onRotaryEncoderClicked();
}
