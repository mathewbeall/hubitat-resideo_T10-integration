/**
 *  Resideo Remote Sensor Driver
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
        name: "Resideo Remote Sensor",
        namespace: "mathewbeall",
        author: "Mathew Beall",
        importUrl: ""
    ) {
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "MotionSensor"
        capability "Sensor"
        capability "Refresh"

        attribute "humidity", "number"
        attribute "occupancy", "string"
        attribute "battery", "string"
        attribute "rssi", "number"
        attribute "roomName", "string"
        attribute "lastUpdate", "string"
        attribute "sensorType", "string"

        command "refresh"
    }

    preferences {
        input "debugOutput", "bool", title: "Enable debug logging", defaultValue: false
        input "descTextEnable", "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def installed() {
    if (debugOutput) log.debug "Installing Resideo Remote Sensor: ${device.displayName}"
    initialize()
}

def updated() {
    if (debugOutput) log.debug "Updated Resideo Remote Sensor: ${device.displayName}"

    // Auto-disable debug logging after 30 minutes
    if (debugOutput) {
        runIn(1800, logsOff)
    }

    initialize()
}

def initialize() {
    // Initial data will be loaded by parent app's updateAllDevices()
}

def refresh() {
    if (debugOutput) log.debug "Refreshing sensor data for ${device.displayName}"
    parent.requestRefresh()
}

def updateSensorData(Map sensorData) {
    if (debugOutput) log.debug "Updating sensor data: ${sensorData}"

    try {
        def nativeUnit = sensorData.nativeUnit ?: "F"

        // Room name
        if (sensorData.roomName) {
            sendEvent(name: "roomName", value: sensorData.roomName)
        }

        // Temperature
        def temperature = sensorData.indoorTemperature
        if (temperature != null) {
            def temp = formatTemperature(temperature, nativeUnit)
            sendEvent(name: "temperature", value: temp, unit: "°${nativeUnit}")
            if (descTextEnable) log.info "${device.displayName} temperature is ${temp}°${nativeUnit}"
        }

        // Humidity
        def humidity = sensorData.indoorHumidity
        if (humidity != null) {
            sendEvent(name: "humidity", value: humidity, unit: "%")
            if (descTextEnable) log.info "${device.displayName} humidity is ${humidity}%"
        }

        // Motion
        def motionDet = sensorData.motionDet
        if (motionDet != null) {
            def motionValue = motionDet ? "active" : "inactive"
            sendEvent(name: "motion", value: motionValue)
            if (descTextEnable) log.info "${device.displayName} motion is ${motionValue}"
        }

        // Occupancy
        def occupancyDet = sensorData.occupancyDet
        if (occupancyDet != null) {
            def occupancyValue = occupancyDet ? "occupied" : "unoccupied"
            sendEvent(name: "occupancy", value: occupancyValue)
            if (descTextEnable) log.info "${device.displayName} occupancy is ${occupancyValue}"
        }

        // Battery status
        def batteryStatus = sensorData.batteryStatus
        if (batteryStatus != null) {
            sendEvent(name: "battery", value: batteryStatus)
        }

        // RSSI
        def rssiAverage = sensorData.rssiAverage
        if (rssiAverage != null) {
            sendEvent(name: "rssi", value: rssiAverage)
        }

        // Sensor type
        def sensorType = sensorData.sensorType
        if (sensorType) {
            sendEvent(name: "sensorType", value: sensorType)
        }

        // Last update timestamp
        sendEvent(name: "lastUpdate", value: new Date().format("yyyy-MM-dd HH:mm:ss"))

    } catch (Exception e) {
        log.error "Error updating sensor data: ${e.message}"
    }
}

// ========== Temperature Conversion ==========

/**
 * Format temperature with proper precision for the unit
 * Fahrenheit: integer (no decimals)
 * Celsius: 0.5 degree precision (always shows decimal, e.g., 17.0, 17.5)
 */
private formatTemperature(value, unit) {
    if (value == null) return null
    if (unit == "C") {
        // Round to 0.5 and use BigDecimal with scale 1 to always show one decimal
        def rounded = Math.round(value * 2) / 2.0
        return new BigDecimal(rounded).setScale(1, BigDecimal.ROUND_HALF_UP)
    } else {
        return Math.round(value) as Integer
    }
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
