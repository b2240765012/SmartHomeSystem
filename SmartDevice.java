import java.time.LocalDateTime;

/**Base abstract class for all smart devices in the system.*/
public abstract class SmartDevice {
    private String name;
    private boolean stateOn;
    private LocalDateTime switchTime;
    private int actionOrder;

    SmartDevice(String name, boolean stateOn) {
        this.name = name;
        this.stateOn = stateOn;
    }

    abstract void turnOn(LocalDateTime time);
    abstract void turnOff(LocalDateTime time);

    /**
     * Switches the current state of the device.
     * @param t The time variable of the operation.
     */
    void switchStatus(LocalDateTime t) {
        if (stateOn) {
            turnOff(t);
            stateOn = false;
        } else {
            stateOn = true;
            turnOn(t);
        }
    }

    /**Sets the device to a specific state.*/
    public void switchTo(boolean stateOn, LocalDateTime time) {
        if (this.stateOn && !stateOn) {
            turnOff(time);
            this.stateOn = false;
        } else if (!this.stateOn && stateOn) {
            this.stateOn = true;
            turnOn(time);
        }
    }

    /** Returns formatted device status information. */
    abstract String getStatus();

    // ── Getter and Setter Methods
    public String getName()                       { return name; }
    public boolean getStateOn()                   { return stateOn; }
    public LocalDateTime getSwitchTime()          { return switchTime; }
    public int getActionOrder()                   { return actionOrder; }
    public void setName(String name)              { this.name = name; }
    public void setStateOn(boolean stateOn)      { this.stateOn = stateOn; }
    public void setSwitchTime(LocalDateTime time) { this.switchTime = time; }
    public void setActionOrder(int order)         { this.actionOrder = order; }
}