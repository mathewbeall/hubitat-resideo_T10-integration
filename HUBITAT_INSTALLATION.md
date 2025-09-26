# Hubitat Integration Installation Guide

This guide covers how to install and configure the Resideo Direct API integration for Hubitat home automation platform.

## Prerequisites

1. **Resideo/Honeywell Account**: Your regular Resideo account with T10 thermostats
2. **Hubitat Hub**: Running firmware version 2.2.4 or later with OAuth enabled
3. **Resideo T10 Thermostats**: Connected to your Resideo account and working in the mobile app

## Quick Start (Built-in OAuth)

### Step 1: Install Hubitat Code

1. **Install the App**:
   - Navigate to **Apps Code** in your Hubitat hub
   - Click **+ New App**
   - Copy the entire contents of `hubitat-resideo-app.groovy`
   - Click **Save**

2. **Install the Driver**:
   - Navigate to **Drivers Code**
   - Click **+ New Driver**
   - Copy the entire contents of `hubitat-resideo-driver.groovy`
   - Click **Save**

3. **Add the App**:
   - Navigate to **Apps**
   - Click **+ Add User App**
   - Select **Resideo Direct API Integration**
   - Click **Done**

### Step 2: Enter API Credentials

1. **Open the app**: Find **Resideo Direct API Integration** in your Apps list
2. **Enter credentials**:
   - **Consumer Key**: Paste your Consumer Key from the Resideo developer portal
   - **Consumer Secret**: Paste your Consumer Secret from the Resideo developer portal
3. **Click**: **Done** to save your credentials

### Step 3: Configure Callback URL

1. **Back in the app**: You should now see your credentials saved
2. **Copy the Callback URL**: Copy the long callback URL displayed in the multi-line text box
3. **Go to Resideo Developer Portal**: Visit [developer.honeywellhome.com](https://developer.honeywellhome.com)
4. **Edit your app**: Find your developer app and click **Edit**
5. **Paste Callback URL**: Paste the callback URL into the **Redirect URI** field
6. **Save**: Click **Save** in the developer portal

### Step 4: Connect to Resideo API

1. **Back in Hubitat app**: Return to the Resideo app in Hubitat
2. **Click**: **Connect to Resideo API**
3. **Authorize**: You'll be taken to Resideo's official OAuth page
4. **Log in**: Use your **regular Resideo account** (same as mobile app)
5. **Grant permission**: Click **"Allow"** to let Hubitat access your thermostats
6. **Done**: You'll be redirected back to Hubitat automatically

**✅ That's it!** OAuth is handled entirely within Hubitat.

### Step 5: Discover and Install Thermostats

1. **Back to main app page**: You should see "✅ Connected to Resideo API"
2. **Click**: **Discover Thermostats**
3. **Wait for discovery**: The app will find all thermostats in your account
4. **Install your thermostats**: For each one you want:
   - Toggle **Install [Thermostat Name]** to **ON**
   - The device will be created automatically
5. **Click**: **Done**

**🎉 Setup Complete!** Your thermostats are now available as Hubitat devices.

---

## What You Get

Once setup is complete, you'll have full thermostat devices in Hubitat with:

### Capabilities
- **Thermostat**: Full thermostat control
- **Temperature Measurement**: Current temperature reading
- **Relative Humidity Measurement**: Current humidity reading
- **Refresh**: Manual data refresh
- **Sensor**: General sensor capabilities
- **Actuator**: Control capabilities

### Attributes
- `temperature`: Current indoor temperature
- `humidity`: Current indoor humidity
- `thermostatSetpoint`: Current target temperature
- `heatingSetpoint`: Heat mode setpoint
- `coolingSetpoint`: Cool mode setpoint
- `thermostatMode`: Current mode (heat/cool/auto/off)
- `thermostatFanMode`: Fan mode (auto/on/circulate)
- `thermostatOperatingState`: Current state (heating/cooling/idle/fan only)
- `outdoorTemperature`: Outside temperature
- `equipmentStatus`: Equipment status
- `setpointStatus`: Hold status
- `lastUpdate`: Last data update timestamp

### Commands
- `setHeatingSetpoint(temperature)`: Set heating temperature
- `setCoolingSetpoint(temperature)`: Set cooling temperature
- `setThermostatMode(mode)`: Set HVAC mode
- `setThermostatFanMode(fanMode)`: Set fan mode
- `heat()`, `cool()`, `auto()`, `off()`: Quick mode changes
- `fanAuto()`, `fanOn()`, `fanCirculate()`: Quick fan changes
- `refresh()`: Update device data

## Settings

### App Settings
- **Enable debug logging**: Detailed logging for troubleshooting
- **Enable descriptionText logging**: Status change notifications

### Device Settings
- **Temperature Scale**: F or C (Fahrenheit or Celsius)
- **Auto Refresh Interval**: Minutes between automatic updates (default: 5)
- **Enable debug logging**: Device-level debug logging
- **Enable descriptionText logging**: Device status notifications

## Automatic Features

### Token Management
- Tokens automatically refresh every 23 hours
- Failed API calls trigger immediate token refresh
- Expired tokens are handled gracefully

### Data Updates
- All devices refresh every 5 minutes by default
- Manual refresh available via device command
- Automatic refresh after successful control commands

### Error Handling
- API connection failures are logged and retried
- Invalid commands return meaningful error messages
- Device discovery handles missing or offline thermostats

## Troubleshooting

### If Built-in OAuth Fails
1. **"Browser didn't open"**: Click the OAuth link manually in the app
2. **"Authorization failed"**: Check you're using your **regular** Resideo account
3. **"Permission denied"**: Make sure you clicked "Allow" on Resideo's page
4. **"Redirect failed"**: Your Hubitat hub may not be accessible externally

### Fallback: Manual Token Entry
If OAuth doesn't work:
1. Use the **Manual Token Entry** option in the app
2. Follow the manual OAuth process using your web browser
3. Copy/paste the access and refresh tokens into the manual entry fields

### Authentication Issues in Hubitat
1. **"Not connected" after OAuth**: Check Hubitat logs for OAuth errors
2. **"Unauthorized" errors**: Try the "Refresh API Token" option in the app
3. **OAuth redirects failing**: Ensure your hub has proper network access

### Device Discovery Problems
1. **No thermostats found**: Verify thermostats are online in the Resideo mobile app
2. **Can't install devices**: Enable debug logging to see detailed API responses
3. **Devices offline**: Try the "Refresh Discovery" option in the app
4. **Wrong thermostats**: Make sure you're logged into the correct Resideo account

### Control Issues
1. Ensure thermostats are not in a schedule hold
2. Check temperature values are within valid ranges (45-99°F)
3. Verify HVAC system is operational
2. Review device logs for API error messages

### Performance Issues
1. Reduce auto-refresh interval if experiencing delays
2. Disable debug logging in production
3. Check Hubitat hub system performance

## Advanced Configuration

### Custom Refresh Intervals
Each device can have its own refresh interval. Set to 0 to disable auto-refresh for a specific device.

### Integration with Rules and Apps
The thermostat devices work with all standard Hubitat apps:
- **Rule Machine**: Create automation rules
- **Thermostat Scheduler**: Schedule temperature changes
- **Hubitat Dashboard**: Add to dashboards
- **Maker API**: External control via HTTP

### Multiple Locations
If you have thermostats in multiple Resideo locations, they will all be discovered and can be installed as separate devices.

## Support

### Logs Location
- App logs: **Apps** → **Resideo Direct API Integration** → **App Status**
- Device logs: **Devices** → Select thermostat → **Events** and **Logs**

### Common Log Messages
- "✅ Connected to Resideo API": Authentication successful
- "Access token refreshed successfully": Token refresh completed
- "Thermostat command successful": Control command completed
- "API Error" messages: Check API connectivity and tokens

---

## Need Help?

### Quick Help
- **Re-run OAuth setup**: Use the "Refresh API Token" option in the app
- **Test API connection**: Check app logs in Hubitat
- **Start over**: Delete the app and re-install with fresh OAuth

### Common Issues
| Problem | Solution |
|---------|----------|
| Tokens expired | Use "Refresh API Token" in the app |
| No thermostats found | Check Resideo mobile app |
| Control not working | Verify thermostats online |
| Setup failed | Use your regular Resideo account |

For technical support, check the project repository or Hubitat community forums.