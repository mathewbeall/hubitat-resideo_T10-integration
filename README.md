# Resideo T10 Thermostat Control

Complete Python solution for controlling Resideo T10 thermostats with **two different API approaches**.

## 🌟 Overview

This project provides programmatic control for Resideo T10 thermostats using both:

1. **Seam API** - Third-party service (simple setup)
2. **Direct Resideo API** - Official Honeywell/Resideo API (full features)

Both approaches are fully functional and can control temperature, HVAC modes, fan settings, and provide real-time status monitoring.

## 📁 Project Structure

```
resideo_thermostat/
├── README.md                     # This file - main overview
├── SEAM_API.md                   # Seam API documentation
├── DIRECT_API.md                 # Direct Resideo API documentation
├── TROUBLESHOOTING.md            # Common issues and solutions
│
├── Seam API Files:
├── thermostat_controller.py      # Main Seam API controller
├── example.py                    # Seam API examples and interactive mode
├── USAGE_GUIDE.md               # Quick Seam usage reference
│
├── Direct API Files:
├── resideo_direct_api.py         # Direct Resideo API controller
├── interactive_direct_control.py # Full interactive control interface
├── resideo_direct_test.py        # Direct API testing script
├── setup_with_credentials.py     # OAuth2 setup (automatic)
├── manual_setup.py               # OAuth2 setup (manual)
├── get_auth_url.py              # Get OAuth authorization URL
├── complete_setup.py            # Complete OAuth with auth code
├── RESIDEO_DIRECT_API_SETUP.md  # Detailed setup guide
│
└── Environment:
    ├── venv/                     # Python virtual environment
    ├── resideo_tokens.json      # OAuth tokens (created after setup)
    └── requirements.txt         # Dependencies
```

## 🚀 Quick Start

### Option 1: Seam API (Recommended for beginners)
```bash
# Already set up and working!
source venv/bin/activate
python thermostat_controller.py    # View both thermostats
python example.py --interactive     # Interactive control
```

### Option 2: Direct Resideo API (Advanced users)
```bash
source venv/bin/activate
python interactive_direct_control.py  # Full interactive control
```

## 🏠 Your Setup

- **Thermostats**: 2× Resideo T10 Smart Thermostats
  - **Downstairs Thermostat** (`LCC-48A2E672B0D5`)
  - **Upstairs Thermostat** (`LCC-48A2E6750237`)
- **Location**: Mission Viejo, CA
- **Both APIs**: Fully configured and operational

## 📊 Available Data

Both APIs provide:
- **Current Temperature** (°F)
- **Humidity** (%)
- **HVAC Mode** (Heat/Cool/Auto/Off)
- **Temperature Setpoints** (heating/cooling targets)
- **Fan Mode** (Auto/On/Circulate)
- **System Status** (actively heating/cooling/idle)

## 🎮 Control Features

Both APIs support:
- **Set HVAC Modes**: Heat, Cool, Auto, Off
- **Adjust Temperatures**: Heating and cooling setpoints
- **Fan Control**: Auto, On, Circulate modes
- **Multi-thermostat**: Control either thermostat individually
- **Quick Presets**: Pre-configured comfort settings

## 🔄 API Comparison

| Feature | Seam API | Direct Resideo API |
|---------|----------|-------------------|
| **Setup Complexity** | ✅ Simple (API key) | 🔶 Complex (OAuth2) |
| **Authentication** | ✅ Static API key | 🔶 Token refresh required |
| **Reliability** | ✅ Very stable | ✅ Official API |
| **Data Freshness** | ✅ Real-time | ✅ Real-time |
| **Control Features** | ✅ Full control | ✅ Full control + more |
| **Rate Limits** | 🔶 Seam's limits | ✅ Generous |
| **Cost** | 🔶 Seam pricing | ✅ Free |
| **Future-proof** | 🔶 Depends on Seam | ✅ Official API |

## 📖 Detailed Documentation

- **[SEAM_API.md](SEAM_API.md)** - Complete Seam API guide
- **[DIRECT_API.md](DIRECT_API.md)** - Direct Resideo API guide
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common issues and fixes

## 🛠️ Requirements

- **Python 3.7+**
- **Internet connection**
- **Thermostats connected to Wi-Fi**
- **Honeywell Home account** (both APIs)
- **Seam account** (for Seam API)
- **Resideo Developer account** (for Direct API)

## ⚡ Quick Examples

### Check Both Thermostats (Seam)
```python
from thermostat_controller import ThermostatController

controller = ThermostatController('seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4')
thermostats = controller.list_thermostats()

for thermostat in thermostats:
    status = controller.get_thermostat_status(thermostat['device_id'])
    print(f"{thermostat['name']}: {status['current_temperature']}°F")
```

### Set Temperature (Direct API)
```python
from resideo_direct_api import ResideoAPIController

controller = ResideoAPIController("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET")
controller.load_tokens()

# Set downstairs to cool mode at 75°F
result = controller.set_temperature('LCC-48A2E672B0D5', 'Cool', cool_setpoint=75)
print(result)  # {'success': True, 'data': {}}
```

## 🎯 Common Use Cases

### Home Automation
- **Morning routine**: Set to comfort temperature
- **Away mode**: Energy-saving temperatures
- **Sleep schedule**: Cooler nights
- **Season switching**: Heat ↔ Cool modes

### Energy Management
- **Peak hours**: Higher/lower setpoints
- **Unoccupied zones**: Turn off unused areas
- **Weather integration**: Adjust based on outdoor temperature
- **Usage monitoring**: Track heating/cooling patterns

### Smart Controls
- **Voice integration**: "Set temperature to 72"
- **Mobile app**: Remote control interface
- **Scheduling**: Time-based temperature changes
- **Sensors**: Room-by-room control

## 🔒 Security Notes

- **API keys are hardcoded** for development convenience
- **For production**: Use environment variables
- **Tokens auto-refresh** (Direct API only)
- **Local network**: All control is over internet APIs

## 🆘 Getting Help

1. **Check the troubleshooting guide**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
2. **Review API-specific docs**: [SEAM_API.md](SEAM_API.md) or [DIRECT_API.md](DIRECT_API.md)
3. **Test with the example scripts** to isolate issues
4. **Both APIs are working** - try the other one if issues persist

## 🏆 Success Status

✅ **Seam API**: Fully operational
✅ **Direct Resideo API**: Fully operational
✅ **Both T10 thermostats**: Discovered and controllable
✅ **Temperature control**: Working perfectly
✅ **Interactive interfaces**: Ready to use

Your thermostat control system is complete and ready for automation! 🎉