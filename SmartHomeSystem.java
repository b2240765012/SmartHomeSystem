import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
/**
 * Main controller for the Smart Home System.
 * Handles the reading the command from input file and writing output to output file.*/
public class SmartHomeSystem {
    static final DateTimeFormatter FORMATTER =DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    static List<SmartDevice> devices = new ArrayList<>();
    static LocalDateTime now;
    static PrintWriter output;
    static int action = 0;
    static boolean lastZReport = false;

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        Scanner scanner = new Scanner(new File(args[0]));
        output = new PrintWriter(new FileWriter(args[1]));

        ArrayList<String> lines = new ArrayList<>();
        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine());
        }
        scanner.close();

        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).trim().isEmpty()) {
                start = i;break;
            }
        }
        if (start == -1) { output.close(); return; }

        String firstCommand = lines.get(start).trim();
        String[] parts = firstCommand.split("\t");
        // Validation part for first command
        try {
            if (!parts[0].trim().equals("SetInitialTime") || parts.length < 2) {
                throw new CommandControlException("First command must be set initial time! Program is going to terminate!");
            }
            if (!SmartHomeSystem.validDate(parts[1].trim())) {throw new CommandControlException("Format of the initial date is wrong! Program is going to terminate!");}
            now = LocalDateTime.parse(parts[1].trim(), SmartHomeSystem.FORMATTER);
            output.println("COMMAND: " + firstCommand);
            output.println("SUCCESS: Time has been set to " + now.format(FORMATTER) + "!");
        } catch (CommandControlException e) {
            output.println("COMMAND: " + firstCommand);
            output.println("ERROR: " + e.getMessage());
            output.close();
            return;
        }

        for (int i = start+ 1; i < lines.size(); i++) {
            if (!lines.get(i).trim().isEmpty())
                runCommand(lines.get(i).trim());
        }

        if (!lastZReport){finalZReport();}
        output.close();
    }

    static String erroneous() {return"ERROR: Erroneous command!";}
    static String noDevice() {return"ERROR: There is not such a device!";}
    static String sameName() {return"ERROR: There is already a smart device with same name!";}

    /** Reads and run commands respective to their handlers.*/
    static void runCommand(String line){
        String[] components = line.split("\t");
        String command = components[0].trim();
        output.println("COMMAND: " + line);
        SmartHomeSystem.lastZReport = false;
        try{
            switch (command) {
                case "SetTime":         setTime(components);            break;
                case "SkipMinutes":     skipMinutes(components);        break;
                case "Nop":             nop(components);                break;
                case "Add":             add(components);                break;
                case "Remove":          remove(components);             break;
                case "SetSwitchTime":   setSwitchTime(components);      break;
                case "Switch":          switchState(components);        break;
                case "ChangeName":      changeName(components);         break;
                case "PlugIn":          plugIn(components);             break;
                case "PlugOut":         plugOut(components);            break;
                case "SetKelvin":       setKelvin(components);          break;
                case "SetBrightness":   setBrightness(components);      break;
                case "SetColorCode":    setColorCode(components);       break;
                case "SetWhite":        setWhite(components);           break;
                case "SetColor":        setColor(components);           break;
                case "ZReport":
                    if (components.length != 1) {throw new CommandControlException(erroneous());}
                    zReport();
                    SmartHomeSystem.lastZReport = true; break;
                case "SetInitialTime":
                default:
                    throw new CommandControlException(erroneous());
            }
        }catch (CommandControlException e){output.println(e.getMessage());}
    }

    static void zReport() {
        output.println("Time is:\t" + now.format(FORMATTER));
        for (SmartDevice d : sorted()) {output.println(d.getStatus());}
    }

    static void finalZReport() {
        output.println("ZReport:");
        output.println("Time is:\t" + (now != null ? now.format(FORMATTER) : ""));
        for (SmartDevice d : sorted()) {output.println(d.getStatus());}
    }

    /** Sorts devices by switch time or action order. Devices with a switchTime are listed first,sorted in ascending order from
     * the earliest time to the latest.If their switch times are same,they are then sorted by their addition order.
     * Devices without a switchTime are placed at the bottom of the list,where they are firstly sorted by their actionOrder variables.
     * If their action orders are same,they are then sorted by their addition order.*/
    static List<SmartDevice> sorted() {
        List<SmartDevice> withSwitchTime = new ArrayList<>();
        List<SmartDevice> noSwitchTime   = new ArrayList<>();

        for (SmartDevice device : devices) {
            if (device.getSwitchTime() != null) {
                withSwitchTime.add(device);
            } else {
                noSwitchTime.add(device);
            }
        }
        for (int i = 0; i < withSwitchTime.size() - 1; i++) {
            for (int j = i + 1; j < withSwitchTime.size(); j++) {
                SmartDevice a = withSwitchTime.get(i);
                SmartDevice b = withSwitchTime.get(j);

                int timeDiff = a.getSwitchTime().compareTo(b.getSwitchTime());
                boolean earlierB = timeDiff > 0;
                boolean bAddedFirst = (timeDiff == 0) && (devices.indexOf(a) > devices.indexOf(b));

                if (earlierB || bAddedFirst) {
                    withSwitchTime.set(i, b); withSwitchTime.set(j, a);
                }
            }
        }
        // Sort devices without a switch time: most recently acted-on goes first.
        // If two devices share the same actionOrder, the one added earlier stays first.
        for (int i = 0; i < noSwitchTime.size() - 1; i++) {
            for (int j = i + 1; j < noSwitchTime.size(); j++) {
                SmartDevice a = noSwitchTime.get(i);
                SmartDevice b = noSwitchTime.get(j);

                boolean lastActionB = a.getActionOrder() < b.getActionOrder();
                boolean bAddedFirst = (a.getActionOrder() == b.getActionOrder()) && (devices.indexOf(a) > devices.indexOf(b));

                if (lastActionB || bAddedFirst) {
                    noSwitchTime.set(i, b); noSwitchTime.set(j, a);
                }
            }
        }
        withSwitchTime.addAll(noSwitchTime);
        return withSwitchTime;
    }

    /** Handles simulation time progression and triggers scheduled events.This method monitors the switchTime of devices
     * between the current time and the target time,executing them in the appropriate order.*/
    static void advanceTime(LocalDateTime target) {
        while (true) {
            // It finds the earliest switch time between now and target
            LocalDateTime nextSwitch = null;
            for (SmartDevice device : devices) {
                if (device.getSwitchTime() == null) {continue;}

                boolean afterNow    = device.getSwitchTime().isAfter(now);
                boolean beforeTarget = !device.getSwitchTime().isAfter(target);

                if (afterNow && beforeTarget) {
                    if (nextSwitch == null || device.getSwitchTime().isBefore(nextSwitch)) {
                        nextSwitch = device.getSwitchTime();
                    }
                }
            }
            if (nextSwitch == null){ break;}
            // All devices switching at the same instant share one actionOrder.
            int switchOrder = ++action;

            for (SmartDevice device : new ArrayList<>(devices)) {
                if ((device.getSwitchTime()!= null) && (device.getSwitchTime().equals(nextSwitch))) {
                    device.switchStatus(nextSwitch);
                    device.setSwitchTime(null);
                    device.setActionOrder(switchOrder);
                }
            }
            now = nextSwitch;
        }
        now = target;
    }

    static void setTime(String[] command) throws CommandControlException {
        if (command.length != 2) {throw new CommandControlException(erroneous());}
        LocalDateTime time;
        if (!SmartHomeSystem.validDate(command[1].trim())){throw new CommandControlException("ERROR: Time format is not correct!");}
        time = LocalDateTime.parse(command[1].trim(), SmartHomeSystem.FORMATTER);
        if (time.isBefore(now)) {throw new CommandControlException("ERROR: Time cannot be reversed!");}
        if (time.equals(now)) {throw new CommandControlException("ERROR: There is nothing to change!");}
        advanceTime(time);
    }

    static void skipMinutes(String[] command) throws CommandControlException {
        if (command.length != 2) {throw new CommandControlException(erroneous());}
        int minutes;
        try {
            minutes = Integer.parseInt(command[1].trim());
        } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
        if (minutes < 0) {throw new CommandControlException("ERROR: Time cannot be reversed!");}
        if (minutes == 0) {throw new CommandControlException("ERROR: There is nothing to skip!");}
        advanceTime(now.plusMinutes(minutes));
    }
    /**This method advances the time to the nearest switchTime point.**/
    static void nop(String[] command) throws CommandControlException {
        if (command.length != 1) {throw new CommandControlException(erroneous());}
        LocalDateTime earliest = null;
        for (SmartDevice device : devices)
            if (device.getSwitchTime() != null && (earliest == null || device.getSwitchTime().isBefore(earliest)))
                earliest = device.getSwitchTime();

        if (earliest == null) {throw new CommandControlException("ERROR: There is nothing to switch!");}
        advanceTime(earliest);
    }
    /**Routes the command to the specific add method based on the device type.*/
    static void add(String[] command) throws CommandControlException {
        if (command.length < 3) {throw new CommandControlException(erroneous());}
        String type = command[1].trim();
        String name = command[2].trim();
        switch (type) {
            case "SmartPlug":   addPlug(command, name);             break;
            case "SmartCamera": addCamera(command, name);           break;
            case "SmartLamp":   addLamp(command, name);             break;
            case "SmartColorLamp": addColorLamp(command, name);     break;
            default:
                throw new CommandControlException(erroneous());
        }
    }

    static void addPlug(String[] command, String name) throws CommandControlException {
        if (find(name) != null) {throw new CommandControlException(sameName());}
        boolean stateOn = false;
        double ampere = -1;
        if (command.length > 3) {
            if (!command[3].trim().equals("On") && !command[3].trim().equals("Off")) {
                throw new CommandControlException(erroneous());
            }
            stateOn = command[3].trim().equals("On");
        }
        if (command.length > 4) {
            try {
                ampere = Double.parseDouble(command[4].trim());
            } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
            if (ampere <= 0) {throw new CommandControlException("ERROR: Ampere value must be a positive number!");}
        }
        if (command.length > 5) {throw new CommandControlException(erroneous());}
        devices.add(new SmartPlug(name, stateOn, ampere, now));
    }

    static void addCamera(String[] command, String name) throws CommandControlException {
        if (command.length < 4 || command.length > 5) {throw new CommandControlException(erroneous());}
        if (find(name) != null) {throw new CommandControlException(sameName());}
        double mb;
        try {
            mb = Double.parseDouble(command[3].trim());
        } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
        if (mb <= 0) {throw new CommandControlException("ERROR: Megabyte value must be a positive number!");}
        boolean stateOn = false;
        if (command.length == 5) {
            if (!command[4].trim().equals("On") && !command[4].trim().equals("Off")) {throw new CommandControlException(erroneous());}
            stateOn = command[4].trim().equals("On");
        }
        devices.add(new SmartCamera(name, stateOn, mb, now));
    }

    static void addLamp(String[] command, String name) throws CommandControlException {
        if (find(name) != null) { throw new CommandControlException(sameName());}
        boolean stateOn = false;
        int kelvin = 4000, brightness = 100;
        if (command.length > 3) {
            if (!command[3].trim().equals("On") && !command[3].trim().equals("Off")) {throw new CommandControlException(erroneous());}
            stateOn = command[3].trim().equals("On");
        }
        if (command.length == 6) {
            try {
                kelvin = Integer.parseInt(command[4].trim());
                brightness = Integer.parseInt(command[5].trim());
            } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
            if (kelvin < 2000 || kelvin > 6500) {throw new CommandControlException("ERROR: Kelvin value must be in range of 2000K-6500K!");}
            if (brightness < 0 || brightness > 100) {throw new CommandControlException("ERROR: Brightness must be in range of 0%-100%!");}
        }
        else if (command.length != 3 && command.length != 4) {throw new CommandControlException(erroneous());}
        devices.add(new SmartLamp(name, stateOn, kelvin, brightness));
    }

    static void addColorLamp(String[] command, String name) throws CommandControlException {
        if (find(name) != null) { throw new CommandControlException(sameName());}
        boolean stateOn = false;
        int kelvin = 4000, brightness = 100;
        String colorCode = null;

        if (command.length > 3) {
            if (!command[3].trim().equals("On") && !command[3].trim().equals("Off")) {throw new CommandControlException(erroneous());}
            stateOn = command[3].trim().equals("On");
        }
        if (command.length == 6) {
            String colorValue = command[4].trim();
            try {
                brightness = Integer.parseInt(command[5].trim());
            } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}

            if (brightness < 0 || brightness > 100) {throw new CommandControlException("ERROR: Brightness must be in range of 0%-100%!");}
            if (colorValue.startsWith("0x")) {
                if (SmartHomeSystem.wrongHex(colorValue)) {throw new CommandControlException(erroneous());}
                if (!SmartHomeSystem.validHex(colorValue)) {throw new CommandControlException("ERROR: Color code value must be in range of 0x0-0xFFFFFF!");}
                colorCode = normalizeHex(colorValue);
            } else {
                try {
                    kelvin = Integer.parseInt(colorValue);
                } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
                if (kelvin < 2000 || kelvin > 6500) {throw new CommandControlException("ERROR: Kelvin value must be in range of 2000K-6500K!");}
            }
        } else if (command.length != 3 && command.length != 4) {throw new CommandControlException(erroneous());}
        devices.add(new SmartColorLamp(name, stateOn, kelvin, brightness, colorCode));
    }

    static void remove(String[] command) throws CommandControlException {
        if (command.length != 2) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        if (device.getStateOn()) {
            device.turnOff(now);
            device.setStateOn(false);
            device.setSwitchTime(null);
        }
        output.println("SUCCESS: Information about removed smart device is as follows:");
        output.println(device.getStatus());
        devices.remove(device);
    }
    /** "This method  changes the state of items in the system on a scheduled basis."**/
    static void setSwitchTime(String[] command) throws CommandControlException {
        if (command.length != 3) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) { throw new CommandControlException(noDevice());}
        LocalDateTime switchT;
        if (!SmartHomeSystem.validDate(command[2].trim())) {throw new CommandControlException(erroneous());}

        switchT = LocalDateTime.parse(command[2].trim(), SmartHomeSystem.FORMATTER);
        if (switchT.isBefore(now)) {
            throw new CommandControlException("ERROR: Switch time cannot be in the past!");}
        if (switchT.equals(now)) {
            device.switchStatus(now);
            device.setSwitchTime(null);
            device.setActionOrder(++action);
            return;
        }
        device.setSwitchTime(switchT);
        device.setActionOrder(++action);
    }

    static void switchState(String[] command) throws CommandControlException {
        if (command.length != 3) {throw new CommandControlException(erroneous());}
        String state = command[2].trim();
        if (!state.equals("On") && !state.equals("Off")) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        boolean stateOn = state.equals("On");
        if (device.getStateOn() == stateOn) {throw new CommandControlException("ERROR: This device is already switched " + state.toLowerCase() + "!");}
        device.switchTo(stateOn, now);
        device.setSwitchTime(null);
        device.setActionOrder(++action);
    }

    static void changeName(String[] command) throws CommandControlException {
        if (command.length != 3) {throw new CommandControlException(erroneous());}
        String oldName = command[1].trim(), newName = command[2].trim();
        if (oldName.equals(newName)) {throw new CommandControlException("ERROR: Both of the names are the same, nothing changed!");}
        SmartDevice device = find(oldName);
        if (device == null) {throw new CommandControlException(noDevice());}
        if (find(newName) != null) {throw new CommandControlException(sameName());}
        device.setName(newName);
    }

    static void plugIn(String[] command) throws CommandControlException {
        if (command.length != 3) {throw new CommandControlException(erroneous());}
        double ampere;
        try {
            ampere = Double.parseDouble(command[2].trim());
        } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        if (!(device instanceof SmartPlug)) {throw new CommandControlException("ERROR: This device is not a smart plug!");}
        SmartPlug plug = (SmartPlug) device;
        if (plug.getAmpere() >= 0) {throw new CommandControlException("ERROR: There is already an item plugged in to that plug!");}
        if (ampere <= 0) {throw new CommandControlException("ERROR: Ampere value must be a positive number!");}
        plug.changeAmpere(ampere, now);
        plug.setActionOrder(++action);
    }

    static void plugOut(String[] command) throws CommandControlException {
        if (command.length != 2) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        if (!(device instanceof SmartPlug)) {throw new CommandControlException("ERROR: This device is not a smart plug!");}
        SmartPlug plug = (SmartPlug) device;
        if (plug.getAmpere() < 0) {throw new CommandControlException("ERROR: This plug has no item to plug out from that plug!");}
        plug.changeAmpere(-1, now);
        plug.setActionOrder(++action);
    }
    
    //Set command methods that updates the values of the smart device based on the provided command parameters
    static void setKelvin(String[] command) throws CommandControlException {
        if (command.length != 3) { throw new CommandControlException(erroneous());}
        int kelvin;
        try {
            kelvin = Integer.parseInt(command[2].trim());
        } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        if (!(device instanceof SmartLamp)) {throw new CommandControlException("ERROR: This device is not a smart lamp!");}
        if (kelvin < 2000 || kelvin > 6500) {throw new CommandControlException("ERROR: Kelvin value must be in range of 2000K-6500K!");}
        ((SmartLamp) device).setKelvin(kelvin);
        if (device instanceof SmartColorLamp) {
            ((SmartColorLamp) device).setColorCode(null);
        }
    }

    static void setBrightness(String[] command) throws CommandControlException {
        if (command.length != 3) {throw new CommandControlException(erroneous());}
        int brightness;
        try {
            brightness = Integer.parseInt(command[2].trim());
        } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        if (!(device instanceof SmartLamp)) {throw new CommandControlException("ERROR: This device is not a smart lamp!");}
        if (brightness < 0 || brightness > 100) {throw new CommandControlException("ERROR: Brightness must be in range of 0%-100%!");}
        ((SmartLamp) device).setBrightness(brightness);
    }

    static void setColorCode(String[] command) throws CommandControlException {
        if (command.length != 3) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        if (!(device instanceof SmartColorLamp)) {throw new CommandControlException("ERROR: This device is not a smart color lamp!");}
        String colorCode = command[2].trim();
        if (!colorCode.startsWith("0x") || SmartHomeSystem.wrongHex(colorCode)) {throw new CommandControlException(erroneous());}
        if (!SmartHomeSystem.validHex(colorCode)) {throw new CommandControlException("ERROR: Color code value must be in range of 0x0-0xFFFFFF!");}
        ((SmartColorLamp) device).setColorCode(normalizeHex(colorCode));
    }

    static void setWhite(String[] command) throws CommandControlException {
        if (command.length != 4) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        if (!(device instanceof SmartLamp)) {throw new CommandControlException("ERROR: This device is not a smart lamp!");}
        int kelvin, brightness;
        try {
            kelvin = Integer.parseInt(command[2].trim());
            brightness = Integer.parseInt(command[3].trim());
        } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
        if (kelvin < 2000 || kelvin > 6500) {throw new CommandControlException("ERROR: Kelvin value must be in range of 2000K-6500K!");}
        if (brightness < 0 || brightness > 100) {throw new CommandControlException("ERROR: Brightness must be in range of 0%-100%!");}
        SmartLamp lamp = (SmartLamp) device;
        lamp.setKelvin(kelvin);
        lamp.setBrightness(brightness);
        if (lamp instanceof SmartColorLamp) {
            ((SmartColorLamp) lamp).setColorCode(null);
        }
    }

    static void setColor(String[] command) throws CommandControlException {
        if (command.length != 4) {throw new CommandControlException(erroneous());}
        SmartDevice device = find(command[1].trim());
        if (device == null) {throw new CommandControlException(noDevice());}
        if (!(device instanceof SmartColorLamp)) {throw new CommandControlException("ERROR: This device is not a smart color lamp!");}
        String code = command[2].trim();
        int brightness;
        try {
            brightness = Integer.parseInt(command[3].trim());
        } catch (NumberFormatException e) {throw new CommandControlException(erroneous());}
        if (!code.startsWith("0x") || SmartHomeSystem.wrongHex(code)) {throw new CommandControlException(erroneous());}
        if (!SmartHomeSystem.validHex(code)) {throw new CommandControlException("ERROR: Color code value must be in range of 0x0-0xFFFFFF!");}
        if (brightness < 0 || brightness > 100) {throw new CommandControlException("ERROR: Brightness must be in range of 0%-100%!");}
        SmartColorLamp colorLamp = (SmartColorLamp) device;
        colorLamp.setColorCode(normalizeHex(code));
        colorLamp.setBrightness(brightness);
    }
    
    /** Calculates the difference between two LocalDateTime objects in seconds. */
    static long seconds(LocalDateTime a, LocalDateTime b) {
        return java.time.temporal.ChronoUnit.SECONDS.between(a, b);
    }
    static boolean validDate(String s) {
        if (!s.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}:\\d{2}:\\d{2}")) return false;
        try { LocalDateTime.parse(s, SmartHomeSystem.FORMATTER); return true; }
        catch (Exception e) { return false; }
    }
    /**Validation method for controlling 6 digit character.**/
    static boolean validHex(String s) {
        return s.matches("(?i)0x[0-9A-Fa-f]{1,6}");}
    /** Validation method for wrong hex character**/
    static boolean wrongHex(String s) {
        return s.startsWith("0x") && !s.substring(2).matches("[0-9A-Fa-f]+");}

    /**Zero-pads a valid hex color code to 6 digits.(0xFF=0x0000FF)**/
    static String normalizeHex(String s) {
        String digits = s.substring(2).toUpperCase();
        while (digits.length() < 6) digits = "0" + digits;
        return "0x" + digits;
    }

    static SmartDevice find(String name) {
        for (SmartDevice device : devices) {
            if (device.getName().equals(name)) {
                return device;
            }
        }
        return null;
    }
}