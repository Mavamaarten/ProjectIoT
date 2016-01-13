package be.maartenvg.io.arduino;

/**
 * Created by Maarten on 4/07/2015.
 */
public interface ArduinoListener {
    void onArduinoValueChanged(int channel, int value);
    void onSensorReadingsChanged(int[] values);
}
