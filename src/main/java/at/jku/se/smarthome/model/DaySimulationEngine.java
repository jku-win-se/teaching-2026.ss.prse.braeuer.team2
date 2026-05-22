package at.jku.se.smarthome.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs automation logic on copied devices so the live system remains unchanged.
 */
@SuppressWarnings({
        "PMD.CommentRequired",
        "PMD.CyclomaticComplexity",
        "PMD.TooManyMethods",
        "PMD.CouplingBetweenObjects"
})
public class DaySimulationEngine {
    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final String INITIAL_CONDITION_ACTOR = "Simulation initial condition";

    private final List<Room> rooms;
    private final List<Rule> activeRules;
    private final List<Schedule> schedules;
    private final List<DaySimulationEvent> events;
    private final Set<String> executingRuleIds;

    public DaySimulationEngine(List<Room> rooms, List<Rule> rules, List<Schedule> schedules) {
        this.rooms = copyRooms(rooms);
        this.activeRules = copyRules(rules);
        this.schedules = copySchedules(schedules);
        this.events = new ArrayList<>();
        this.executingRuleIds = new HashSet<>();
    }

    public DaySimulationResult simulate(DaySimulationRequest request) {
        applyInitialSensorValues(request.getStartDateTime(), request.getInitialSensorValues());

        LocalDateTime simulationTime = request.getStartDateTime();
        LocalDateTime endDateTime = request.getStartDateTime().plusDays(1);
        for (int minute = 0; minute < MINUTES_PER_DAY; minute++) {
            executeDueSchedules(simulationTime);
            executeDueTimeRules(simulationTime);
            simulationTime = simulationTime.plusMinutes(1);
        }
        return new DaySimulationResult(request.getStartDateTime(), endDateTime, rooms, events);
    }

    private void applyInitialSensorValues(LocalDateTime simulationTime, Map<String, Double> initialSensorValues) {
        for (Map.Entry<String, Double> entry : initialSensorValues.entrySet()) {
            Device device = findDeviceById(entry.getKey());
            if (device == null) {
                throw new IllegalArgumentException("Simulation sensor not found");
            }
            if (device.getType() != DeviceType.SENSOR) {
                throw new IllegalArgumentException("Initial values can only be configured for sensors");
            }
            applyValue(device, entry.getValue(), simulationTime, ActivityActorType.USER, INITIAL_CONDITION_ACTOR);
        }
    }

    private void executeDueSchedules(LocalDateTime simulationTime) {
        LocalDate simulationDate = simulationTime.toLocalDate();
        for (Schedule schedule : schedules) {
            if (!schedule.isDue(simulationDate, simulationTime.toLocalTime())) {
                continue;
            }
            executeSchedule(schedule, simulationTime);
        }
    }

    private void executeDueTimeRules(LocalDateTime simulationTime) {
        LocalDate simulationDate = simulationTime.toLocalDate();
        for (Rule rule : activeRules) {
            RuleTrigger trigger = rule.getTrigger();
            if (trigger.getTriggerType() != RuleTriggerType.TIME
                    || !trigger.isDue(simulationDate, simulationTime.toLocalTime())) {
                continue;
            }
            if (executeRule(rule, simulationTime)) {
                trigger.markTriggered(simulationDate);
            }
        }
    }

    private void executeSchedule(Schedule schedule, LocalDateTime simulationTime) {
        Device device = findDeviceById(schedule.getDeviceId());
        if (device == null) {
            throw new IllegalArgumentException("Simulation schedule device not found");
        }
        if (schedule.getActionType() == ScheduleActionType.TOGGLE) {
            applyToggle(device, simulationTime, ActivityActorType.RULE, schedule.getName());
        } else if (device.getType() == DeviceType.SWITCH) {
            applySwitchState(device, schedule.getTargetValue() == 1.0, simulationTime, ActivityActorType.RULE,
                    schedule.getName());
        } else {
            applyValue(device, schedule.getTargetValue(), simulationTime, ActivityActorType.RULE, schedule.getName());
        }
        schedule.markExecuted(simulationTime.toLocalDate());
    }

    private boolean executeRule(Rule rule, LocalDateTime simulationTime) {
        if (executingRuleIds.contains(rule.getId())) {
            return false;
        }

        executingRuleIds.add(rule.getId());
        try {
            RuleAction action = rule.getAction();
            Device targetDevice = findDeviceById(action.getTargetDeviceId());
            if (targetDevice == null) {
                throw new IllegalArgumentException("Simulation rule target device not found");
            }
            if (targetDevice.getType() == DeviceType.SWITCH) {
                applySwitchState(targetDevice, action.getTargetValue() == 1.0, simulationTime,
                        ActivityActorType.RULE, rule.getName());
            } else {
                applyValue(targetDevice, action.getTargetValue(), simulationTime, ActivityActorType.RULE,
                        rule.getName());
            }
            return true;
        } finally {
            executingRuleIds.remove(rule.getId());
        }
    }

    private void applyToggle(Device device, LocalDateTime simulationTime, ActivityActorType actorType,
                             String actorName) {
        String previousState = device.getStatusText();
        device.toggle();
        recordChange(device, simulationTime, actorType, actorName, previousState);
    }

    private void applySwitchState(Device device, boolean on, LocalDateTime simulationTime,
                                  ActivityActorType actorType, String actorName) {
        String previousState = device.getStatusText();
        device.setPowerState(on);
        recordChange(device, simulationTime, actorType, actorName, previousState);
    }

    private void applyValue(Device device, Double value, LocalDateTime simulationTime, ActivityActorType actorType,
                            String actorName) {
        if (value == null) {
            throw new IllegalArgumentException("Simulation target value must not be null");
        }
        String previousState = device.getStatusText();
        device.setValue(value);
        recordChange(device, simulationTime, actorType, actorName, previousState);
    }

    private void recordChange(Device device, LocalDateTime simulationTime, ActivityActorType actorType,
                              String actorName, String previousState) {
        String newState = device.getStatusText();
        if (previousState.equals(newState)) {
            return;
        }
        events.add(new DaySimulationEvent(simulationTime, device.getId(), device.getName(), actorType, actorName,
                previousState, newState));
        evaluateRulesAfterDeviceChange(device, previousState, newState, simulationTime);
    }

    private void evaluateRulesAfterDeviceChange(Device changedDevice, String previousState, String newState,
                                                LocalDateTime simulationTime) {
        if (previousState.equals(newState)) {
            return;
        }
        for (Rule rule : activeRules) {
            RuleTrigger trigger = rule.getTrigger();
            if (trigger.getSourceDeviceId() == null || !trigger.getSourceDeviceId().equals(changedDevice.getId())) {
                continue;
            }
            if (ruleMatchesChangedDevice(rule, changedDevice)) {
                executeRule(rule, simulationTime);
            }
        }
    }

    private boolean ruleMatchesChangedDevice(Rule rule, Device device) {
        return switch (rule.getTrigger().getTriggerType()) {
            case DEVICE_STATE_CHANGE -> ruleMatchesDeviceState(rule, device);
            case THRESHOLD -> ruleMatchesThreshold(rule, device);
            case TIME -> false;
        };
    }

    private boolean ruleMatchesDeviceState(Rule rule, Device device) {
        Double expectedValue = rule.getTrigger().getExpectedValue();
        return switch (device.getType()) {
            case SWITCH -> (expectedValue != null && expectedValue == 1.0) == device.isOn();
            case BLIND, DIMMER, THERMOSTAT, SENSOR ->
                    expectedValue != null && Double.compare(device.getValue(), expectedValue) == 0;
        };
    }

    private boolean ruleMatchesThreshold(Rule rule, Device device) {
        RuleTrigger trigger = rule.getTrigger();
        if (device.getType() != DeviceType.SENSOR || trigger.getExpectedValue() == null
                || trigger.getThresholdOperator() == null) {
            return false;
        }
        return switch (trigger.getThresholdOperator()) {
            case ABOVE -> device.getValue() > trigger.getExpectedValue();
            case BELOW -> device.getValue() < trigger.getExpectedValue();
        };
    }

    private Device findDeviceById(String deviceId) {
        for (Room room : rooms) {
            Device device = room.findDeviceById(deviceId);
            if (device != null) {
                return device;
            }
        }
        return null;
    }

    private List<Room> copyRooms(List<Room> sourceRooms) {
        List<Room> copiedRooms = new ArrayList<>();
        for (Room sourceRoom : sourceRooms) {
            Room copiedRoom = new Room(sourceRoom.getId(), sourceRoom.getName());
            for (Device sourceDevice : sourceRoom.getDevices()) {
                copiedRoom.addDevice(copyDevice(sourceDevice));
            }
            copiedRooms.add(copiedRoom);
        }
        return copiedRooms;
    }

    private Device copyDevice(Device sourceDevice) {
        Device copiedDevice = new Device(sourceDevice.getId(), sourceDevice.getName(), sourceDevice.getType());
        if (sourceDevice.getType() == DeviceType.SWITCH) {
            copiedDevice.setPowerState(sourceDevice.isOn());
        } else {
            copiedDevice.setValue(sourceDevice.getValue());
        }
        return copiedDevice;
    }

    private List<Rule> copyRules(List<Rule> sourceRules) {
        List<Rule> copiedRules = new ArrayList<>();
        for (Rule sourceRule : sourceRules) {
            copiedRules.add(new Rule(sourceRule.getId(), sourceRule.getName(), copyTrigger(sourceRule.getTrigger()),
                    copyAction(sourceRule.getAction())));
        }
        return copiedRules;
    }

    private RuleTrigger copyTrigger(RuleTrigger sourceTrigger) {
        return new RuleTrigger(
                sourceTrigger.getTriggerType(),
                sourceTrigger.getSourceDeviceId(),
                sourceTrigger.getExpectedValue(),
                sourceTrigger.getThresholdOperator(),
                sourceTrigger.getTriggerTime(),
                null
        );
    }

    private RuleAction copyAction(RuleAction sourceAction) {
        return new RuleAction(sourceAction.getActionType(), sourceAction.getTargetDeviceId(),
                sourceAction.getTargetValue());
    }

    private List<Schedule> copySchedules(List<Schedule> sourceSchedules) {
        List<Schedule> copiedSchedules = new ArrayList<>();
        for (Schedule sourceSchedule : sourceSchedules) {
            copiedSchedules.add(new Schedule(
                    sourceSchedule.getId(),
                    sourceSchedule.getName(),
                    sourceSchedule.getDeviceId(),
                    sourceSchedule.getActionType(),
                    sourceSchedule.getTargetValue(),
                    sourceSchedule.getExecutionTime(),
                    sourceSchedule.getRecurringDays()
            ));
        }
        return copiedSchedules;
    }
}
