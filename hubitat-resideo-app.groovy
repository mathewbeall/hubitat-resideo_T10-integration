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
                href "credentialsPage", title: "Update Credentials", description: "Change developer credentials"
            }
        }

        // Only show API connection section if OAuth is enabled and credentials are provided
        if (settings.clientId && settings.clientSecret) {
            // Check if OAuth is actually enabled by trying to create access token
            def oauthEnabled = true
            try {
                if (!state.accessToken) {
                    createAccessToken()
                }
            } catch (Exception e) {
                oauthEnabled = false
            }

            if (oauthEnabled) {
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
            } else {
                section("🚨 OAuth Required Before API Connection") {
                    paragraph """
                    <div style='background-color:#fff3e0; border:2px solid #f57c00; padding:10px; border-radius:5px;'>
                        <p style='color:#ef6c00; font-weight:bold; margin:0;'>
                            ⚠️ OAuth must be enabled before you can connect to Resideo API
                        </p>
                        <p style='color:#666; margin:5px 0 0 0; font-size:14px;'>
                            Go to Apps Code → Find this app → Click 3 dots (⋯) next to Save → OAuth → Enable OAuth in App
                        </p>
                    </div>
                    """
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
        }
    }
}

def credentialsPage() {
    // Reset the refresh button if it was clicked
    if (settings.refreshCredentials) {
        app.removeSetting("refreshCredentials")
    }

    // Allow installation if credentials are provided
    def canInstall = (settings.clientId && settings.clientSecret)
    dynamicPage(name: "credentialsPage", title: "Developer Credentials Setup", nextPage: "mainPage", install: canInstall, uninstall: true) {
        section("How to Get Credentials") {
            paragraph """
            To use the Resideo API, you need developer credentials from Honeywell/Resideo.
            These are different from your regular Resideo account credentials.
            """

            paragraph """
            <div style='background-color:#e3f2fd; border:1px solid #1976d2; padding:15px; border-radius:5px; margin:10px 0;'>
                <h3 style='color:#1976d2; margin-top:0;'>📋 Follow These Steps:</h3>
                <ol style='margin:5px 0 0 20px; color:#333;'>
                    <li><b>Go to</b> <a href="https://developer.honeywellhome.com" target="_blank">https://developer.honeywellhome.com</a></li>
                    <li><b>Sign in</b> with your regular Resideo account</li>
                    <li><b>Click</b> "My Apps" → "Create New App"</li>
                    <li><b>Fill out the form:</b><br/>
                        • App Name: "Hubitat Integration" (or whatever you prefer)</li>
                    <li><b>Submit</b> and copy the Consumer Key and Consumer Secret below ⬇️</li>
                </ol>
            </div>
            """
        }

        section("Enter Your Credentials") {
            paragraph "<b>Paste the credentials from your Resideo developer app:</b>"

            input "clientId", "text", title: "Consumer Key", description: "Your Resideo API Consumer Key (from developer portal)", required: true, submitOnChange: true
            input "clientSecret", "password", title: "Consumer Secret", description: "Your Resideo API Consumer Secret (from developer portal)", required: true, submitOnChange: true

            // Add refresh button for users who paste without triggering submitOnChange
            if (settings.clientId || settings.clientSecret) {
                input "refreshCredentials", "bool", title: "✅ Check Credentials & Show Save Options", submitOnChange: true, defaultValue: false, description: "Click this after pasting both credentials to refresh the page"
            }
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
                input "callbackUrlDisplay", "textarea", title: "Callback URL (Select All & Copy)",
                    description: "Triple-click to select all, then copy this URL to your Resideo developer portal",
                    defaultValue: callbackUrl
            }
        } else if (callbackUrl && callbackUrl.startsWith("ERROR")) {
            section("🚨 OAUTH REQUIRED - ACTION NEEDED 🚨") {
                paragraph """
                <div style='background-color:#ffebee; border:3px solid #d32f2f; padding:15px; margin:10px 0; border-radius:5px;'>
                    <h2 style='color:#d32f2f; margin-top:0; text-align:center;'>⚠️ OAUTH IS NOT ENABLED ⚠️</h2>
                    <p style='color:#d32f2f; font-weight:bold; font-size:16px; text-align:center; margin-bottom:15px;'>
                        You must enable OAuth before proceeding!
                    </p>
                    <div style='background-color:white; padding:10px; border-radius:3px; border:1px solid #d32f2f;'>
                        <b style='color:#d32f2f;'>Required Steps:</b><br/>
                        <ol style='color:#333; margin:5px 0 0 20px;'>
                            <li><b>Go to "Apps Code"</b> section in Hubitat (main menu → Apps Code)</li>
                            <li><b>Find this app</b> in your Apps Code list</li>
                            <li><b>Click on the app name</b> to open it</li>
                            <li><b>Look for the 3 dots (⋯)</b> next to the "Save" button at the top right</li>
                            <li><b>Click the 3 dots (⋯)</b> to open the dropdown menu</li>
                            <li><b>Select "OAuth"</b> from the dropdown menu</li>
                            <li><b>Click "Enable OAuth in App"</b></li>
                            <li><b>Click "Update"</b> to save the OAuth setting</li>
                            <li><b>Return to this app's main page</b> - the callback URL will appear</li>
                        </ol>
                    </div>
                </div>
                """
            }
        }

        // Show save instructions if credentials are entered
        if (canInstall) {
            section("✅ Credentials Configured - Next Steps") {
                paragraph """
                <div style='background-color:#e8f5e8; border:1px solid #4caf50; padding:15px; border-radius:5px; margin:10px 0;'>
                    <h3 style='color:#4caf50; margin-top:0;'>🎉 Great! Your credentials are configured.</h3>
                    <p style='color:#333; margin-bottom:10px;'><b>IMPORTANT:</b> Click "Done" to save the app now. This prevents losing your settings during OAuth setup.</p>
                    <p style='color:#333; margin:5px 0;'><b>After saving:</b></p>
                    <ol style='margin:5px 0 0 20px; color:#333;'>
                        <li>The app will be installed and your credentials saved</li>
                        <li>You can then return to configure OAuth authentication</li>
                        <li>Complete the OAuth flow without losing your settings</li>
                    </ol>
                </div>
                """
            }
        }

        section("Important Notes") {
            paragraph """
            • Keep these credentials secure - they provide access to your thermostats<br/>
            • The Consumer Secret will only be shown once on the developer portal<br/>
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
            section("⚠️ FIRST: Update Your Resideo Developer App") {
                paragraph """
                <div style='background-color:#e3f2fd; border:2px solid #1976d2; padding:15px; border-radius:5px; margin-bottom:15px;'>
                    <h3 style='color:#1976d2; margin-top:0;'>📋 Required: Add Redirect URL to Your Resideo App</h3>
                    <p style='margin:5px 0;'><b>Before clicking "Authorize" below, you MUST add this redirect URL to your Resideo developer app:</b></p>

                    <div style='background-color:#f5f5f5; padding:10px; border-radius:3px; border:1px solid #ccc; margin:10px 0;'>
                        <b>Redirect URL to copy:</b><br/>
                        <code style='font-family:monospace; word-break:break-all;'>${buildRedirectUrl()}</code>
                    </div>

                    <p><b>How to add it:</b></p>
                    <ol style='margin:5px 0 0 20px;'>
                        <li>Go to <a href="https://developer.honeywellhome.com" target="_blank">https://developer.honeywellhome.com</a></li>
                        <li>Sign in and find your app</li>
                        <li>Click "Edit" on your app</li>
                        <li>Find the "Redirect URIs" field</li>
                        <li>Paste the redirect URL above</li>
                        <li>Click "Save" or "Update"</li>
                        <li>Come back here and click "Authorize" below</li>
                    </ol>
                </div>
                """
            }

            section("Authorization") {
                paragraph "After adding the redirect URL above, click the link below to authorize:"
                href url: authUrl, title: "Authorize with Resideo", external: true, description: "This will open Resideo's authorization page"
                paragraph "After authorizing, you will be redirected back to complete the setup."
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
        if (!state.thermostats) {
            discoverThermostats()
        }

        if (state.thermostats) {
            // Separate thermostats into installed vs available
            def installedThermostats = []
            def availableThermostats = []

            state.thermostats.each { thermostat ->
                def deviceExists = getChildDevice(thermostat.deviceID)
                if (deviceExists) {
                    installedThermostats.add(thermostat)
                } else {
                    availableThermostats.add(thermostat)
                }
            }

            // Show installed thermostats
            if (installedThermostats.size() > 0) {
                section("✅ Installed Thermostats") {
                    installedThermostats.each { thermostat ->
                        def deviceId = thermostat.deviceID
                        def name = thermostat.userDefinedDeviceName
                        def temperature = thermostat.indoorTemperature
                        def humidity = thermostat.indoorHumidity
                        def mode = thermostat.changeableValues?.mode ?: "Unknown"

                        paragraph """
                        <div style='background-color:#e8f5e8; border:2px solid #4caf50; padding:12px; border-radius:5px; margin:8px 0;'>
                            <b>${name}</b> ✅<br/>
                            Device ID: ${deviceId}<br/>
                            Current: ${temperature}°F, ${humidity}% humidity<br/>
                            Mode: ${mode}<br/>
                            <b style='color:#2e7d32'>Status: ✅ Installed and Ready</b>
                        </div>
                        """
                    }
                }
            }

            // Show available thermostats for installation
            if (availableThermostats.size() > 0) {
                section("📥 Available Thermostats") {
                    // Add "Install All" button if there are multiple thermostats
                    if (availableThermostats.size() > 1) {
                        input "installAll", "bool", title: "📥 Install All Available Thermostats", defaultValue: false, submitOnChange: true

                        // Handle "Install All" button press
                        if (installAll) {
                            def allInstallations = []
                            availableThermostats.each { thermostat ->
                                def result = installThermostat(thermostat)
                                if (result) {
                                    allInstallations.add(thermostat.userDefinedDeviceName ?: "Thermostat ${thermostat.deviceID}")
                                }
                            }
                            app.removeSetting("installAll")

                            if (allInstallations.size() > 0) {
                                section("🎉 Mass Installation Complete") {
                                    paragraph """
                                    <div style='background-color:#e8f5e8; border:2px solid #4caf50; padding:15px; border-radius:5px; margin:10px 0;'>
                                        <h3 style='color:#2e7d32; margin-top:0;'>🎉 All Thermostats Successfully Installed!</h3>
                                        <p style='color:#2e7d32; margin:5px 0;'><b>Installed ${allInstallations.size()} thermostat(s):</b></p>
                                        <ul style='color:#2e7d32; margin:5px 0 0 20px;'>
                                            ${allInstallations.collect { "<li><b>${it}</b></li>" }.join('')}
                                        </ul>
                                        <p style='color:#666; margin:10px 0 0 0; font-size:14px;'>
                                            All your thermostats are now available in the Devices section and ready to use!
                                        </p>
                                        <p style='color:#666; margin:10px 0 0 0; font-size:14px;'>
                                            <i>Page will refresh in a moment to update the thermostat list...</i>
                                        </p>
                                    </div>
                                    <script>
                                        setTimeout(function() {
                                            window.location.reload();
                                        }, 2000);
                                    </script>
                                    """
                                }
                            }
                            return
                        }
                    }

                    availableThermostats.each { thermostat ->
                        def deviceId = thermostat.deviceID
                        def name = thermostat.userDefinedDeviceName
                        def temperature = thermostat.indoorTemperature
                        def humidity = thermostat.indoorHumidity
                        def mode = thermostat.changeableValues?.mode ?: "Unknown"

                        paragraph """
                        <div style='background-color:#f9f9f9; border:2px solid #ddd; padding:12px; border-radius:5px; margin:8px 0;'>
                            <b>${name}</b><br/>
                            Device ID: ${deviceId}<br/>
                            Current: ${temperature}°F, ${humidity}% humidity<br/>
                            Mode: ${mode}<br/>
                            <b style='color:#666'>Status: ⚪ Available for Installation</b>
                        </div>
                        """

                        input "install_${deviceId}", "bool", title: "📥 Install ${name}", defaultValue: false, submitOnChange: true
                    }

                    // Check for new individual installations
                    def newInstallations = []
                    availableThermostats.each { thermostat ->
                        def deviceId = thermostat.deviceID
                        if (settings["install_${deviceId}"]) {
                            def result = installThermostat(thermostat)
                            if (result) {
                                newInstallations.add(thermostat.userDefinedDeviceName ?: "Thermostat ${deviceId}")
                            }
                            app.removeSetting("install_${deviceId}")
                        }
                    }

                    // Show individual installation success feedback and redirect to refresh the page
                    if (newInstallations.size() > 0) {
                        section("✅ Installation Complete") {
                            paragraph """
                            <div style='background-color:#e8f5e8; border:2px solid #4caf50; padding:10px; border-radius:5px; margin:10px 0;'>
                                <h3 style='color:#2e7d32; margin-top:0;'>🎉 Successfully Installed!</h3>
                                <p style='color:#2e7d32; margin:5px 0;'><b>Installed thermostats:</b></p>
                                <ul style='color:#2e7d32; margin:5px 0 0 20px;'>
                                    ${newInstallations.collect { "<li><b>${it}</b></li>" }.join('')}
                                </ul>
                                <p style='color:#666; margin:10px 0 0 0; font-size:14px;'>
                                    Your thermostats are now available in the Devices section and ready to use!
                                </p>
                                <p style='color:#666; margin:10px 0 0 0; font-size:14px;'>
                                    <i>Page will refresh in a moment to update the thermostat list...</i>
                                </p>
                            </div>
                            <script>
                                setTimeout(function() {
                                    window.location.reload();
                                }, 2000);
                            </script>
                            """
                        }
                        return // Exit early to show only the success message and trigger refresh
                    }
                }
            } else if (installedThermostats.size() > 0) {
                section("📥 Available Thermostats") {
                    paragraph """
                    <div style='background-color:#e8f5e8; border:2px solid #4caf50; padding:15px; border-radius:5px; margin:10px 0;'>
                        <h3 style='color:#2e7d32; margin-top:0;'>🎉 All Thermostats Installed!</h3>
                        <p style='color:#2e7d32; margin:5px 0;'>
                            All discovered thermostats have been successfully installed and are ready to use.
                        </p>
                        <p style='color:#666; margin:10px 0 0 0; font-size:14px;'>
                            Your thermostats are available in the Devices section.
                        </p>
                    </div>
                    """
                }
            }

        } else {
            section("Discovered Thermostats") {
                paragraph "⚠️ No thermostats discovered. Check your API connection."
            }
        }

        section("Discovery Options") {
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

        return true  // Installation successful

    } catch (Exception e) {
        log.error "Error installing thermostat ${name}: ${e.message}"
        return false  // Installation failed
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

    logDebug "Getting fresh thermostat data before sending command..."

    // First test if basic API access works
    if (!testApiAccess()) {
        log.error "API access test failed - cannot proceed with command"
        return [success: false, error: "API access failed"]
    }

    discoverThermostats()
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

            // Keep thermostatSetpointStatus for LCC devices
            requestBody.thermostatSetpointStatus = "PermanentHold"

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

            logDebug "Using complete changeable values approach for mode change to: ${parameters.mode}"
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
    log.info "Request body: ${requestBody}"

    def jsonBuilder = new JsonBuilder(requestBody)
    def jsonString = jsonBuilder.toString()

    logDebug "JSON string being sent: ${jsonString}"
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
    // Use proper JSON serialization like working thermostat commands
    def requestBody = [mode: fanMode]
    def jsonBuilder = new JsonBuilder(requestBody)
    def jsonString = jsonBuilder.toString()

    logDebug "Fan request body: ${requestBody}"
    logDebug "Fan JSON string being sent: ${jsonString}"

    def params = [
        uri: "https://api.honeywell.com/v2/devices/thermostats/${deviceId}/fan",
        query: [
            apikey: settings.clientId,
            locationId: locationId
        ],
        headers: [
            'Authorization': "Bearer ${state.resideoAccessToken}",
            'Content-Type': 'application/json; charset=utf-8'
        ],
        body: jsonString,
        requestContentType: 'application/json'
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

// ========== Priority Command Handlers ==========

def sendPriorityCommand(deviceId, command, parameters = [:]) {
    logDebug "Sending priority command: ${command} to device ${deviceId} with parameters: ${parameters}"

    if (!state.resideoAccessToken) {
        log.error "No access token available for priority command"
        return [success: false, error: "No access token"]
    }

    // Find the thermostat to get locationId
    def targetThermostat = null
    state.thermostats?.each { thermostat ->
        if (thermostat.deviceID == deviceId) {
            targetThermostat = thermostat
        }
    }

    if (!targetThermostat) {
        log.error "Could not find thermostat with device ID: ${deviceId}"
        return [success: false, error: "Device not found"]
    }

    def locationId = targetThermostat.locationID

    switch (command) {
        case "setPriority":
            return setPrioritySettings(deviceId, locationId, parameters.priorityType, parameters.selectedRooms)

        case "setSelectedRooms":
            return setPrioritySettings(deviceId, locationId, parameters.priorityType, parameters.selectedRooms)

        default:
            log.error "Unknown priority command: ${command}"
            return [success: false, error: "Unknown command"]
    }
}

def getRoomPriorityData(deviceId) {
    logDebug "Getting room priority data for device ${deviceId}"

    if (!state.resideoAccessToken) {
        log.error "No access token available for room data"
        return [success: false, error: "No access token"]
    }

    // Find the thermostat to get locationId
    def targetThermostat = null
    state.thermostats?.each { thermostat ->
        if (thermostat.deviceID == deviceId) {
            targetThermostat = thermostat
        }
    }

    if (!targetThermostat) {
        log.error "Could not find thermostat with device ID: ${deviceId}"
        return [success: false, error: "Device not found"]
    }

    def locationId = targetThermostat.locationID
    def priorityData = getPrioritySettings(deviceId, locationId)

    if (priorityData) {
        return [success: true, data: priorityData]
    } else {
        return [success: false, error: "Failed to get priority data"]
    }
}

// ========== Priority API Functions ==========

def getPrioritySettings(deviceId, locationId) {
    def params = [
        uri: "https://api.honeywell.com/v2/devices/thermostats/${deviceId}/priority",
        query: [
            apikey: settings.clientId,
            locationId: locationId
        ],
        headers: [
            'Authorization': "Bearer ${state.resideoAccessToken}",
            'Content-Type': 'application/json'
        ]
    ]

    try {
        def priorityData = null
        httpGet(params) { response ->
            if (response.status >= 200 && response.status < 300) {
                priorityData = response.data
            } else {
                log.error "Get priority failed with status: ${response.status}"
            }
        }
        return priorityData
    } catch (Exception e) {
        log.error "Error getting priority settings: ${e.message}"
        return null
    }
}

def setPrioritySettings(deviceId, locationId, priorityType, selectedRooms = null) {
    def prioritySetting = [priorityType: priorityType]

    if (priorityType == 'PickARoom' && selectedRooms) {
        prioritySetting.selectedRooms = selectedRooms
    }

    // Pre-serialize the JSON body like the working thermostat command
    def requestBody = [currentPriority: prioritySetting]
    def jsonBuilder = new JsonBuilder(requestBody)
    def jsonString = jsonBuilder.toString()

    logDebug "Priority API request body: ${requestBody}"
    logDebug "Priority JSON string being sent: ${jsonString}"

    def params = [
        uri: "https://api.honeywell.com/v2/devices/thermostats/${deviceId}/priority",
        query: [
            apikey: settings.clientId,
            locationId: locationId
        ],
        headers: [
            'Authorization': "Bearer ${state.resideoAccessToken}",
            'Content-Type': 'application/json; charset=utf-8'
        ],
        body: jsonString,
        requestContentType: 'application/json'
    ]

    try {
        def prioritySuccess = false
        def priorityError = null
        httpPut(params) { response ->
            if (response.status >= 200 && response.status < 300) {
                log.info "Priority setting successful: ${priorityType}"
                runIn(2, updateAllDevices)
                prioritySuccess = true
            } else {
                log.error "Priority setting failed with status: ${response.status}"
                priorityError = "HTTP ${response.status}"
            }
        }

        if (prioritySuccess) {
            return [success: true]
        } else {
            return [success: false, error: priorityError ?: "Priority setting failed"]
        }
    } catch (Exception e) {
        log.error "Error setting priority: ${e.message}"
        return [success: false, error: e.message]
    }
}

def getRoomData(deviceId, locationId, groupId = 0) {
    def params = [
        uri: "https://api.honeywell.com/v2/devices/thermostats/${deviceId}/group/${groupId}/rooms",
        query: [
            apikey: settings.clientId,
            locationId: locationId
        ],
        headers: [
            'Authorization': "Bearer ${state.resideoAccessToken}",
            'Content-Type': 'application/json'
        ]
    ]

    try {
        def roomData = null
        httpGet(params) { response ->
            if (response.status >= 200 && response.status < 300) {
                roomData = response.data
            } else {
                log.error "Get rooms failed with status: ${response.status}"
            }
        }
        return roomData
    } catch (Exception e) {
        log.error "Error getting room data: ${e.message}"
        return null
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
                <h2 style="color: green;">Authorization Successful!</h2>
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