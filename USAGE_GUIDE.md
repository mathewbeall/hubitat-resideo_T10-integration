# Hubitat Usage Guide

How to use your Resideo T10 thermostats through Hubitat after installation.

## Device Control

### Basic Temperature Control

**From Device Page**:
1. Go to **Devices** → Find your thermostat
2. Use the **temperature controls** to adjust setpoints:
   - **Heating Setpoint**: Set desired heating temperature
   - **Cooling Setpoint**: Set desired cooling temperature
3. Change **modes** using the buttons:
   - **Heat**, **Cool**, **Auto**, **Off**

**From Dashboard**:
1. Add thermostat to your dashboard
2. Use the **thermostat tile** for quick control
3. Tap numbers to adjust temperatures
4. Tap mode buttons to switch heat/cool/auto/off

### Fan Control

**Available Modes**:
- **Auto**: Fan runs only when heating/cooling
- **On**: Fan runs continuously
- **Circulate**: Fan cycles periodically for air circulation

**Control Methods**:
- **Device Page**: Use fan mode buttons or dropdown
- **Dashboard**: Add fan control tile
- **Voice**: "Alexa, turn the fan on" (if Alexa integrated)

### Mode Changes

**HVAC Modes**:
- **Heat**: Only heating available
- **Cool**: Only cooling available
- **Auto**: Automatic heating/cooling based on setpoints
- **Off**: HVAC system turned off (fan may still run)
- **Emergency Heat**: Activates auxiliary/backup heating (heat pump systems only)

**Quick Commands**:
- Use individual mode buttons on device page
- Or use the main mode dropdown selector
- Use `emergencyHeat()` command for emergency/auxiliary heat

### Emergency Heat (Heat Pump Systems)

**What is Emergency Heat?**
Emergency heat bypasses the heat pump and uses only the auxiliary heating elements (electric resistance or gas backup). Use this when:
- Outdoor temperature is extremely low (below heat pump efficiency range)
- Heat pump is malfunctioning
- Faster heating is needed in very cold conditions

**How to Activate**:
- **Device Page**: Click `emergencyHeat` command button
- **Dashboard**: Set mode to "emergency heat"
- **Rule Machine**: Use `setThermostatMode("emergency heat")` or `emergencyHeat()` command
- **Voice**: "Alexa, set thermostat to emergency heat"

**Important Notes**:
- Emergency heat uses more energy than normal heat pump operation
- Only available on systems with auxiliary heating
- Mode will show as "emergency heat" when active
- Switch to regular "heat" mode when conditions allow
- If your thermostat doesn't support emergency heat (no heat pump with auxiliary heating), the command will fail with an error and restore your previous mode

## Advanced Features

### Schedule Override

**Hold Temperature**:
1. Set desired temperature on device
2. Temperature is held until manually changed
3. Overrides any programmed schedules

**Resume Schedule**:
1. Go to **Device Commands**
2. Use `setSchedule("schedule")` to resume programmed schedule
3. Or `setSchedule("hold")` to maintain current settings

### Auto Refresh

**Configuration**:
1. Go to **Device Preferences**
2. Set **Auto Refresh Interval** (1-60 minutes)
3. Lower values = more frequent updates, higher API usage
4. Higher values = less frequent updates, better performance

**Manual Refresh**:
- Click **Refresh** command anytime for immediate update
- Useful when you've changed settings on the physical thermostat

## Dashboard Integration

### Creating Thermostat Dashboard

**Add Device**:
1. **Dashboards** → Select your dashboard → **Settings**
2. **Add Device** → Select your thermostat
3. Choose **thermostat template** for full control

**Tile Options**:
- **Full Thermostat**: Complete control interface
- **Temperature Only**: Just shows current temperature
- **Mode Control**: Only HVAC mode buttons
- **Custom**: Select specific attributes to display

### Multiple Thermostats

**Separate Controls**:
- Each thermostat appears as individual device
- Control each zone independently
- Create separate dashboard tiles for each

**Group Control** (via Rule Machine):
- Create rules to control multiple thermostats together
- Set "whole house" temperatures
- Coordinate mode changes across zones

## Rule Machine Integration

### Basic Automation Examples

**Morning Routine**:
```
IF Time is 7:00 AM
THEN Set Thermostat Mode: Cool
AND Set Cooling Setpoint: 72
```

**Away Mode**:
```
IF Mode changes to Away
THEN Set Cooling Setpoint: 78 (summer)
AND Set Heating Setpoint: 65 (winter)
```

**Bedtime**:
```
IF Time is 10:00 PM
THEN Set Cooling Setpoint: 68
```

### Advanced Automations

**Temperature Based**:
```
IF Outdoor Temperature > 85
THEN Set Cooling Setpoint: 74
ELSE Set Cooling Setpoint: 72
```

**Occupancy Based**:
```
IF Motion Sensor inactive for 2 hours
THEN Set Thermostat Mode: Off
```

**Seasonal Switching**:
```
IF Month is October through March
THEN Set Thermostat Mode: Heat
ELSE Set Thermostat Mode: Cool
```

## Voice Control

### Alexa Integration

**Setup**: Ensure Hubitat Alexa skill is connected

**Commands**:
- "Alexa, set the thermostat to 72 degrees"
- "Alexa, turn on the air conditioning"
- "Alexa, what's the temperature in the living room?"
- "Alexa, turn off the thermostat"
- "Alexa, set thermostat to emergency heat"

### Google Assistant

**Setup**: Connect Hubitat to Google Assistant

**Commands**:
- "Hey Google, set thermostat to heat mode"
- "Hey Google, make it cooler"
- "Hey Google, what's the temperature upstairs?"

## Mobile App Control

### Hubitat Mobile App

**Device Control**:
1. Open Hubitat mobile app
2. Go to **Devices** tab
3. Find your thermostats
4. Tap for quick controls

**Dashboard Access**:
1. **Dashboards** tab
2. Select dashboard with thermostats
3. Full control from phone/tablet

### Remote Access

**Requirements**:
- Hubitat cloud connection enabled
- Mobile app connected to your hub
- Internet access on phone

**Features**:
- Control from anywhere
- View current status
- Get notifications (via rules)

## Monitoring & Status

### Current Information Available

**Temperature Data**:
- Current room temperature
- Current humidity level
- Outdoor temperature
- Target heating/cooling setpoints

**System Status**:
- Current HVAC mode (heat/cool/auto/off)
- Fan mode (auto/on/circulate)
- Operating state (heating/cooling/idle)
- Equipment status (detailed system state)

### Historical Data

**Hubitat Logging**:
- Temperature changes logged automatically
- Mode changes tracked
- Error events recorded

**Third-party Integration**:
- Export data to InfluxDB
- Display in Grafana dashboards
- Integration with other monitoring tools

## Troubleshooting Usage Issues

### Common Problems

**Thermostat Not Responding**:
1. Check device status in Hubitat
2. Verify thermostat is online (green status)
3. Try manual refresh command
4. Check physical thermostat Wi-Fi connection

**Wrong Temperature Display**:
1. Check **Temperature Scale** in device preferences
2. Change between Fahrenheit/Celsius as needed
3. Save preferences and refresh device

**Commands Delayed**:
1. Increase auto-refresh interval to reduce API calls
2. Check internet connection speed
3. Verify Resideo service status

### Performance Optimization

**Reduce API Calls**:
- Set auto-refresh to 5-15 minutes instead of 1-2 minutes
- Disable debug logging after troubleshooting
- Don't refresh manually too frequently

**Hub Performance**:
- Monitor hub memory usage
- Disable unused device features
- Regular hub maintenance

## Best Practices

### Daily Usage

- **Use dashboard tiles** for quick adjustments
- **Set reasonable schedules** to minimize manual changes
- **Monitor outdoor temperature** for seasonal adjustments
- **Check device status** if behavior seems unusual

### Energy Efficiency

- **Wider temperature ranges** when away from home
- **Auto mode** for most efficient operation
- **Fan circulation** for better air distribution
- **Schedule coordination** with occupancy patterns

### Maintenance

- **Weekly**: Check device status and logs
- **Monthly**: Review auto-refresh intervals
- **Seasonally**: Update temperature preferences
- **As needed**: Refresh OAuth tokens if connection issues

## Getting More Help

See **TROUBLESHOOTING.md** for specific issues and **HUBITAT_INSTALLATION.md** for setup problems.