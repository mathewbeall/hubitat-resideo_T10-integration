# Hubitat Resideo T10 Integration

A complete Hubitat Elevation integration for Resideo T10 smart thermostats using the official Honeywell/Resideo API.

## 🌟 Overview

This integration provides native Hubitat control for Resideo T10 thermostats with full two-way communication. Control your thermostats through Hubitat dashboards, automations, and voice commands while maintaining real-time status updates.

## 📦 Package Contents

```
hubitat-resideo-T10-integration/
├── README.md                     # This file - overview and getting started
├── hubitat-resideo-app.groovy    # Main Hubitat parent app
├── hubitat-resideo-driver.groovy # Thermostat device driver
├── HUBITAT_INSTALLATION.md       # Detailed step-by-step setup guide
├── QUICK_START.md                # Fast setup for experienced users
├── TROUBLESHOOTING.md            # Common issues and solutions
├── USAGE_GUIDE.md               # How to use the integration
└── LICENSE                      # Apache 2.0 license
```

## 🚀 Quick Start

### Prerequisites
- **Hubitat Elevation hub** (C-5, C-7, or C-8)
- **Resideo T10 thermostats** connected to Wi-Fi
- **Honeywell Home account**
- **Resideo Developer account** (free)

### Installation Methods

#### Option 1: Hubitat Package Manager (Recommended)
The easiest way to install - and it **automatically keeps you updated** when new versions are released!

1. **Get developer credentials** from [developer.honeywellhome.com](https://developer.honeywellhome.com)
2. **Open HPM** in your Hubitat Apps
3. **Install** → **Search by Keywords** → Search for "Resideo T10"
4. **Configure OAuth** and add your thermostats

#### Option 2: Manual Installation
If you prefer to install manually:

1. **Get developer credentials** from [developer.honeywellhome.com](https://developer.honeywellhome.com)
2. **Install the app code** in Hubitat (Apps Code → New App)
3. **Install the driver code** in Hubitat (Drivers Code → New Driver)
4. **Configure OAuth** in the app
5. **Add your thermostats** and start controlling!

📖 **Full instructions**: See [HUBITAT_INSTALLATION.md](HUBITAT_INSTALLATION.md)

## 🏠 Features

### Complete Thermostat Control
- ✅ **Temperature Setpoints** - Set heating and cooling targets
- ✅ **HVAC Modes** - Heat, Cool, Auto, Off, Emergency Heat
- ✅ **Emergency Heat** - Activate auxiliary/backup heating for heat pump systems
- ✅ **Fan Control** - Auto, On, Circulate
- ✅ **Real-time Status** - Current temperature, humidity, operating state
- ✅ **Schedule Override** - Hold temperatures or follow schedule

### Hubitat Integration
- ✅ **Native Device Support** - Full thermostat capability
- ✅ **Dashboard Compatible** - Works with all Hubitat dashboards
- ✅ **Rule Machine** - Use in automations and rules
- ✅ **Voice Control** - Alexa, Google Assistant integration
- ✅ **Multiple Thermostats** - Support for multiple T10 units

### Real-time Updates
- ✅ **Two-way Communication** - Status updates from thermostat to Hubitat
- ✅ **Outdoor Temperature** - Weather data from Resideo service
- ✅ **Equipment Status** - Know when heating/cooling is actively running
- ✅ **Auto Refresh** - Configurable update intervals

## 📊 Thermostat Attributes

The integration provides these device attributes:

| Attribute | Description | Example |
|-----------|-------------|---------|
| `temperature` | Current room temperature | `72°F` |
| `humidity` | Current humidity level | `45%` |
| `thermostatMode` | Current HVAC mode (heat, cool, auto, off, emergency heat) | `heat` |
| `thermostatFanMode` | Current fan setting | `auto` |
| `heatingSetpoint` | Heating target temperature | `68°F` |
| `coolingSetpoint` | Cooling target temperature | `74°F` |
| `thermostatOperatingState` | Equipment status | `cooling` |
| `outdoorTemperature` | Outside temperature | `78°F` |
| `equipmentStatus` | Detailed equipment state | `EquipmentOff` |

## 🎮 Control Commands

Use these commands in rules, dashboards, or device pages:

### Temperature Control
- `setHeatingSetpoint(68)` - Set heating target
- `setCoolingSetpoint(74)` - Set cooling target

### Mode Control
- `setThermostatMode("cool")` - Set HVAC mode (heat, cool, auto, off, emergency heat)
- `heat()`, `cool()`, `auto()`, `off()` - Quick mode buttons
- `emergencyHeat()` - Activate emergency/auxiliary heat (for heat pump systems)

### Fan Control
- `setThermostatFanMode("on")` - Set fan mode
- `fanAuto()`, `fanOn()`, `fanCirculate()` - Quick fan buttons

### Utility Commands
- `refresh()` - Update device status immediately

## 🔧 Configuration Options

### Device Preferences
- **Temperature Scale** - Fahrenheit or Celsius display
- **Auto Refresh** - How often to update (1-60 minutes)
- **Debug Logging** - Enable for troubleshooting
- **Description Logging** - Enable status messages

### App Settings
- **API Credentials** - Consumer Key and Secret from Resideo
- **OAuth Authentication** - Secure connection to Resideo API
- **Device Discovery** - Automatically find your thermostats

## 🛡️ Security & Privacy

- ✅ **Official API** - Uses Resideo's official developer API
- ✅ **OAuth 2.0** - Secure authentication with token refresh
- ✅ **Local Processing** - Commands processed on your Hubitat hub
- ✅ **No Cloud Dependencies** - Works even if Hubitat cloud is down
- ✅ **Token Management** - Automatic token refresh and error handling

## 📖 Documentation

- **[HUBITAT_INSTALLATION.md](HUBITAT_INSTALLATION.md)** - Complete setup guide
- **[QUICK_START.md](QUICK_START.md)** - Fast setup for experienced users
- **[USAGE_GUIDE.md](USAGE_GUIDE.md)** - How to use after installation
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common issues and solutions

## 🆘 Getting Help

1. **Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md)** for common issues
2. **Enable debug logging** in device preferences for detailed logs
3. **Verify API credentials** in the Resideo developer portal
4. **Test OAuth connection** by refreshing the token

## 📱 Compatibility

### Hubitat Models
- ✅ **C-5** - Fully supported
- ✅ **C-7** - Fully supported
- ✅ **C-8** - Fully supported

### Thermostat Models
- ✅ **T10 Smart Thermostat** - Fully supported
- ❓ **Other Resideo/Honeywell models** - May work (untested)

### Dashboard Compatibility
- ✅ **Hubitat Dashboard** - Native support
- ✅ **Hubivue** - Full compatibility
- ✅ **SharpTools** - Works perfectly
- ✅ **Hubitat Mobile App** - Full control

## 🎯 Use Cases

### Home Automation
```groovy
// Morning routine - set comfortable temperature
if (time is 7:00 AM) {
    setThermostatMode("cool")
    setCoolingSetpoint(72)
}
```

### Energy Saving
```groovy
// Away mode - save energy when nobody's home
if (presence is "not present") {
    setCoolingSetpoint(78)  // Higher in summer
    setHeatingSetpoint(65)  // Lower in winter
}
```

### Voice Control
- *"Alexa, set the thermostat to 72 degrees"*
- *"Hey Google, turn on the air conditioning"*
- *"Alexa, what's the temperature upstairs?"*

## 🏆 Status

✅ **Integration**: Complete and stable
✅ **API Connection**: Fully operational
✅ **Device Discovery**: Automatic thermostat detection
✅ **Temperature Control**: All modes working
✅ **Real-time Updates**: Two-way communication active
✅ **Documentation**: Complete installation guides

## 📈 Version History

- **v1.3.0** - Dynamic capability detection - supportedThermostatModes now reflects actual thermostat capabilities (emergency heat only shown for thermostats that support it)
- **v1.2.9** - Fix supported modes - use JSON format for JSON_OBJECT attributes
- **v1.2.8** - Fix schedule conflict - rename setSchedule command to setScheduleMode
- **v1.2.7** - Move supported modes init to initialize() to avoid Groovy issues
- **v1.2.6** - Fix emergency heat - use mode='EmergencyHeat' instead of flag approach
- **v1.2.5** - Fix Groovy method resolution by reordering updated() operations
- **v1.2.4** - Fix Groovy scoping error in supported modes initialization
- **v1.2.3** - Fix supported thermostat modes not populating for existing devices
- **v1.2.2** - Emergency heat restores previous mode if not supported
- **v1.2.1** - Emergency heat now verifies thermostat support, shows error if not available
- **v1.2.0** - Added emergency heat support for heat pump systems
- **v1.1.1** - Bug fixes and interface cleanup
- **v1.1.0** - Major UX improvements and workflow enhancements
- **v1.0.0** - Initial release with full thermostat control

## 🤝 Contributing

This is an open-source project! Feel free to:
- Report issues or bugs
- Suggest new features
- Submit pull requests
- Share your automation ideas

## 📄 License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

---

**Ready to get started?** 👉 Check out [HUBITAT_INSTALLATION.md](HUBITAT_INSTALLATION.md) for step-by-step setup instructions!