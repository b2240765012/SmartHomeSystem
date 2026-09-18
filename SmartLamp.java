import java.time.LocalDateTime;

/**Represents a smart lamp with kelvin and brightness value.*/
public class SmartLamp extends SmartDevice {
    private int kelvin     = 4000;
    private int brightness = 100;

    SmartLamp(String name, boolean stateOn, int kelvin, int brightness) {
        super(name, stateOn);
        this.kelvin     = kelvin;
        this.brightness = brightness;
    }

    /**
     * Returns a status string including kelvin, brightness, and state.
     */
    @Override 
    String getStatus() {
        String strSwitchT = (getSwitchTime() == null) ? "null" : getSwitchTime().format(SmartHomeSystem.FORMATTER);
        String strStateOn = (getStateOn()) ? "on" : "off";
        return String.format("Smart Lamp %s is %s and its kelvin value is %dK " +
                        "with %d%% brightness, and its time to switch its status is %s.",
                getName(), strStateOn, kelvin, brightness, strSwitchT);
    }

    @Override void turnOn(LocalDateTime t)  {}
    @Override void turnOff(LocalDateTime t) {}

    // ── Getter and Setter Methods
    public int getKelvin()                    { return kelvin; }
    public int getBrightness()                { return brightness; }
    public void setKelvin(int kelvin)         { this.kelvin = kelvin; }
    public void setBrightness(int brightness) { this.brightness = brightness; }
}