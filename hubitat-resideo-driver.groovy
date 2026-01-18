/**
 *  Resideo Direct Thermostat Driver
 *
 *  Copyright 2024 Mathew Beall
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

metadata {
    definition (
        name: "Resideo Direct Thermostat",
        namespace: "mathewbeall",
        author: "Mathew Beall",
        importUrl: ""
    ) {
        capability "Thermostat"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "Refresh"
        capability "Sensor"
        capability "Actuator"

        // Standard thermostat attributes
        attribute "temperature", "number"
        attribute "humidity", "number"
        attribute "thermostatSetpoint", "number"
        attribute "heatingSetpoint", "number"
        attribute "coolingSetpoint", "number"
        attribute "thermostatMode", "string"
        attribute "thermostatFanMode", "string"
        attribute "thermostatOperatingState", "string"
        attribute "supportedThermostatFanModes", "JSON_OBJECT"
        attribute "supportedThermostatModes", "JSON_OBJECT"

        // Additional Resideo-specific attributes
        attribute "outdoorTemperature", "number"
        attribute "equipmentStatus", "string"
        attribute "setpointStatus", "string"
        attribute "scheduleStatus", "string"
        attribute "lastUpdate", "string"

        // Commands
        command "setHeatingSetpoint", [[name:"temperature*", type:"NUMBER", description:"Heating setpoint temperature"]]
        command "setCoolingSetpoint", [[name:"temperature*", type:"NUMBER", description:"Cooling setpoint temperature"]]
        command "setThermostatMode", [[name:"mode*", type:"ENUM", constraints:["heat","cool","auto","off","emergency heat"]]]
        command "setThermostatFanMode", [[name:"fanMode*", type:"ENUM", constraints:["auto","on","circulate"]]]
        command "heat"
        command "cool"
        command "auto"
        command "off"
        command "fanAuto"
        command "fanOn"
        command "fanCirculate"
        command "emergencyHeat"

        // Custom commands
        command "setSchedule", [[name:"mode*", type:"ENUM", constraints:["hold","schedule"]]]
        command "refresh"
    }

    preferences {
        input "tempScale", "enum", title: "Temperature Scale", options: ["F", "C"], defaultValue: "F", required: false
        input "autoRefresh", "number", title: "Auto Refresh Interval (minutes)", defaultValue: 5, required: false
        input "debugOutput", "bool", title: "Enable debug logging", defaultValue: false
        input "descTextEnable", "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def installed() {
    if (debugOutput) log.debug "Installing Resideo Direct Thermostat: ${device.displayName}"

    // Set initial values
    sendEvent(name: "supportedThermostatModes", value: ["heat", "cool", "auto", "off", "emergency heat"])
    sendEvent(name: "supportedThermostatFanModes", value: ["auto", "on", "circulate"])

    // Schedule auto refresh if enabled
    if (autoRefresh && autoRefresh > 0) {
        schedule("0 */${autoRefresh} * * * ?", "refresh")
    }

    initialize()
}

def updated() {
    if (debugOutput) log.debug "Updated Resideo Direct Thermostat: ${device.displayName}"

    // Unschedule existing jobs
    unschedule()

    // Schedule auto refresh if enabled
    if (autoRefresh && autoRefresh > 0) {
        schedule("0 */${autoRefresh} * * * ?", "refresh")
    }

    // Auto-disable debug logging after 30 minutes
    if (debugOutput) {
        runIn(1800, logsOff)
    }

    initialize()
}

def initialize() {
    // Initial refresh
    refresh()
}

def refresh() {
    if (debugOutput) log.debug "Refreshing thermostat data for ${device.displayName}"

    // The parent app will call updateThermostatData() with fresh data
    parent.updateAllDevices()
}

def updateThermostatData(thermostat) {
    if (debugOutput) log.debug "Updating thermostat data: ${thermostat}"

    try {
        // Temperature
        def temperature = thermostat.indoorTemperature
        if (temperature != null) {
            def temp = (tempScale == "C") ? fahrenheitToCelsius(temperature) : temperature
            sendEvent(name: "temperature", value: temp, unit: "°${tempScale}")
            if (descTextEnable) log.info "${device.displayName} temperature is ${temp}°${tempScale}"
        }

        // Humidity
        def humidity = thermostat.indoorHumidity
        if (humidity != null) {
            sendEvent(name: "humidity", value: humidity, unit: "%")
            if (descTextEnable) log.info "${device.displayName} humidity is ${humidity}%"
        }

        // Outdoor temperature
        def outdoorTemp = thermostat.outdoorTemperature
        if (outdoorTemp != null) {
            def temp = (tempScale == "C") ? fahrenheitToCelsius(outdoorTemp) : outdoorTemp
            sendEvent(name: "outdoorTemperature", value: temp, unit: "°${tempScale}")
        }

        // Changeable values (current settings)
        def changeableValues = thermostat.changeableValues
        if (changeableValues) {
            // HVAC Mode - check for emergency heat active flag
            def emergencyHeatActive = changeableValues.emergencyHeatActive ?: false
            def hvacMode = convertResideoModeToHubitat(changeableValues.mode, emergencyHeatActive)
            sendEvent(name: "thermostatMode", value: hvacMode)
            if (descTextEnable) log.info "${device.displayName} thermostat mode is ${hvacMode}"

            // Setpoints
            if (changeableValues.heatSetpoint != null) {
                def heatTemp = (tempScale == "C") ? fahrenheitToCelsius(changeableValues.heatSetpoint) : changeableValues.heatSetpoint
                sendEvent(name: "heatingSetpoint", value: heatTemp, unit: "°${tempScale}")
            }

            if (changeableValues.coolSetpoint != null) {
                def coolTemp = (tempScale == "C") ? fahrenheitToCelsius(changeableValues.coolSetpoint) : changeableValues.coolSetpoint
                sendEvent(name: "coolingSetpoint", value: coolTemp, unit: "°${tempScale}")
            }

            // Current setpoint (based on mode)
            def currentSetpoint = getCurrentSetpoint(hvacMode, changeableValues.heatSetpoint, changeableValues.coolSetpoint)
            if (currentSetpoint != null) {
                def setpointTemp = (tempScale == "C") ? fahrenheitToCelsius(currentSetpoint) : currentSetpoint
                sendEvent(name: "thermostatSetpoint", value: setpointTemp, unit: "°${tempScale}")
            }

            // Setpoint status
            def setpointStatus = changeableValues.thermostatSetpointStatus ?: "Unknown"
            sendEvent(name: "setpointStatus", value: setpointStatus)
        }

        // Fan settings
        def fanSettings = thermostat.settings?.fan?.changeableValues
        if (fanSettings?.mode) {
            def fanMode = convertResideoFanModeToHubitat(fanSettings.mode)
            sendEvent(name: "thermostatFanMode", value: fanMode)
            if (descTextEnable) log.info "${device.displayName} fan mode is ${fanMode}"
        }

        // Operating state
        def operationStatus = thermostat.operationStatus
        if (operationStatus) {
            def operatingState = convertResideoOperatingState(operationStatus)
            sendEvent(name: "thermostatOperatingState", value: operatingState)
            sendEvent(name: "equipmentStatus", value: operationStatus.mode ?: "Unknown")
            if (descTextEnable) log.info "${device.displayName} operating state is ${operatingState}"
        }

        // Last update timestamp
        sendEvent(name: "lastUpdate", value: new Date().format("yyyy-MM-dd HH:mm:ss"))

    } catch (Exception e) {
        log.error "Error updating thermostat data: ${e.message}"
    }
}

// ========== Thermostat Control Commands ==========

def setHeatingSetpoint(temperature) {
    if (debugOutput) log.debug "Setting heating setpoint to ${temperature}°${tempScale}"

    def tempF = (tempScale == "C") ? celsiusToFahrenheit(temperature) : temperature

    def result = parent.sendThermostatCommand(device.deviceNetworkId, "setTemperature", [
        heatSetpoint: tempF
    ])

    if (result.success) {
        sendEvent(name: "heatingSetpoint", value: temperature, unit: "°${tempScale}")
        if (descTextEnable) log.info "${device.displayName} heating setpoint set to ${temperature}°${tempScale}"
    } else {
        log.error "Failed to set heating setpoint: ${result.error}"
    }
}

def setCoolingSetpoint(temperature) {
    if (debugOutput) log.debug "Setting cooling setpoint to ${temperature}°${tempScale}"

    def tempF = (tempScale == "C") ? celsiusToFahrenheit(temperature) : temperature

    def result = parent.sendThermostatCommand(device.deviceNetworkId, "setTemperature", [
        coolSetpoint: tempF
    ])

    if (result.success) {
        sendEvent(name: "coolingSetpoint", value: temperature, unit: "°${tempScale}")
        if (descTextEnable) log.info "${device.displayName} cooling setpoint set to ${temperature}°${tempScale}"
    } else {
        log.error "Failed to set cooling setpoint: ${result.error}"
    }
}

def setThermostatMode(mode) {
    if (debugOutput) log.debug "Setting thermostat mode to ${mode}"

    // Handle emergency heat specially - route to emergencyHeat() command
    if (mode?.toLowerCase() == "emergency heat") {
        emergencyHeat()
        return
    }

    def resideoMode = convertHubitatModeToResideo(mode)

    def result = parent.sendThermostatCommand(device.deviceNetworkId, "setMode", [
        mode: resideoMode
    ])


    if (result && result.success) {
        sendEvent(name: "thermostatMode", value: mode)
        if (descTextEnable) log.info "${device.displayName} thermostat mode set to ${mode}"
    } else {
        log.error "Failed to set thermostat mode: ${result ? result.error : 'No response from app'}"
    }
}

def setThermostatFanMode(fanMode) {
    if (debugOutput) log.debug "Setting fan mode to ${fanMode}"

    def resideoFanMode = convertHubitatFanModeToResideo(fanMode)

    def result = parent.sendThermostatCommand(device.deviceNetworkId, "setFan", [
        fanMode: resideoFanMode
    ])

    if (result.success) {
        sendEvent(name: "thermostatFanMode", value: fanMode)
        if (descTextEnable) log.info "${device.displayName} fan mode set to ${fanMode}"
    } else {
        log.error "Failed to set fan mode: ${result.error}"
    }
}

// ========== Convenience Commands ==========

def heat() {
    setThermostatMode("heat")
}

def cool() {
    setThermostatMode("cool")
}

def auto() {
    setThermostatMode("auto")
}

def off() {
    setThermostatMode("off")
}

def fanAuto() {
    setThermostatFanMode("auto")
}

def fanOn() {
    setThermostatFanMode("on")
}

def fanCirculate() {
    setThermostatFanMode("circulate")
}

def emergencyHeat() {
    if (debugOutput) log.debug "Setting emergency heat mode"

    def result = parent.sendThermostatCommand(device.deviceNetworkId, "setEmergencyHeat", [
        emergencyHeatActive: true
    ])

    if (result && result.success) {
        sendEvent(name: "thermostatMode", value: "emergency heat")
        if (descTextEnable) log.info "${device.displayName} thermostat mode set to emergency heat"
    } else {
        def errorMsg = result?.error ?: 'No response from app'
        log.error "Failed to set emergency heat mode: ${errorMsg}"

        // Provide user-friendly warning for unsupported systems
        if (errorMsg.contains("not supported")) {
            log.warn "${device.displayName} does not support emergency heat - this feature requires a heat pump system with auxiliary/backup heating"
        }
    }
}

// ========== Custom Commands ==========

def setSchedule(mode) {
    if (debugOutput) log.debug "Setting schedule mode to ${mode}"

    def setpointStatus = (mode == "schedule") ? "NoHold" : "PermanentHold"

    log.info "${device.displayName} schedule mode requested: ${mode} (setpoint status: ${setpointStatus})"
    sendEvent(name: "scheduleStatus", value: mode)
}

// ========== Helper Functions ==========

private convertResideoModeToHubitat(resideoMode, emergencyHeatActive = false) {
    // Check for emergency heat first - it's a flag on top of heat mode
    if (emergencyHeatActive) {
        return "emergency heat"
    }

    switch (resideoMode?.toLowerCase()) {
        case "heat": return "heat"
        case "cool": return "cool"
        case "auto": return "auto"
        case "heat_cool": return "auto"
        case "off": return "off"
        default: return "unknown"
    }
}

private convertHubitatModeToResideo(hubitatMode) {
    switch (hubitatMode?.toLowerCase()) {
        case "heat": return "Heat"
        case "cool": return "Cool"
        case "auto": return "Auto"
        case "off": return "Off"
        default: return "Off"
    }
}

private convertResideoFanModeToHubitat(resideoFanMode) {
    switch (resideoFanMode?.toLowerCase()) {
        case "auto": return "auto"
        case "on": return "on"
        case "circulate": return "circulate"
        default: return "auto"
    }
}

private convertHubitatFanModeToResideo(hubitatFanMode) {
    switch (hubitatFanMode?.toLowerCase()) {
        case "auto": return "Auto"
        case "on": return "On"
        case "circulate": return "Circulate"
        default: return "Auto"
    }
}

private convertResideoOperatingState(operationStatus) {
    def mode = operationStatus.mode?.toLowerCase()
    def fanRequest = operationStatus.fanRequest
    def circulationFanRequest = operationStatus.circulationFanRequest

    switch (mode) {
        case "heat":
        case "heating":
            return "heating"
        case "cool":
        case "cooling":
            return "cooling"
        case "equipmentoff":
        case "idle":
            if (fanRequest || circulationFanRequest) {
                return "fan only"
            }
            return "idle"
        default:
            return "idle"
    }
}

private getCurrentSetpoint(mode, heatSetpoint, coolSetpoint) {
    switch (mode) {
        case "heat":
            return heatSetpoint
        case "cool":
            return coolSetpoint
        case "auto":
            if (heatSetpoint != null && coolSetpoint != null) {
                return coolSetpoint
            } else if (heatSetpoint != null) {
                return heatSetpoint
            } else if (coolSetpoint != null) {
                return coolSetpoint
            }
            break
        default:
            return null
    }
}

// ========== Temperature Conversion ==========

private fahrenheitToCelsius(fahrenheit) {
    return Math.round((fahrenheit - 32) * 5 / 9 * 10) / 10
}

private celsiusToFahrenheit(celsius) {
    return Math.round((celsius * 9 / 5 + 32) * 10) / 10
}

// ========== Logging Functions ==========

def logsOff() {
    log.warn "Debug logging disabled for ${device.displayName}"
    device.updateSetting("debugOutput", [value: "false", type: "bool"])
}

private logDebug(msg) {
    if (settings?.debugOutput || settings?.debugOutput == null) {
        log.debug "$msg"
    }
}

private logInfo(msg) {
    if (settings?.descTextEnable || settings?.descTextEnable == null) {
        log.info "$msg"
    }
}