/**
 *  Resideo Direct API Integration
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

import groovy.json.JsonBuilder

definition(
    name: "Resideo Direct API Integration",
    namespace: "mathewbeall",
    author: "Mathew Beall",
    description: "Integrates Resideo T10 thermostats using the official Resideo/Honeywell API",
    category: "Climate Control",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: true,
    oauth: true
)

preferences {
    page(name: "mainPage")
    page(name: "credentialsPage")
    page(name: "authPage")
    page(name: "oauthInitialize")
    page(name: "discoveryPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Resideo Direct API Integration", install: true, uninstall: true) {
        section("Developer Credentials") {
            if (!settings.clientId || !settings.clientSecret) {
                paragraph "⚠️ Developer credentials required"
                href "credentialsPage", title: "Setup Developer Credentials", description: "Enter your Resideo API credentials"
            } else {
                paragraph "✅ Developer credentials configured"
                paragraph "Client ID: ${settings.clientId}"
                href "credentialsPage", title: "Update Credentials", description: "Change developer credentials"
            }
        }

        if (settings.clientId && settings.clientSecret) {
            section("API Connection") {
                if (state.resideoAccessToken) {
                    paragraph "✅ Connected to Resideo API"
                    paragraph "Access token expires: ${state.tokenExpires}"
                    input "refreshToken", "bool", title: "Refresh API Token", submitOnChange: true, defaultValue: false
                } else {
                    paragraph "⚠️ Not connected to Resideo API"
                    if (!state.resideoAccessToken) {
                        href "oauthInitialize", title: "Connect to Resideo API", description: "Authorize access to your thermostats"
                        href "authPage", title: "Manual Token Entry", description: "Enter tokens manually if OAuth fails"
                    }
                }
            }
        }

        if (state.resideoAccessToken) {
            section("Device Discovery") {
                href "discoveryPage", title: "Discover Thermostats", description: "Find and add Resideo thermostats"
                if (state.thermostats) {
                    paragraph "Found ${state.thermostats.size()} thermostat(s)"
                }
            }
        }


        section("Installed Devices") {
            def children = getChildDevices()
            if (children) {
                children.each { device ->
                    paragraph "${device.displayName} (${device.deviceNetworkId})"
                }
            } else {
                paragraph "No thermostats installed yet"
            }
        }

        section("Options") {
            input "debugOutput", "bool", title: "Enable debug logging", defaultValue: false
            input "descTextEnable", "bool", title: "Enable descriptionText logging", defaultValue: true
        }

        section("API Information") {
            paragraph "This integration uses the official Resideo/Honeywell API to control your T10 thermostats directly."
            if (settings.clientId) {
                paragraph "Using Client ID: ${settings.clientId}"
            }
        }
    }
}

def credentialsPage() {
    dynamicPage(name: "credentialsPage", title: "Developer Credentials Setup", nextPage: "mainPage") {
        section("Resideo API Credentials") {
            paragraph """
            To use the Resideo API, you need developer credentials from Honeywell/Resideo.
            These are different from your regular Resideo account credentials.
            """

            input "clientId", "text", title: "Client ID", description: "Your Resideo API Client ID", required: true
            input "clientSecret", "password", title: "Client Secret", description: "Your Resideo API Client Secret", required: true
        }

        section("How to Get Credentials") {
            paragraph """
            <b>Step 1:</b> Go to <a href="https://developer.honeywellhome.com" target="_blank">https://developer.honeywellhome.com</a><br/>
            <b>Step 2:</b> Sign in with your regular Resideo account<br/>
            <b>Step 3:</b> Click "My Apps" → "Create New App"<br/>
            <b>Step 4:</b> Fill out the form:<br/>
            • App Name: "Hubitat Integration" (or whatever you prefer)<br/>
            <b>Step 5:</b> Submit and copy the Client ID and Client Secret below
            """
        }

        // Show callback URL only after credentials are entered
        def callbackUrl = null
        if (clientId && clientSecret) {
            // Ensure we have an access token for the callback URL
            if (!state.accessToken) {
                createAccessToken()
            }
            callbackUrl = buildRedirectUrl()
        }

        if (callbackUrl && !callbackUrl.startsWith("ERROR")) {
            section("📋 Callback URL (Copy This to Resideo Developer Portal)") {
                paragraph "<b>Copy and paste this URL exactly:</b>"
                paragraph "<pre style='background-color:#f5f5f5; padding:10px; border:1px solid #ccc; font-family:monospace; word-wrap:break-word;'>${callbackUrl}</pre>"
            }
        } else if (callbackUrl && callbackUrl.startsWith("ERROR")) {
            section("⚠️ OAuth Setup Required") {
                paragraph "<div style='color:red; font-weight:bold;'>OAuth is not enabled for this app!</div>"
                paragraph """
                <b>To enable OAuth:</b><br/>
                1. Click "Done" to save this app<br/>
                2. Find this app in your Apps list<br/>
                3. Click the 3 dots (⋯) menu next to the app name<br/>
                4. Select "OAuth" from the menu<br/>
                5. Click "Enable OAuth in App"<br/>
                6. Come back and the callback URL will appear here
                """
            }
        }

        section("Important Notes") {
            paragraph """
            • Keep these credentials secure - they provide access to your thermostats<br/>
            • The Client Secret will only be shown once on the developer portal<br/>
            • You can always regenerate credentials if needed
            """
        }
    }
}

def oauthInitialize() {
    if (!state.resideoAccessToken) {
        logDebug "Starting OAuth initialization"

        // Store the external OAuth parameters for use in callback
        state.oauthClientId = settings.clientId
        state.oauthClientSecret = settings.clientSecret

        def oauthParams = [
            response_type: "code",
            scope: "",
            client_id: settings.clientId,
            redirect_uri: buildRedirectUrl()
        ]

        def authUrl = "https://api.honeywell.com/oauth2/authorize?" +
            oauthParams.collect { key, value -> "${key}=${java.net.URLEncoder.encode(value.toString(), 'UTF-8')}" }.join('&')

        logDebug "Authorization URL: ${authUrl}"
        logDebug "Redirect URL: ${buildRedirectUrl()}"

        dynamicPage(name: "oauthInitialize", title: "Connect to Resideo API", nextPage: null, uninstall: false, install: false) {
            section("Authorization") {
                paragraph "Click the link below to authorize this app with your Resideo account:"
                href url: authUrl, title: "Authorize with Resideo", external: true, description: "This will open Resideo's authorization page"
                paragraph "Redirect URL being used: ${buildRedirectUrl()}"
                paragraph "After authorizing, you will be redirected back to complete the setup."
            }
            section("Debug") {
                paragraph "Client ID: ${settings.clientId}"
                paragraph "Full auth URL: ${authUrl}"
            }
        }
    } else {
        dynamicPage(name: "oauthInitialize", title: "Already Connected", nextPage: "mainPage", uninstall: false, install: false) {
            section("Connection Status") {
                paragraph "✅ Already connected to Resideo API"
                paragraph "Return to main page to discover thermostats."
            }
        }
    }
}

def authPage() {
    dynamicPage(name: "authPage", title: "Manual Token Entry", nextPage: "mainPage") {
        section("Manual Authentication") {
            paragraph "If OAuth isn't working, you can manually enter tokens from external OAuth setup:"

            input "accessToken", "text", title: "Access Token", description: "Paste access token here", required: false
            input "refreshTokenValue", "text", title: "Refresh Token", description: "Paste refresh token here", required: false
            input "tokenExpires", "text", title: "Token Expires", description: "Token expiration date/time", required: false
        }

        section("Instructions") {
            paragraph """
            Use this only if the built-in OAuth doesn't work.
            You can use the oauth_helper.py script to get tokens manually.
            """
        }
    }
}

def discoveryPage() {
    dynamicPage(name: "discoveryPage", title: "Thermostat Discovery", nextPage: "mainPage") {
        section("Discovered Thermostats") {
            if (!state.thermostats) {
                discoverThermostats()
            }

            if (state.thermostats) {
                state.thermostats.each { thermostat ->
                    def deviceId = thermostat.deviceID
                    def name = thermostat.userDefinedDeviceName
                    def temperature = thermostat.indoorTemperature
                    def humidity = thermostat.indoorHumidity
                    def mode = thermostat.changeableValues?.mode ?: "Unknown"

                    def deviceExists = getChildDevice(deviceId)

                    paragraph """
                    <b>${name}</b><br/>
                    Device ID: ${deviceId}<br/>
                    Current: ${temperature}°F, ${humidity}% humidity<br/>
                    Mode: ${mode}<br/>
                    Status: ${deviceExists ? "✅ Installed" : "⚪ Not installed"}
                    """

                    if (!deviceExists) {
                        input "install_${deviceId}", "bool", title: "Install ${name}", defaultValue: false, submitOnChange: true
                    }
                }

                // Check for new installations
                state.thermostats.each { thermostat ->
                    def deviceId = thermostat.deviceID
                    if (settings["install_${deviceId}"] && !getChildDevice(deviceId)) {
                        installThermostat(thermostat)
                        app.removeSetting("install_${deviceId}")
                    }
                }

            } else {
                paragraph "⚠️ No thermostats discovered. Check your API connection."
            }

            input "rediscover", "bool", title: "Refresh Discovery", submitOnChange: true, defaultValue: false
            if (rediscover) {
                state.remove("thermostats")
                app.removeSetting("rediscover")
            }
        }
    }
}

def installed() {
    logDebug "Installed with settings: ${settings}"
    createAccessToken()
    initialize()
}

def updated() {
    logDebug "Updated with settings: ${settings}"

    // Update stored tokens if provided
    if (accessToken) {
        state.resideoAccessToken = accessToken
        app.removeSetting("accessToken") // Don't store in settings
    }
    if (refreshTokenValue) {
        state.refreshToken = refreshTokenValue
        app.removeSetting("refreshTokenValue") // Don't store in settings
    }
    if (tokenExpires) {
        state.tokenExpires = tokenExpires
        app.removeSetting("tokenExpires")
    }

    if (refreshToken) {
        refreshAccessToken()
        app.removeSetting("refreshToken")
    }

    initialize()
}

def initialize() {
    logDebug "Initializing Resideo Direct API Integration"

    // Schedule token refresh (every 23 hours)
    schedule("0 0 1 * * ?", refreshAccessToken)

    // Schedule regular updates (every 5 minutes)
    schedule("0 */5 * * * ?", updateAllDevices)

    // Clean up old scheduled jobs
    unschedule("discoveryRefresh")
}

def uninstalled() {
    logDebug "Uninstalling and removing child devices"
    removeChildDevices(getChildDevices())
}

def removeChildDevices(devices) {
    devices.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

// ========== API Functions ==========

def discoverThermostats() {
    if (!state.resideoAccessToken) {
        log.error "No access token available for API discovery"
        return
    }

    def params = [
        uri: "https://api.honeywell.com/v2/locations",
        query: [apikey: settings.clientId],
        headers: [
            'Authorization': "Bearer ${state.resideoAccessToken}",
            'Content-Type': 'application/json'
        ]
    ]

    try {
        httpGet(params) { response ->
            if (response.status == 200) {
                def locations = response.data
                def allThermostats = []

                locations.each { location ->
                    if (location.devices) {
                        location.devices.each { device ->
                            if (device.deviceType == "Thermostat" || device.deviceClass == "Thermostat") {
                                // Add location info to device
                                device.locationID = location.locationID
                                device.locationName = location.name
                                allThermostats.add(device)
                            }
                        }
                    }
                }

                state.thermostats = allThermostats
                log.info "Discovered ${allThermostats.size()} thermostats"

                if (debugOutput) {
                    logDebug "Discovered thermostats: ${allThermostats}"
                }

            } else {
                log.error "API discovery failed with status: ${response.status}"
            }
        }
    } catch (Exception e) {
        log.error "Error discovering thermostats: ${e.message}"
        if (e.message.contains("Unauthorized")) {
            log.warn "Access token may be expired, attempting refresh..."
            refreshAccessToken()
        }
    }
}

def refreshAccessToken() {
    if (!state.refreshToken) {
        log.error "No refresh token available"
        return false
    }

    def credentials = "${settings.clientId}:${settings.clientSecret}"
    def credentialsB64 = credentials.bytes.encodeBase64()

    def params = [
        uri: "https://api.honeywell.com/oauth2/token",
        headers: [
            'Authorization': "Basic ${credentialsB64}",
            'Content-Type': 'application/x-www-form-urlencoded'
        ],
        body: [
            'grant_type': 'refresh_token',
            'refresh_token': state.refreshToken
        ]
    ]

    def success = false

    try {
        httpPost(params) { response ->
            logDebug "Token refresh response status: ${response.status}"
            logDebug "Token refresh response data: ${response.data}"
            if (response.status == 200) {
                def tokenData = response.data
                logDebug "Token data received: ${tokenData}"

                if (tokenData?.access_token) {
                    state.resideoAccessToken = tokenData.access_token
                    logDebug "New access token stored: ${tokenData.access_token?.take(10)}..."
                } else {
                    log.error "No access_token in response data!"
                    success = false
                    return
                }

                if (tokenData.refresh_token) {
                    state.refreshToken = tokenData.refresh_token
                    logDebug "New refresh token stored: ${tokenData.refresh_token?.take(10)}..."
                }

                def expiresIn = (tokenData.expires_in as Integer) ?: 3600
                def expirationTime = new Date(now() + (expiresIn * 1000))
                state.tokenExpires = expirationTime.toString()

                log.info "Access token refreshed successfully, expires: ${state.tokenExpires}"
                success = true
            } else {
                log.error "Token refresh failed with status: ${response.status}"
                log.error "Response data: ${response.data}"
                success = false
            }
        }
    } catch (Exception e) {
        log.error "Error refreshing token: ${e.message}"
        success = false
    }

    return success
}

def installThermostat(thermostat) {
    def deviceId = thermostat.deviceID
    def name = thermostat.userDefinedDeviceName
    def model = thermostat.deviceModel ?: "T10"

    try {
        def childDevice = addChildDevice(
            "mathewbeall",
            "Resideo Direct Thermostat",
            deviceId,
            null,
            [
                "name": name,
                "label": name,
                "data": [
                    "locationID": thermostat.locationID,
                    "locationName": thermostat.locationName,
                    "deviceModel": model
                ]
            ]
        )

        log.info "Installed thermostat: ${name} (${deviceId})"

        // Initial update
        updateThermostat(childDevice, thermostat)

    } catch (Exception e) {
        log.error "Error installing thermostat ${name}: ${e.message}"
    }
}

def updateAllDevices() {
    if (debugOutput) logDebug "Updating all thermostat devices"

    discoverThermostats() // Refresh data

    getChildDevices().each { device ->
        def deviceId = device.deviceNetworkId
        def thermostat = state.thermostats?.find { it.deviceID == deviceId }

        if (thermostat) {
            updateThermostat(device, thermostat)
        } else {
            log.warn "Could not find thermostat data for device: ${device.displayName}"
        }
    }
}

def updateThermostat(device, thermostat) {
    if (debugOutput) logDebug "Updating thermostat: ${device.displayName}"

    // Update device with current thermostat data
    device.updateThermostatData(thermostat)
}

// ========== Child Device Communication ==========

def sendThermostatCommand(deviceId, command, parameters = [:]) {
    logDebug "Sending thermostat command: ${command} to device ${deviceId} with parameters: ${parameters}"

    if (!state.resideoAccessToken) {
        log.error "No access token available for thermostat command"
        return [success: false, error: "No access token"]
    }

    // CRITICAL: Get FRESH thermostat data like Python does
    logDebug "Getting fresh thermostat data before sending command..."

    // First test if basic API access works
    if (!testApiAccess()) {
        log.error "API access test failed - cannot proceed with command"
        return [success: false, error: "API access failed"]
    }

    discoverThermostats() // This refreshes state.thermostats

    // CRITICAL: Based on timestamp analysis, need 500ms delay after discovery
    // for API data to be properly available
    pauseExecution(500)

    // Get the thermostat data to find location ID
    def thermostat = state.thermostats?.find { it.deviceID == deviceId }
    if (!thermostat) {
        log.error "Could not find thermostat data for device: ${deviceId}"
        logDebug "Available thermostats: ${state.thermostats?.collect { it.deviceID }}"
        return [success: false, error: "Thermostat not found"]
    }

    def locationId = thermostat.locationID
    logDebug "Using locationID: ${locationId}"

    // Get current changeable values from FRESH data
    def currentValues = thermostat.changeableValues
    logDebug "Fresh changeable values: ${currentValues}"

    // CRITICAL: This log.info is somehow essential for proper execution
    log.info "Current thermostat values: ${currentValues}"



    // Build the request based on command
    def requestBody = [:]

    switch (command) {
        case "setTemperature":
            requestBody = currentValues?.clone() ?: [:]
            if (parameters.mode) requestBody.mode = parameters.mode
            if (parameters.heatSetpoint != null) requestBody.heatSetpoint = parameters.heatSetpoint
            if (parameters.coolSetpoint != null) requestBody.coolSetpoint = parameters.coolSetpoint
            requestBody.thermostatSetpointStatus = "PermanentHold"
            break

        case "setMode":
            // CRITICAL: Must send ALL changeable values (like working Python script)
            // Get complete current changeable values and modify only the mode
            requestBody = currentValues?.clone() ?: [:]

            // Update only the mode field
            requestBody.mode = parameters.mode.toString()

            // CRITICAL: Do NOT modify heatCoolMode!
            // The working Python script keeps heatCoolMode unchanged
            // Only modify the primary 'mode' field

            // Keep thermostatSetpointStatus for LCC devices
            requestBody.thermostatSetpointStatus = "PermanentHold"

            // CRITICAL: Ensure exact data types that work in Python
            // Force integers for temperature values
            if (requestBody.heatSetpoint != null) {
                requestBody.heatSetpoint = Math.round(requestBody.heatSetpoint as Double) as Integer
            }
            if (requestBody.coolSetpoint != null) {
                requestBody.coolSetpoint = Math.round(requestBody.coolSetpoint as Double) as Integer
            }
            if (requestBody.endHeatSetpoint != null) {
                requestBody.endHeatSetpoint = Math.round(requestBody.endHeatSetpoint as Double) as Integer
            }
            if (requestBody.endCoolSetpoint != null) {
                requestBody.endCoolSetpoint = Math.round(requestBody.endCoolSetpoint as Double) as Integer
            }
            // Force boolean for boolean values
            if (requestBody.autoChangeoverActive != null) {
                requestBody.autoChangeoverActive = (requestBody.autoChangeoverActive == true || requestBody.autoChangeoverActive == 'true')
            }
            // Ensure string values are proper strings
            if (requestBody.mode != null) {
                requestBody.mode = requestBody.mode.toString()
            }
            if (requestBody.heatCoolMode != null) {
                requestBody.heatCoolMode = requestBody.heatCoolMode.toString()
            }
            if (requestBody.thermostatSetpointStatus != null) {
                requestBody.thermostatSetpointStatus = requestBody.thermostatSetpointStatus.toString()
            }
            if (requestBody.nextPeriodTime != null) {
                requestBody.nextPeriodTime = requestBody.nextPeriodTime.toString()
            }

            // DO NOT remove any fields - API expects complete changeable values
            // This includes potentially "conflicting" fields like heatCoolMode

            logDebug "Using complete changeable values approach (like Python) for mode change to: ${parameters.mode}"
            logDebug "Final request body after type conversion: ${requestBody}"
            break

        case "setFan":
            // Fan mode is handled differently - use fan endpoint
            return sendFanCommand(deviceId, locationId, parameters.fanMode)

        default:
            log.error "Unknown command: ${command}"
            return [success: false, error: "Unknown command"]
    }

    logDebug "Request body: ${requestBody}"

    // CRITICAL: This log.info is somehow essential for proper execution
    log.info "Request body: ${requestBody}"


    // CRITICAL: Force proper JSON serialization by converting to JSON string
    def jsonBuilder = new JsonBuilder(requestBody)
    def jsonString = jsonBuilder.toString()

    logDebug "JSON string being sent: ${jsonString}"

    // CRITICAL: This log.info is somehow essential for proper execution
    log.info "JSON being sent: ${jsonString}"

    def params = [
        uri: "https://api.honeywell.com/v2/devices/thermostats/${deviceId}",
        query: [
            locationId: locationId,
            apikey: settings.clientId
        ],
        headers: [
            'Authorization': "Bearer ${state.resideoAccessToken}",
            'Content-Type': 'application/json; charset=utf-8'
        ],
        body: jsonString,
        requestContentType: 'application/json'
    ]

    logDebug "Full API request: ${params}"

    try {
        def commandSuccess = false
        httpPost(params) { response ->
            logDebug "Response status: ${response.status}"
            logDebug "Response data: ${response.data}"
            logDebug "Response headers: ${response.headers}"

            if (response.status >= 200 && response.status < 300) {
                log.info "Thermostat command successful: ${command}"

                // Refresh device data after successful command
                runIn(2, updateAllDevices)

                commandSuccess = true
            } else {
                log.error "Thermostat command failed with status: ${response.status}"
                log.error "Response data: ${response.data}"

                // Enhanced error logging for 500 errors
                if (response.status == 500) {
                    log.error "SERVER ERROR 500 - This indicates an API server problem"
                    log.error "Request that caused error: ${params}"

                    // Check if this is a token issue
                    if (state.resideoAccessToken) {
                        logDebug "Access token exists, length: ${state.resideoAccessToken.length()}"
                        logDebug "Token expires: ${state.tokenExpires}"
                    } else {
                        log.error "No access token found!"
                    }
                }

                // Try to get more details from the response - capture response body for 500 errors
                try {
                    if (response.data) {
                        log.error "API Error response data: ${response.data}"
                    }
                    if (response.errorData) {
                        log.error "API Error errorData: ${response.errorData}"
                    }
                    if (response.errorMessage) {
                        log.error "API Error message: ${response.errorMessage}"
                    }
                    // Try to access response content directly
                    log.error "Response status: ${response.status}"
                    log.error "Response headers: ${response.headers}"
                } catch (Exception parseEx) {
                    logDebug "Could not parse error response: ${parseEx.message}"
                }

                return [success: false, error: "HTTP ${response.status}"]
            }
        }

        // Return based on success flag set in closure
        if (commandSuccess) {
            return [success: true]
        } else {
            return [success: false, error: "Command failed"]
        }

    } catch (Exception e) {
        log.error "Error sending thermostat command: ${e.message}"
        log.error "Full exception details: ${e}"

        // Handle specific error types
        if (e.message.contains("Unauthorized")) {
            log.warn "Unauthorized error, attempting token refresh..."
            refreshAccessToken()
        } else if (e.message.contains("500")) {
            log.error "500 Server Error caught in exception handler"
            log.error "This suggests the server rejected our request format"
        }

        return [success: false, error: e.message]
    }
}

def sendFanCommand(deviceId, locationId, fanMode) {
    def params = [
        uri: "https://api.honeywell.com/v2/devices/thermostats/${deviceId}/fan",
        query: [
            apikey: settings.clientId,
            locationId: locationId
        ],
        headers: [
            'Authorization': "Bearer ${state.resideoAccessToken}",
            'Content-Type': 'application/json'
        ],
        body: [mode: fanMode]
    ]

    try {
        def fanSuccess = false
        def fanError = null
        httpPost(params) { response ->
            if (response.status >= 200 && response.status < 300) {
                log.info "Fan command successful: ${fanMode}"
                runIn(2, updateAllDevices)
                fanSuccess = true
            } else {
                log.error "Fan command failed with status: ${response.status}"
                fanError = "HTTP ${response.status}"
            }
        }

        if (fanSuccess) {
            return [success: true]
        } else {
            return [success: false, error: fanError ?: "Fan command failed"]
        }
    } catch (Exception e) {
        log.error "Error sending fan command: ${e.message}"
        return [success: false, error: e.message]
    }
}

// ========== API Test Functions ==========

def testApiAccess() {
    // Test basic API access with a simple locations call
    def params = [
        uri: "https://api.honeywell.com/v2/locations",
        query: [
            apikey: settings.clientId
        ],
        headers: [
            'Authorization': "Bearer ${state.resideoAccessToken}",
            'Content-Type': 'application/json'
        ]
    ]

    try {
        def success = false
        httpGet(params) { response ->
            logDebug "API test response status: ${response.status}"
            if (response.status >= 200 && response.status < 300) {
                log.info "✅ API access test successful"
                success = true
            } else {
                log.error "❌ API access test failed with status: ${response.status}"
                success = false
            }
        }
        if (success) return true
    } catch (Exception e) {
        log.error "❌ API access test exception: ${e.message}"

        // If we get 401 Unauthorized, try to refresh the token
        if (e.message.contains("401") || e.message.contains("Unauthorized")) {
            log.warn "🔄 Token appears expired, attempting refresh..."
            if (refreshAccessToken()) {
                log.info "✅ Token refreshed successfully, retrying API test..."

                // Update the authorization header with new token
                params.headers['Authorization'] = "Bearer ${state.resideoAccessToken}"

                // Retry the test
                try {
                    def retrySuccess = false
                    httpGet(params) { response ->
                        logDebug "API test retry response status: ${response.status}"
                        if (response.status >= 200 && response.status < 300) {
                            log.info "✅ API access test successful after token refresh"
                            retrySuccess = true
                        } else {
                            log.error "❌ API access test still failed after token refresh: ${response.status}"
                            retrySuccess = false
                        }
                    }
                    if (retrySuccess) return true
                } catch (Exception retryEx) {
                    log.error "❌ API access test retry failed: ${retryEx.message}"
                    return false
                }
            } else {
                log.error "❌ Token refresh failed"
                return false
            }
        }

        return false
    }
}

// ========== Logging Functions ==========

def logsOff() {
    log.warn "Debug logging disabled"
    device.updateSetting("debugOutput", [value: "false", type: "bool"])
}


private logDebug(msg) {
    if (settings?.debugOutput) {
        log.debug "$msg"
    }
}

private logInfo(msg) {
    if (settings?.descTextEnable || settings?.descTextEnable == null) {
        log.info "$msg"
    }
}

// ========== OAuth2 Functions ==========

def buildRedirectUrl() {
    logDebug "buildRedirectUrl called - checking access token"
    logDebug "Current state.accessToken: ${state.accessToken}"

    if (!state.accessToken) {
        try {
            logDebug "Creating new access token..."
            createAccessToken()
            logDebug "Created access token: ${state.accessToken}"
        } catch (Exception e) {
            log.error "Failed to create access token: ${e.message}"
            log.error "Exception details: ${e}"
            return "ERROR: Could not create access token - OAuth may not be enabled. Exception: ${e.message}"
        }
    }

    def fullUrl = getFullApiServerUrl()
    logDebug "getFullApiServerUrl() returned: ${fullUrl}"
    if (!fullUrl) {
        return "ERROR: Could not get API server URL - getFullApiServerUrl() returned null"
    }

    def finalUrl = fullUrl + "/callback?access_token=${state.accessToken}"
    logDebug "Final callback URL: ${finalUrl}"
    return finalUrl
}

def oauthCallback() {
    logDebug "OAuth callback received: ${params}"
    logDebug "Full params: ${params}"

    def code = params.code
    def error = params.error

    if (error) {
        log.error "OAuth error: ${error}"
        log.error "Error description: ${params.error_description}"
        render contentType: "text/html", data: """
        <html>
        <body>
            <h2 style="color: red;">Authorization Failed</h2>
            <p>Error: ${error}</p>
            <p>Description: ${params.error_description ?: 'No description provided'}</p>
            <p>Please return to Hubitat and try again.</p>
        </body>
        </html>
        """
        return
    }

    if (code) {
        log.info "Authorization code received: ${code}"

        // Exchange code for tokens
        def tokenResult = exchangeCodeForTokens(code)
        logDebug "exchangeCodeForTokens returned: ${tokenResult}"

        if (tokenResult) {
            log.info "Token exchange successful, redirecting user"
            render contentType: "text/html", data: """
            <html>
            <body style="font-family: Arial; text-align: center; margin-top: 50px;">
                <h2 style="color: green;">✅ Authorization Successful!</h2>
                <p>Your Resideo thermostats are now connected to Hubitat.</p>
                <p>You can close this window and return to your Hubitat app.</p>
                <script>
                    setTimeout(function() {
                        window.close();
                    }, 3000);
                </script>
            </body>
            </html>
            """
        } else {
            log.error "Token exchange returned false, showing error page"
            render contentType: "text/html", data: """
            <html>
            <body style="font-family: Arial; text-align: center; margin-top: 50px;">
                <h2 style="color: red;">❌ Token Exchange Failed</h2>
                <p>Failed to exchange authorization code for access tokens.</p>
                <p>Check the app logs for more details.</p>
                <p>Please return to Hubitat and try again.</p>
            </body>
            </html>
            """
        }
    } else {
        log.error "No authorization code received in callback"
        log.error "Available params: ${params.keySet()}"
        render contentType: "text/html", data: """
        <html>
        <body>
            <h2 style="color: red;">No Authorization Code</h2>
            <p>Did not receive authorization code from Resideo.</p>
            <p>Available parameters: ${params.keySet().join(', ')}</p>
            <p>Please return to Hubitat and try again.</p>
        </body>
        </html>
        """
    }
}

def exchangeCodeForTokens(code) {
    logDebug "Exchanging authorization code for tokens"
    logDebug "Code received: ${code}"
    logDebug "Client ID: ${settings.clientId}"
    logDebug "Redirect URI: ${buildRedirectUrl()}"

    if (!settings.clientId || !settings.clientSecret) {
        log.error "Missing client credentials"
        return false
    }

    def credentials = "${settings.clientId}:${settings.clientSecret}"
    def credentialsB64 = credentials.bytes.encodeBase64()

    def params = [
        uri: "https://api.honeywell.com/oauth2/token",
        headers: [
            'Authorization': "Basic ${credentialsB64}",
            'Content-Type': 'application/x-www-form-urlencoded'
        ],
        body: [
            'grant_type': 'authorization_code',
            'code': code,
            'redirect_uri': buildRedirectUrl()
        ]
    ]

    logDebug "Token exchange request: ${params}"

    def success = false

    try {
        httpPost(params) { response ->
            logDebug "Token response status: ${response.status}"
            logDebug "Token response data: ${response.data}"

            if (response.status == 200) {
                def tokenData = response.data
                logDebug "Processing token data: ${tokenData}"

                if (!tokenData.access_token) {
                    log.error "No access token in response"
                    success = false
                    return
                }

                state.resideoAccessToken = tokenData.access_token
                logDebug "Stored access token: ${tokenData.access_token}"

                if (tokenData.refresh_token) {
                    state.refreshToken = tokenData.refresh_token
                    logDebug "Stored refresh token: ${tokenData.refresh_token}"
                }

                try {
                    def expiresIn = (tokenData.expires_in as Integer) ?: 3600
                    logDebug "Token expires in: ${expiresIn} seconds"

                    def expirationTime = new Date(now() + (expiresIn * 1000L))
                    state.tokenExpires = expirationTime.toString()
                    logDebug "Token expiration set to: ${state.tokenExpires}"

                } catch (Exception dateEx) {
                    log.warn "Error processing expiration date: ${dateEx.message}"
                    logDebug "Setting calculated expiration"

                    // Calculate manually without Date formatting
                    def expiresIn = (tokenData.expires_in as Integer) ?: 1800
                    def currentTime = now()
                    def expirationTimestamp = currentTime + (expiresIn * 1000L)

                    // Create a simple readable format
                    def hours = (expiresIn / 3600) as int
                    def minutes = ((expiresIn % 3600) / 60) as int

                    if (hours > 0) {
                        state.tokenExpires = "${hours}h ${minutes}m from now"
                    } else {
                        state.tokenExpires = "${minutes} minutes from now"
                    }

                    logDebug "Manual expiration calculation: ${state.tokenExpires}"
                }

                log.info "OAuth tokens obtained successfully"
                logDebug "Final token state - Access: ${state.resideoAccessToken?.take(10)}..., Refresh: ${state.refreshToken?.take(10)}..."
                success = true
            } else {
                log.error "Token exchange failed with status: ${response.status}"
                log.error "Response data: ${response.data}"
                success = false
            }
        }
    } catch (Exception e) {
        log.error "Error exchanging tokens: ${e.message}"
        log.error "Exception details: ${e}"
        success = false
    }

    logDebug "Returning success: ${success}"
    return success
}

mappings {
    path("/callback") {
        action: [GET: "oauthCallback"]
    }
    path("/test") {
        action: [GET: "testCallback"]
    }
}

def testCallback() {
    log.info "Test callback called - mappings are working!"
    render contentType: "text/html", data: "Test callback works!"
}

// Hubitat's standard OAuth callback method
def oauthCallbackHandler() {
    logDebug "Hubitat OAuth callback handler called: ${params}"
    return oauthCallback()
}