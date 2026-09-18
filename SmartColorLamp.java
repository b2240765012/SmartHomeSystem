import java.time.LocalDateTime;

/**The subclass of SmartLamp that supports color codes.*/
public class SmartColorLamp extends SmartLamp {
    private String colorCode;

    SmartColorLamp(String name, boolean stateOn, int kelvin, int brightness, String colorCode) {
        super(name, stateOn, kelvin, brightness);
        this.colorCode = colorCode;
    }

    /**Returns the status string with color code value or kelvin value.*/
    @Override 
    String getStatus() {
        String strSwitchT = (getSwitchTime() == null) ? "null" : getSwitchTime().format(SmartHomeSystem.FORMATTER);
        String colorValue = (colorCode != null) ? colorCode : getKelvin() + "K";
        String strStateOn = (getStateOn()) ? "on" : "off";
        return String.format("Smart Color Lamp %s is %s and its color value is %s " +
                        "with %d%% brightness, and its time to switch its status is %s.",
                getName(), strStateOn, colorValue, getBrightness(), strSwitchT);
    }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
}