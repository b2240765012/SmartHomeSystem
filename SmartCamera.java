import java.time.LocalDateTime;

/** The smart camera class that shows storage usage in MB.*/
public class SmartCamera extends SmartDevice {
    private double mbPerMin;
    private double usedMB = 0;
    private LocalDateTime lastOnTime;

    SmartCamera(String name, boolean stateOn, double mbPerMin, LocalDateTime now) {
        super(name, stateOn);
        this.mbPerMin = mbPerMin;
        this.lastOnTime = stateOn ? now : null;
    }

    @Override 
    void turnOn(LocalDateTime t) { 
        lastOnTime = t; 
    }

    /**Calculates and updates the total storage used when the camera is turned off.*/
    @Override 
    void turnOff(LocalDateTime t) {
        if (lastOnTime != null) {
            usedMB += mbPerMin * SmartHomeSystem.seconds(lastOnTime, t) / 60.0;
            lastOnTime = null;
        }
    }

    /**Return the status of the camera with usedMB value in storage*/
    @Override 
    String getStatus() {
        String strSwitchT = (getSwitchTime() == null) ? "null" : getSwitchTime().format(SmartHomeSystem.FORMATTER);
        String strStateOn = (getStateOn()) ? "on" : "off";
        return String.format("Smart Camera %s is %s and used %.2f MB of storage so far " +
                        "(excluding present usage), and its time to switch its status is %s.",
                getName(), strStateOn, usedMB, strSwitchT);
    }

    // ── Getter and Setter Methods
    public double getUsedMB() { return usedMB; }
    public double getMBPerMin() { return mbPerMin; }
    public LocalDateTime getLastOnTime() { return lastOnTime; }
    public void setMBPerMin(double mbPerMin) { this.mbPerMin = mbPerMin; }
}