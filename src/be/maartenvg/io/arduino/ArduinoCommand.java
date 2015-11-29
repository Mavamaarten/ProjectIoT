package be.maartenvg.io.arduino;

public enum ArduinoCommand {
    ACKNOWLEDGE(0x00), ENABLE_WARNING_LED(0x01), DISABLE_WARNING_LED(0x02), ENABLE_SIREN(0x03), DISABLE_SIREN(0x04);

    private byte value;

    ArduinoCommand(int value) {
        this.value = (byte)value;
    }

    public byte getValue(){
        return value;
    }
}
