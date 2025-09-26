# Troubleshooting Guide

Common issues and solutions for the Hubitat Resideo T10 integration.

## Installation Issues

### Cannot Find App or Driver

**Problem**: Can't find "Resideo Direct API Integration" app or driver options.

**Solution**:
1. Verify code was pasted correctly in **Apps Code** and **Drivers Code**
2. Check for syntax errors - look for red text in the code editor
3. Click **Save** after pasting the code
4. Refresh your browser and try again

### OAuth Not Available

**Problem**: Can't find OAuth option or getting OAuth errors.

**Solution**:
1. Go to **Apps Code** → Find your Resideo app
2. Click the **⋮ menu** (three dots) next to **Save**
3. Select **OAuth** → **Enable OAuth in App** → **Update**
4. Return to **Apps** and try connecting again

### App Won't Save/Install

**Problem**: Error messages when trying to save or install the app.

**Solution**:
1. Check for **missing commas** or **brackets** in the code
2. Ensure you copied the **complete file** (first line should be `/**`)
3. Look for error messages in red at the bottom of the code editor
4. Try refreshing the page and pasting the code again

## Connection Issues

### OAuth Connection Fails

**Problem**: "Connect to Resideo API" fails or shows errors.

**Solution**:
1. **Verify Credentials**: Ensure Consumer Key/Secret are correct
2. **Check Callback URL**: Copy the exact callback URL to your Resideo developer app
3. **Browser Issues**: Try a different browser or clear cache
4. **Account Match**: Use the SAME Resideo account for developer portal AND OAuth login

### "Not Connected" After OAuth

**Problem**: Completed OAuth but app shows "Not connected".

**Solution**:
1. Check **Hubitat Logs** (gear icon → Logs) for error details
2. Try the **"Refresh API Token"** option in the app
3. Verify the callback URL is exactly correct in Resideo developer portal
4. Delete the app and start over with fresh OAuth setup

### Callback URL Issues

**Problem**: OAuth redirects fail or show "page not found".

**Solution**:
1. **Copy URL Exactly**: Use the multi-line text box in the app to copy the full URL
2. **Hub Access**: Ensure your Hubitat hub is accessible from the internet
3. **Network Issues**: Check firewall/router settings
4. **Cloud vs Local**: Make sure you're accessing Hubitat the same way consistently

## Device Discovery Issues

### No Thermostats Found

**Problem**: "Discover Thermostats" finds no devices.

**Solution**:
1. **Check Mobile App**: Verify thermostats appear in the Resideo/Honeywell Home app
2. **Account Match**: Ensure OAuth used the same account as your mobile app
3. **Device Online**: Confirm thermostats are connected to Wi-Fi
4. **Refresh Discovery**: Try the discovery process again after a few minutes

### Can't Install Devices

**Problem**: Thermostats found but won't install as devices.

**Solution**:
1. **Driver Installation**: Verify the driver code was installed correctly
2. **Enable Debug**: Turn on debug logging in the app to see detailed errors
3. **One at a Time**: Try installing one thermostat at a time
4. **Hub Resources**: Ensure your hub has available device slots

## Control Issues

### Commands Don't Work

**Problem**: Can set temperature or modes but thermostat doesn't respond.

**Solution**:
1. **Device Status**: Check if thermostat shows "offline" in Hubitat
2. **Temperature Limits**: Verify setpoints are within valid range (45-99°F)
3. **Mode Conflicts**: Ensure thermostat isn't in a schedule hold mode
4. **HVAC System**: Check that physical HVAC system is operational

### Status Not Updating

**Problem**: Thermostat device shows old/incorrect information.

**Solution**:
1. **Manual Refresh**: Click the "Refresh" command on the device
2. **Auto Refresh**: Check auto refresh interval in device preferences
3. **Network Issues**: Verify thermostat has good Wi-Fi connection
4. **API Issues**: Check app logs for API communication errors

### Temperature Scale Wrong

**Problem**: Shows Celsius instead of Fahrenheit (or vice versa).

**Solution**:
1. Go to **Device Preferences**
2. Change **Temperature Scale** to your preference
3. Click **Save Preferences**
4. Refresh the device to update display

## Performance Issues

### Slow Response

**Problem**: Commands take a long time to execute or show up.

**Solution**:
1. **Reduce Refresh Rate**: Increase auto refresh interval to reduce API calls
2. **Hub Performance**: Check overall hub performance and available memory
3. **Network Speed**: Verify good internet connection speed
4. **Disable Debug**: Turn off debug logging in production use

### Hub Slowdown

**Problem**: Hubitat hub seems slower after installing integration.

**Solution**:
1. **Debug Logging**: Disable debug logging on all devices
2. **Refresh Interval**: Increase refresh intervals to reduce load
3. **Remove Unused**: Delete any test devices you don't need
4. **Hub Reboot**: Sometimes a hub reboot helps with performance

## Error Messages

### "400 Bad Request"

**Problem**: API calls failing with 400 errors.

**Solution**:
1. **Invalid Parameters**: Check temperature values are reasonable
2. **Token Issues**: Try refreshing the API token
3. **Account Status**: Verify Resideo account is in good standing
4. **Rate Limiting**: Wait a few minutes and try again

### "401 Unauthorized"

**Problem**: Authentication failures.

**Solution**:
1. **Token Expired**: Use "Refresh API Token" in the app
2. **Credentials Wrong**: Verify Consumer Key/Secret are correct
3. **Account Issues**: Check Resideo account status
4. **Re-OAuth**: Delete app and set up OAuth again

### "Device Not Found"

**Problem**: Thermostat device can't be controlled.

**Solution**:
1. **Discovery Refresh**: Run thermostat discovery again
2. **Device ID**: Check device network ID matches thermostat ID
3. **Account Access**: Verify thermostat is in the connected account
4. **Reinstall Device**: Delete and reinstall the specific device

## Advanced Troubleshooting

### Debug Logging

Enable debug logging to get detailed information:

1. **App Debugging**: Enable in app settings
2. **Device Debugging**: Enable in device preferences
3. **View Logs**: Go to **Logs** (gear icon) to see detailed messages
4. **Disable When Done**: Turn off debug logging after troubleshooting

### Starting Over

If all else fails, start completely fresh:

1. **Delete Devices**: Remove all thermostat devices
2. **Delete App**: Remove the Resideo app
3. **Clear Tokens**: Remove any saved OAuth tokens
4. **Reinstall**: Follow installation guide from the beginning
5. **New OAuth**: Set up OAuth connection from scratch

### Network Diagnostics

Check network connectivity:

1. **Hub Internet**: Verify hub has internet access
2. **Thermostat Wi-Fi**: Check thermostat network connection
3. **Mobile App**: Confirm mobile app can control thermostats
4. **API Status**: Check Resideo/Honeywell service status

## Getting More Help

### Log Information

When asking for help, include:
- **Hubitat Model**: C-5, C-7, C-8
- **Firmware Version**: Found in hub settings
- **Error Messages**: Copy exact error text from logs
- **Steps Tried**: What troubleshooting you've already attempted

### Community Resources

- **Hubitat Community**: community.hubitat.com
- **GitHub Issues**: Report bugs on the project repository
- **Documentation**: Review all README and installation guides

### Common Solutions Summary

| Problem | Quick Fix |
|---------|-----------|
| OAuth fails | Check callback URL exactly |
| No devices found | Verify mobile app works |
| Commands don't work | Check temperature ranges |
| Slow performance | Disable debug logging |
| Connection lost | Refresh API token |
| Can't install | Check OAuth is enabled |

Most issues are related to **OAuth setup** or **callback URL configuration**. Double-check these first!