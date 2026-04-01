package at.jku.se.smarthome.model;

public class Device {
    private final String id;
    private String name;
    private final DeviceType type;
    private boolean isOn;
    private double value;
    private String unit;

    public Device(String id, String name, DeviceType type) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Device id must not be empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Device type must not be null");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.type = type;

        initializeDefaults();
    }

    private void initializeDefaults() {
        switch (type) {
            case SWITCH -> {
                this.isOn = false;
                this.value = 0;
                this.unit = "";
            }
            case DIMMER -> {
                this.isOn = false;
                this.value = 0;
                this.unit = "%";
            }
            case THERMOSTAT -> {
                this.isOn = true;
                this.value = 20.0;
                this.unit = "°C";
            }
            case BLIND -> {
                this.isOn = false;
                this.value = 0;
                this.unit = "%";
            }
            case SENSOR -> {
                this.isOn = false;
                this.value = 0;
                this.unit = "";
            }
            default -> throw new IllegalStateException("Unexpected device type: " + type);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DeviceType getType() {
        return type;
    }

    public boolean isOn() {
        return isOn;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }


    //Gerät umbennen
    public void rename (String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Device name must not be empty!");
        }
        this.name = newName.trim();
    }

    public void toggle() {
        if (type != DeviceType.SWITCH) {
            throw new IllegalStateException("Toggle is only supported for SWITCH devices");
        }
        this.isOn = !this.isOn;
    }

    public void setValue(double value) {
        switch (type) {
            case DIMMER -> setDimmerValue(value);
            case THERMOSTAT -> setThermostatValue(value);
            case BLIND -> setBlindValue(value);
            case SENSOR -> setSensorValue(value);
            case SWITCH -> throw new IllegalStateException("SWITCH does not support numeric values");
            default -> throw new IllegalStateException("Unexpected device type: " + type);
        }
    }

    private void setDimmerValue(double value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Dimmer value must be between 0 and 100");
        }
        this.value = value;
        this.isOn = value > 0;
    }

    private void setThermostatValue(double value) {
        this.value = value;
        this.isOn = true;
    }

    private void setBlindValue(double value) {
        if (value != 0 && value != 100) {
            throw new IllegalArgumentException("Blind value must be 0 or 100");
        }
        this.value = value;
        this.isOn = value == 100;
    }

    private void setSensorValue(double value) {
        this.value = value;
        this.isOn = true;
    }


    //Gerätestatus anzeigen
    public String getStatusText() {
        switch (type) {
            case SWITCH:
                return isOn ? "On" : "Off";
            case DIMMER:
                return value + " " + unit;
            case THERMOSTAT:
                return value + " " + unit;
            case BLIND:
                return isOn ? "Open" : "Closed";
            case SENSOR:
                return String.valueOf(value);
            default:
                throw new IllegalStateException("Unexpected device type: " + type);
        }
    }

}
