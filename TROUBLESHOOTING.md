# Troubleshooting Guide

Complete troubleshooting guide for both Seam and Direct Resideo API approaches.

## 🚨 Quick Diagnosis

### Is Everything Working?
Run these quick tests to check your setup:

```bash
source venv/bin/activate

# Test Seam API
python thermostat_controller.py

# Test Direct Resideo API
python resideo_direct_test.py
```

**Expected Results**:
- Both should show 2 thermostats (Downstairs, Upstairs)
- Current temperatures should display
- No error messages

## 🔍 Common Issues

### 1. No Thermostats Found

#### Seam API Issues
```
❌ No thermostats found!
Make sure your T10 thermostats are connected to Seam.
```

**Causes & Solutions**:

1. **API Key Issue**
   ```python
   # Check if API key is correct
   from thermostat_controller import ThermostatController
   controller = ThermostatController('seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4')

   try:
       thermostats = controller.list_thermostats()
       print(f"Found {len(thermostats)} thermostats")
   except Exception as e:
       print(f"API Error: {e}")
   ```

2. **Seam Account Issue**
   - Check https://console.seam.co/
   - Verify thermostats are connected
   - Re-add thermostats if needed

3. **Network Connectivity**
   ```bash
   # Test internet connection
   ping api.seam.co
   ```

#### Direct Resideo API Issues
```
❌ No thermostats found!
Make sure your thermostats are connected to your Honeywell Home account.
```

**Causes & Solutions**:

1. **Token Expired**
   ```python
   from resideo_direct_api import ResideoAPIController
   controller = ResideoAPIController("AnMxR152mAzQAWP21NG6wkEqeO67OvZZ", "k1ps9LGIW7w6Oixs")

   # Check token status
   if controller.load_tokens():
       result = controller.refresh_access_token()
       print("Token refresh:", result)
   else:
       print("No tokens found - need to re-authenticate")
   ```

2. **OAuth Re-authentication Needed**
   ```bash
   # Delete old tokens and re-authenticate
   rm resideo_tokens.json
   python get_auth_url.py
   # Follow the OAuth flow again
   ```

3. **Account Mismatch**
   - Ensure developer account matches Honeywell Home account
   - Check that thermostats are in the same account

### 2. Authentication Errors

#### Seam API Authentication
```
❌ Unauthorized: Invalid API key
```

**Solution**:
- Verify API key: `seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4`
- Check Seam console for key validity
- Regenerate API key if needed

#### Direct API Authentication
```
❌ Failed to refresh token: 401 Client Error: Unauthorized
```

**Solutions**:

1. **Complete Re-authentication**
   ```bash
   # Remove old tokens
   rm resideo_tokens.json

   # Start fresh OAuth flow
   python get_auth_url.py
   # Copy URL to browser, complete authorization
   # Copy authorization code from redirect URL
   python complete_setup.py [YOUR_AUTH_CODE]
   ```

2. **Check Credentials**
   ```python
   # Verify credentials in resideo_direct_api.py
   CLIENT_ID = "AnMxR152mAzQAWP21NG6wkEqeO67OvZZ"
   CLIENT_SECRET = "k1ps9LGIW7w6Oixs"
   ```

### 3. Temperature Control Failures

#### Seam API Control Issues
```
❌ Failed to set temperature: Device not found
```

**Solutions**:

1. **Check Device ID**
   ```python
   thermostats = controller.list_thermostats()
   for t in thermostats:
       print(f"Name: {t['name']}")
       print(f"Device ID: {t['device_id']}")
       print(f"Online: {t['online']}")
   ```

2. **Verify Online Status**
   ```python
   status = controller.get_thermostat_status('device_id_here')
   if not status.get('online'):
       print("Thermostat is offline - check Wi-Fi connection")
   ```

#### Direct API Control Issues
```
❌ Failed to set temperature: API Error: {'code': 400, 'message': '...'}
```

**Solutions**:

1. **Check Current State First**
   ```python
   thermostats = controller.get_thermostats()
   for t in thermostats:
       print(f"Device ID: {t['deviceID']}")
       print(f"Current values: {t['changeableValues']}")
       print(f"Alive: {t['isAlive']}")
   ```

2. **Use Correct Device IDs**
   ```python
   # Your actual device IDs (hardware IDs)
   DOWNSTAIRS = 'LCC-48A2E672B0D5'
   UPSTAIRS = 'LCC-48A2E6750237'
   ```

3. **Test Simple Change**
   ```python
   # Try a simple temperature change
   result = controller.set_temperature(
       device_id='LCC-48A2E672B0D5',
       mode='Cool',
       cool_setpoint=75
   )
   print("Result:", result)
   ```

### 4. Network & Connectivity Issues

#### Internet Connection Problems
```bash
# Test basic connectivity
ping google.com

# Test API endpoints
ping api.seam.co
ping api.honeywell.com
```

#### Firewall/Proxy Issues
```python
# Test with explicit timeout
import requests

try:
    response = requests.get('https://api.seam.co', timeout=10)
    print("Seam API accessible")
except requests.exceptions.ConnectTimeout:
    print("Connection timeout - check firewall/proxy")
except requests.exceptions.ConnectionError:
    print("Connection error - check internet")
```

### 5. Python Environment Issues

#### Virtual Environment Problems
```bash
# Verify virtual environment
which python
python --version

# Check installed packages
pip list | grep seam
pip list | grep requests
```

#### Missing Dependencies
```bash
# Reinstall dependencies
pip install --upgrade seam requests

# For Direct API
pip install --upgrade requests
```

#### Python Version Issues
```bash
# Check Python version (needs 3.7+)
python --version

# If too old, use python3
python3 --version
```

## 🔧 Debugging Tools

### 1. API Connection Tester
```python
def test_apis():
    """Test both APIs quickly"""
    print("=== API Connection Test ===")

    # Test Seam API
    try:
        from thermostat_controller import ThermostatController
        seam = ThermostatController('seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4')
        thermostats = seam.list_thermostats()
        print(f"✅ Seam API: Found {len(thermostats)} thermostats")
    except Exception as e:
        print(f"❌ Seam API: {e}")

    # Test Direct API
    try:
        from resideo_direct_api import ResideoAPIController
        direct = ResideoAPIController("AnMxR152mAzQAWP21NG6wkEqeO67OvZZ", "k1ps9LGIW7w6Oixs")
        direct.load_tokens()
        thermostats = direct.get_thermostats()
        print(f"✅ Direct API: Found {len(thermostats)} thermostats")
    except Exception as e:
        print(f"❌ Direct API: {e}")

# Run the test
test_apis()
```

### 2. Detailed Status Checker
```python
def detailed_status():
    """Get detailed status of all systems"""
    print("=== Detailed System Status ===")

    # Environment check
    import sys
    import os
    print(f"Python version: {sys.version}")
    print(f"Current directory: {os.getcwd()}")
    print(f"Virtual environment: {sys.prefix}")

    # File check
    files_to_check = [
        'thermostat_controller.py',
        'resideo_direct_api.py',
        'resideo_tokens.json'
    ]

    for file in files_to_check:
        if os.path.exists(file):
            print(f"✅ {file}: Found")
        else:
            print(f"❌ {file}: Missing")

    # Package check
    try:
        import seam
        print(f"✅ Seam package: {seam.__version__}")
    except ImportError:
        print("❌ Seam package: Not installed")

    try:
        import requests
        print(f"✅ Requests package: {requests.__version__}")
    except ImportError:
        print("❌ Requests package: Not installed")

# Run detailed check
detailed_status()
```

### 3. Token Validator (Direct API)
```python
def validate_tokens():
    """Validate Direct API tokens"""
    import json
    import os
    from datetime import datetime

    if not os.path.exists('resideo_tokens.json'):
        print("❌ No tokens file found")
        return

    try:
        with open('resideo_tokens.json', 'r') as f:
            tokens = json.load(f)

        print("=== Token Status ===")
        print(f"Access token length: {len(tokens.get('access_token', ''))}")
        print(f"Refresh token length: {len(tokens.get('refresh_token', ''))}")

        if 'expires' in tokens:
            expires = datetime.fromisoformat(tokens['expires'])
            now = datetime.now()
            if expires > now:
                print(f"✅ Token expires: {expires} (valid)")
            else:
                print(f"❌ Token expired: {expires}")
        else:
            print("⚠️  No expiration info")

    except Exception as e:
        print(f"❌ Token file error: {e}")

# Check tokens
validate_tokens()
```

## 🔄 Recovery Procedures

### Complete Reset - Seam API
```bash
# 1. Check Seam console
echo "1. Visit https://console.seam.co/"
echo "2. Verify thermostats are connected"
echo "3. Check API key validity"

# 2. Test with fresh install
pip uninstall seam
pip install seam

# 3. Test basic connection
python -c "
from thermostat_controller import ThermostatController
controller = ThermostatController('seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4')
print('Thermostats:', len(controller.list_thermostats()))
"
```

### Complete Reset - Direct API
```bash
# 1. Remove old tokens
rm resideo_tokens.json

# 2. Fresh OAuth authentication
python get_auth_url.py
# Complete browser authentication
# Get authorization code from redirect URL
python complete_setup.py [YOUR_AUTH_CODE]

# 3. Test connection
python resideo_direct_test.py
```

### Environment Reset
```bash
# 1. Remove virtual environment
rm -rf venv

# 2. Create fresh environment
python3 -m venv venv
source venv/bin/activate

# 3. Install fresh dependencies
pip install seam requests

# 4. Test both APIs
python thermostat_controller.py
python resideo_direct_test.py
```

## 📊 System Requirements Check

### Minimum Requirements
```python
def check_requirements():
    """Check if system meets requirements"""
    import sys
    import platform
    import subprocess

    print("=== System Requirements Check ===")

    # Python version
    python_version = sys.version_info
    if python_version >= (3, 7):
        print(f"✅ Python {python_version.major}.{python_version.minor} (OK)")
    else:
        print(f"❌ Python {python_version.major}.{python_version.minor} (Need 3.7+)")

    # Operating system
    print(f"✅ OS: {platform.system()} {platform.release()}")

    # Internet connectivity
    try:
        import socket
        socket.create_connection(("8.8.8.8", 53), timeout=3)
        print("✅ Internet connection: OK")
    except OSError:
        print("❌ Internet connection: Failed")

    # Disk space
    import shutil
    free_space = shutil.disk_usage('.').free / (1024**3)  # GB
    if free_space > 0.1:  # 100MB
        print(f"✅ Disk space: {free_space:.1f}GB available")
    else:
        print(f"❌ Disk space: Only {free_space:.1f}GB available")

check_requirements()
```

## 🆘 Emergency Contacts

### When All Else Fails

1. **Check Service Status**
   - Seam Status: https://status.seam.co/
   - Honeywell Status: Check developer portal

2. **Validate Thermostats**
   - Use Honeywell Home app directly
   - Verify thermostats are online and responsive

3. **Network Diagnostics**
   ```bash
   # Check DNS resolution
   nslookup api.seam.co
   nslookup api.honeywell.com

   # Check HTTPS connectivity
   curl -I https://api.seam.co
   curl -I https://api.honeywell.com
   ```

4. **Last Resort Reset**
   ```bash
   # Complete project reset
   cd ..
   rm -rf resideo_thermostat
   # Re-clone or recreate the project
   ```

## 📈 Performance Issues

### Slow API Responses
```python
import time

def measure_api_performance():
    """Measure API response times"""
    from thermostat_controller import ThermostatController

    controller = ThermostatController('seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4')

    # Time API calls
    start = time.time()
    thermostats = controller.list_thermostats()
    list_time = time.time() - start

    start = time.time()
    status = controller.get_thermostat_status()
    status_time = time.time() - start

    print(f"List thermostats: {list_time:.2f}s")
    print(f"Get status: {status_time:.2f}s")

    if list_time > 5 or status_time > 5:
        print("⚠️  Slow API responses - check network connection")

measure_api_performance()
```

### Rate Limiting
```python
def check_rate_limits():
    """Check if hitting rate limits"""
    import time
    from thermostat_controller import ThermostatController

    controller = ThermostatController('seam_2SpKUsqH_RHynH7MDb6WxRK6SbUvDRkf4')

    # Make rapid requests to test limits
    for i in range(5):
        try:
            start = time.time()
            thermostats = controller.list_thermostats()
            duration = time.time() - start
            print(f"Request {i+1}: {duration:.2f}s, {len(thermostats)} thermostats")
        except Exception as e:
            if "rate limit" in str(e).lower():
                print(f"❌ Rate limited on request {i+1}")
            else:
                print(f"❌ Error on request {i+1}: {e}")

        time.sleep(1)

check_rate_limits()
```

## 🏆 Success Indicators

When everything is working correctly, you should see:

### ✅ Seam API Working
- `python thermostat_controller.py` shows both thermostats
- Temperature readings display correctly
- HVAC modes show current settings
- Interactive mode responds to commands

### ✅ Direct API Working
- `python resideo_direct_test.py` finds both thermostats
- OAuth tokens load successfully
- Temperature control commands succeed
- Interactive control interface works

### ✅ Both APIs Healthy
- No authentication errors
- Fast response times (< 3 seconds)
- Consistent temperature readings
- Control commands work reliably

Your system currently shows **all success indicators** - both APIs are fully operational! 🎉