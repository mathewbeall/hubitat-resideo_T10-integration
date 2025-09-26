# Quick Usage Guide for Resideo T10 Thermostat Controller

## Your Setup
- **API Key**: `seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4` (hardcoded)
- **Thermostats**: 2 T10 units connected via Seam
  - Downstairs Thermostat (`a37802f2-da16-4fae-aac7-7ad3aaa267b8`)
  - Upstairs Thermostat (`abd587ec-7ed1-4ed0-a732-df974a041d9c`)

## Quick Commands

### 1. Check Status of Both Thermostats
```bash
source venv/bin/activate
python thermostat_controller.py
```
**Output**: Lists both thermostats with current temp, HVAC mode, setpoints, system activity

### 2. Interactive Control Mode
```bash
source venv/bin/activate
python example.py --interactive
```
**Features**:
- Select which thermostat to control
- Real-time status checks
- Set HVAC modes (heat/cool/auto/off)
- Adjust temperature setpoints
- Change fan modes

### 3. View Both Thermostats Status
```bash
source venv/bin/activate
python example.py
```
**Output**: Detailed status of both thermostats with emojis

### 4. Demo Control Operations
```bash
source venv/bin/activate
python example.py --control
```
**Actions**: Automatically tests setting modes and temperatures on first thermostat

## Python Code Usage

### Basic Status Check
```python
from thermostat_controller import ThermostatController

controller = ThermostatController('seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4')

# Get status of first thermostat
status = controller.get_thermostat_status()
print(f"Temperature: {status['current_temperature']}°F")
print(f"Mode: {status['hvac_mode']}")

# Get status of specific thermostat
upstairs_status = controller.get_thermostat_status('abd587ec-7ed1-4ed0-a732-df974a041d9c')
```

### Control Operations
```python
# Set to auto mode with comfortable temps
controller.set_hvac_mode('auto')
controller.set_temperature(heating_temp=68, cooling_temp=75)

# Control specific thermostat
controller.set_hvac_mode('heat', device_id='abd587ec-7ed1-4ed0-a732-df974a041d9c')
controller.set_temperature(heating_temp=70, device_id='abd587ec-7ed1-4ed0-a732-df974a041d9c')

# Turn off thermostat
controller.set_hvac_mode('off')
```

## Available Data Points

### Status Dictionary Contains:
- `current_temperature`: Current temp in °F
- `humidity`: Relative humidity (0-1 scale)
- `hvac_mode`: Current mode (off/heat/cool/heat_cool)
- `heating_setpoint`: Target heating temp
- `cooling_setpoint`: Target cooling temp
- `fan_mode`: Fan setting (auto/on/circulate)
- `is_heating`: True if actively heating
- `is_cooling`: True if actively cooling
- `is_fan_running`: True if fan is running

### Control Options:
- **HVAC Modes**: `heat`, `cool`, `auto` (heat_cool), `off`
- **Fan Modes**: `auto`, `on`, `circulate`
- **Temperatures**: Any value in °F (e.g., 68, 72.5)

## Tested & Working Features

✅ Read current temperature from both thermostats
✅ Read HVAC status (on/off/auto)
✅ Read system activity (heating/cooling/fan running)
✅ Set HVAC modes
✅ Set temperature setpoints
✅ Control individual thermostats
✅ Real-time status updates

## File Structure
- `thermostat_controller.py` - Main controller class
- `example.py` - Demo scripts and interactive mode
- `README.md` - Detailed documentation
- `USAGE_GUIDE.md` - This quick reference

## Troubleshooting
- If thermostats show as offline, check Wi-Fi connection
- Changes may take 2-5 seconds to reflect in status
- Use `controller._refresh_thermostats()` to force refresh