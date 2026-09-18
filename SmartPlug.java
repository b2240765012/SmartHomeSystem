import java.time.LocalDateTime;

/**Represents a smart plug that measures energy consumption.*/
public class SmartPlug extends SmartDevice {
    private double ampere = -1;
    private double energy  = 0;
    private LocalDateTime lastOnTime;

    SmartPlug(String name, boolean stateOn, double ampere, LocalDateTime now) {
        super(name, stateOn);
        this.ampere  = ampere;
        this.lastOnTime = stateOn ? now : null;
    }

    @Override 
    void turnOn(LocalDateTime time) { 
        lastOnTime = time; 
    }

    /**Calculates energy consumption when the device is turned off.*/
    @Override 
    void turnOff(LocalDateTime time) {
        if (lastOnTime != null && ampere > 0) {
            energy += 220.0 * ampere * SmartHomeSystem.seconds(lastOnTime, time) / 3600.0;
            lastOnTime = null;
        }
    }

    /**Updates the ampere value and adds the energy consumed*/
    public void changeAmpere(double newAmpere, LocalDateTime time) {
        if (getStateOn() && lastOnTime != null && ampere > 0) {
            energy += 220.0 * ampere * SmartHomeSystem.seconds(lastOnTime, time) / 3600.0;
        }
        ampere = newAmpere;
        lastOnTime = getStateOn() ? time : null;
    }

    /** Return the status of the plug device and the energy consumption value*/
    @Override 
    String getStatus() {
        String strSwitchT = (getSwitchTime() == null) ? "null" : getSwitchTime().format(SmartHomeSystem.FORMATTER);
        String strStateOn = (getStateOn()) ? "on" : "off";
        return String.format("Smart Plug %s is %s and consumed %.2fW so far " +
                        "(excluding present usage), and its time to switch its status is %s.",
                getName(), strStateOn, energy, strSwitchT);
    }

    // ── Getter Method
    public double getAmpere() { return ampere; }
    public double getEnergy() { return energy; }
    public LocalDateTime getLastOnTime() { return lastOnTime; }
}