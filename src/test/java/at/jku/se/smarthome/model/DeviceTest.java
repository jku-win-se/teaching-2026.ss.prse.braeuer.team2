package at.jku.se.smarthome.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeviceTest {

    @Test
    public void constructor_switch_setsDefaultValues() {
        Device device = new Device("1", "Lamp", DeviceType.SWITCH);

        assertEquals("1", device.getId());
        assertEquals("Lamp", device.getName());
        assertEquals(DeviceType.SWITCH, device.getType());
        assertFalse(device.isOn());
        assertEquals(0.0, device.getValue(), 0.0001);
        assertEquals("", device.getUnit());
    }

    @Test
    public void constructor_dimmer_setsDefaultValues() {
        Device device = new Device("2", "Dimmer Lamp", DeviceType.DIMMER);

        assertFalse(device.isOn());
        assertEquals(0.0, device.getValue(), 0.0001);
        assertEquals("%", device.getUnit());
    }

    @Test
    public void constructor_thermostat_setsDefaultValues() {
        Device device = new Device("3", "Heater", DeviceType.THERMOSTAT);

        assertTrue(device.isOn());
        assertEquals(20.0, device.getValue(), 0.0001);
        assertEquals("°C", device.getUnit());
    }

    @Test
    public void toggle_switch_changesState() {
        Device device = new Device("1", "Lamp", DeviceType.SWITCH);

        device.toggle();
        assertTrue(device.isOn());

        device.toggle();
        assertFalse(device.isOn());
    }

    @Test
    public void toggle_nonSwitch_throwsException() {
        Device device = new Device("2", "Dimmer Lamp", DeviceType.DIMMER);

        assertThrows(IllegalStateException.class, device::toggle);
    }

    @Test
    public void setValue_dimmer_setsBrightnessAndTurnsOn() {
        Device device = new Device("2", "Dimmer Lamp", DeviceType.DIMMER);

        device.setValue(60);

        assertEquals(60.0, device.getValue(), 0.0001);
        assertTrue(device.isOn());
    }

    @Test
    public void setValue_dimmer_zeroTurnsOff() {
        Device device = new Device("2", "Dimmer Lamp", DeviceType.DIMMER);

        device.setValue(0);

        assertEquals(0.0, device.getValue(), 0.0001);
        assertFalse(device.isOn());
    }

    @Test
    public void setValue_dimmer_invalidValue_throwsException() {
        Device device = new Device("2", "Dimmer Lamp", DeviceType.DIMMER);

        assertThrows(IllegalArgumentException.class, () -> device.setValue(120));
    }

    @Test
    public void setValue_thermostat_setsTemperature() {
        Device device = new Device("3", "Heater", DeviceType.THERMOSTAT);

        device.setValue(22.5);

        assertEquals(22.5, device.getValue(), 0.0001);
        assertTrue(device.isOn());
    }

    @Test
    public void setValue_blind_open_sets100AndOn() {
        Device device = new Device("4", "Blind", DeviceType.BLIND);

        device.setValue(100);

        assertEquals(100.0, device.getValue(), 0.0001);
        assertTrue(device.isOn());
    }

    @Test
    public void setValue_blind_closed_sets0AndOff() {
        Device device = new Device("4", "Blind", DeviceType.BLIND);

        device.setValue(0);

        assertEquals(0.0, device.getValue(), 0.0001);
        assertFalse(device.isOn());
    }

    @Test
    public void setValue_blind_invalidValue_throwsException() {
        Device device = new Device("4", "Blind", DeviceType.BLIND);

        assertThrows(IllegalArgumentException.class, () -> device.setValue(50));
    }

    @Test
    public void setValue_sensor_setsValue() {
        Device device = new Device("5", "Temperature Sensor", DeviceType.SENSOR);

        device.setValue(18.3);

        assertEquals(18.3, device.getValue(), 0.0001);
        assertTrue(device.isOn());
    }

    @Test
    public void setValue_switch_throwsException() {
        Device device = new Device("6", "Switch", DeviceType.SWITCH);

        assertThrows(IllegalStateException.class, () -> device.setValue(1));
    }

    @Test
    public void setValue_dimmer_minValue() {
        Device device = new Device("1", "Dimmer", DeviceType.DIMMER);

        device.setValue(0);

        assertEquals(0.0, device.getValue(), 0.0001);
        assertFalse(device.isOn());
    }

    @Test
    public void setValue_dimmer_maxValue() {
        Device device = new Device("1", "Dimmer", DeviceType.DIMMER);

        device.setValue(100);

        assertEquals(100.0, device.getValue(), 0.0001);
        assertTrue(device.isOn());
    }

    public void setValue_thermostat_negativeValue() {
        Device device = new Device("3", "Heater", DeviceType.THERMOSTAT);

        device.setValue(-5);

        assertEquals(-5, device.getValue(), 0.0001);
    }

}
