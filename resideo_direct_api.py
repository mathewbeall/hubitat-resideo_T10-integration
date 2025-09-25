#!/usr/bin/env python3
"""
Direct Resideo/Honeywell Home API Controller
Controls T10 thermostats using the official Resideo Developer API
"""

import requests
import json
import base64
import urllib.parse
from typing import Dict, List, Optional
from datetime import datetime, timedelta


class ResideoAPIController:
    def __init__(self, client_id: str, client_secret: str, redirect_uri: str = None):
        """
        Initialize the Resideo API controller

        Args:
            client_id (str): Your Resideo Developer API client ID
            client_secret (str): Your Resideo Developer API client secret
            redirect_uri (str): Redirect URI for OAuth2 flow
        """
        self.client_id = client_id
        self.client_secret = client_secret
        self.redirect_uri = redirect_uri or "http://localhost:8080/callback"

        # Resideo API base URLs - correct endpoints based on documentation
        self.auth_base_url = "https://api.honeywell.com/oauth2"
        self.api_base_url = "https://api.honeywell.com/v2"

        self.access_token = None
        self.refresh_token = None
        self.token_expires = None

    def get_authorization_url(self) -> str:
        """
        Generate the OAuth2 authorization URL for user consent

        Returns:
            str: Authorization URL for the user to visit
        """
        params = {
            'response_type': 'code',
            'client_id': self.client_id,
            'redirect_uri': self.redirect_uri,
            'scope': ''  # Try without specific scope
        }

        url = f"{self.auth_base_url}/authorize?" + urllib.parse.urlencode(params)
        return url

    def exchange_code_for_token(self, authorization_code: str) -> Dict:
        """
        Exchange authorization code for access token

        Args:
            authorization_code (str): Code received from OAuth2 callback

        Returns:
            Dict: Token response or error
        """
        # Create Basic Auth header
        credentials = f"{self.client_id}:{self.client_secret}"
        credentials_b64 = base64.b64encode(credentials.encode()).decode()

        headers = {
            'Authorization': f'Basic {credentials_b64}',
            'Content-Type': 'application/x-www-form-urlencoded'
        }

        data = {
            'grant_type': 'authorization_code',
            'code': authorization_code,
            'redirect_uri': self.redirect_uri
        }

        try:
            response = requests.post(f"{self.auth_base_url}/token", headers=headers, data=data)
            response.raise_for_status()

            token_data = response.json()
            self.access_token = token_data.get('access_token')
            self.refresh_token = token_data.get('refresh_token')

            # Calculate token expiration
            expires_in = int(token_data.get('expires_in', 3600))
            self.token_expires = datetime.now() + timedelta(seconds=expires_in)

            return {"success": True, "data": token_data}

        except requests.exceptions.RequestException as e:
            return {"error": f"Failed to exchange code for token: {e}"}

    def refresh_access_token(self) -> Dict:
        """
        Refresh the access token using refresh token

        Returns:
            Dict: Success/failure response
        """
        if not self.refresh_token:
            return {"error": "No refresh token available"}

        credentials = f"{self.client_id}:{self.client_secret}"
        credentials_b64 = base64.b64encode(credentials.encode()).decode()

        headers = {
            'Authorization': f'Basic {credentials_b64}',
            'Content-Type': 'application/x-www-form-urlencoded'
        }

        data = {
            'grant_type': 'refresh_token',
            'refresh_token': self.refresh_token
        }

        try:
            response = requests.post(f"{self.auth_base_url}/token", headers=headers, data=data)
            response.raise_for_status()

            token_data = response.json()
            self.access_token = token_data.get('access_token')

            # Update refresh token if provided
            if 'refresh_token' in token_data:
                self.refresh_token = token_data['refresh_token']

            # Calculate new expiration
            expires_in = int(token_data.get('expires_in', 3600))
            self.token_expires = datetime.now() + timedelta(seconds=expires_in)

            return {"success": True, "message": "Token refreshed successfully"}

        except requests.exceptions.RequestException as e:
            return {"error": f"Failed to refresh token: {e}"}

    def _ensure_valid_token(self) -> bool:
        """
        Ensure we have a valid access token, refresh if needed

        Returns:
            bool: True if valid token is available
        """
        if not self.access_token:
            return False

        # Check if token is about to expire (refresh 5 minutes early)
        if self.token_expires and datetime.now() >= (self.token_expires - timedelta(minutes=5)):
            result = self.refresh_access_token()
            return result.get('success', False)

        return True

    def _make_api_request(self, method: str, endpoint: str, data: Dict = None) -> Dict:
        """
        Make authenticated API request to Resideo

        Args:
            method (str): HTTP method (GET, POST, PUT, DELETE)
            endpoint (str): API endpoint path
            data (Dict): Request data for POST/PUT requests

        Returns:
            Dict: API response or error
        """
        if not self._ensure_valid_token():
            return {"error": "No valid access token available"}

        headers = {
            'Authorization': f'Bearer {self.access_token}',
            'Content-Type': 'application/json'
        }

        # Add apikey parameter to URL (required by Honeywell API)
        url = f"{self.api_base_url}{endpoint}"
        separator = '&' if '?' in url else '?'
        url = f"{url}{separator}apikey={self.client_id}"

        try:
            if method.upper() == 'GET':
                response = requests.get(url, headers=headers)
            elif method.upper() == 'POST':
                response = requests.post(url, headers=headers, json=data)
            elif method.upper() == 'PUT':
                response = requests.put(url, headers=headers, json=data)
            elif method.upper() == 'DELETE':
                response = requests.delete(url, headers=headers)
            else:
                return {"error": f"Unsupported HTTP method: {method}"}

            response.raise_for_status()

            # Handle empty response (successful POST/PUT often return empty content)
            if response.status_code in [200, 201, 202, 204] and len(response.content) == 0:
                return {"success": True, "data": {}}

            try:
                return {"success": True, "data": response.json()}
            except ValueError:
                # Response is not JSON, but status is successful
                return {"success": True, "data": {"response_text": response.text}}

        except requests.exceptions.RequestException as e:
            if hasattr(e, 'response') and e.response is not None:
                try:
                    error_data = e.response.json()
                    return {"error": f"API Error: {error_data}"}
                except:
                    return {"error": f"HTTP {e.response.status_code}: {e.response.text}"}
            else:
                return {"error": f"Request failed: {e}"}

    def get_locations(self) -> Dict:
        """
        Get all locations from Resideo account

        Returns:
            Dict: List of locations or error
        """
        return self._make_api_request('GET', '/locations')

    def get_all_devices(self) -> Dict:
        """
        Get all connected devices from Resideo account

        Returns:
            Dict: List of devices or error
        """
        # Get locations which include devices directly in the response
        locations_result = self.get_locations()
        if not locations_result.get('success'):
            return locations_result

        all_devices = []
        locations = locations_result['data']

        for location in locations:
            # Devices are embedded in the location response
            devices = location.get('devices', [])
            all_devices.extend(devices)

        return {"success": True, "data": all_devices}

    def get_thermostats(self) -> List[Dict]:
        """
        Get all thermostats from the account

        Returns:
            List of thermostat devices
        """
        devices_response = self.get_all_devices()

        if not devices_response.get('success'):
            return []

        devices = devices_response['data']
        # Filter for thermostats - check both deviceType and deviceClass
        thermostats = [device for device in devices
                      if device.get('deviceType') == 'Thermostat' or
                         device.get('deviceClass') == 'Thermostat']

        return thermostats

    def get_thermostat_status(self, device_id: str, location_id: int) -> Dict:
        """
        Get detailed status of a specific thermostat

        Args:
            device_id (str): Thermostat device ID
            location_id (int): Location ID where thermostat is located

        Returns:
            Dict: Thermostat status data
        """
        return self._make_api_request('GET', f'/devices/thermostats/{device_id}?locationId={location_id}')

    def set_thermostat_settings(self, device_id: str, location_id: int, settings: Dict) -> Dict:
        """
        Update thermostat settings

        Args:
            device_id (str): Thermostat device ID
            location_id (int): Location ID where thermostat is located
            settings (Dict): Settings to update

        Returns:
            Dict: Success/failure response
        """
        # Add locationId to URL parameters
        url_params = f'?locationId={location_id}'
        endpoint = f'/devices/thermostats/{device_id}{url_params}'

        print(f"DEBUG: Making API request to {endpoint}")
        print(f"DEBUG: Request data: {settings}")

        return self._make_api_request('POST', endpoint, settings)

    def set_temperature(self, device_id: str, mode: str, heat_setpoint: float = None,
                       cool_setpoint: float = None, location_id: int = None) -> Dict:
        """
        Set thermostat temperature and mode using correct Resideo API format

        Args:
            device_id (str): Thermostat device ID
            mode (str): HVAC mode (Heat, Cool, Auto, Off, etc.)
            heat_setpoint (float): Heating setpoint in Fahrenheit
            cool_setpoint (float): Cooling setpoint in Fahrenheit
            location_id (int): Location ID (will be auto-detected if not provided)

        Returns:
            Dict: Success/failure response
        """
        # Get location_id if not provided
        if location_id is None:
            locations_result = self.get_locations()
            if not locations_result.get('success') or not locations_result['data']:
                return {"error": "Could not get location information"}
            location_id = locations_result['data'][0].get('locationID')

        # Get current changeable values and modify only what we need to change
        locations_result = self.get_locations()
        if not locations_result.get('success'):
            return {"error": "Could not get current thermostat status"}

        # Find the specific thermostat in the locations data
        target_thermostat = None
        for location in locations_result['data']:
            for device in location.get('devices', []):
                if device.get('deviceID') == device_id:
                    target_thermostat = device
                    break
            if target_thermostat:
                break

        if not target_thermostat:
            return {"error": f"Thermostat {device_id} not found"}

        # Get current changeable values and modify only the fields we want to change
        current_changeable_values = target_thermostat.get('changeableValues', {}).copy()

        # Update the values we want to change
        current_changeable_values["mode"] = mode

        if heat_setpoint is not None:
            current_changeable_values["heatSetpoint"] = heat_setpoint

        if cool_setpoint is not None:
            current_changeable_values["coolSetpoint"] = cool_setpoint

        # Use PermanentHold as the setpoint status (keeping the same field name as in the response)
        current_changeable_values["thermostatSetpointStatus"] = "PermanentHold"

        # Debug: Print what we're about to send
        print(f"DEBUG: About to send request body: {current_changeable_values}")

        # According to documentation, POST the changeable values directly (not wrapped)
        return self.set_thermostat_settings(device_id, location_id, current_changeable_values)

    def set_fan_mode(self, device_id: str, fan_mode: str, location_id: int = None) -> Dict:
        """
        Set thermostat fan mode

        Args:
            device_id (str): Thermostat device ID
            fan_mode (str): Fan mode (Auto, On, Circulate, etc.)
            location_id (int): Location ID (will be auto-detected if not provided)

        Returns:
            Dict: Success/failure response
        """
        valid_modes = ['auto', 'on', 'circulate']
        if fan_mode.lower() not in valid_modes:
            return {"error": f"Invalid fan mode '{fan_mode}'. Valid modes: {valid_modes}"}

        # Get location_id if not provided
        if location_id is None:
            locations_result = self.get_locations()
            if not locations_result.get('success') or not locations_result['data']:
                return {"error": "Could not get location information"}
            location_id = locations_result['data'][0].get('locationID')

        # Try the direct fan endpoint first
        endpoint = f'/devices/thermostats/{device_id}/fan?locationId={location_id}'
        return self._make_api_request('POST', endpoint, {"mode": fan_mode.title()})

    def save_tokens(self, filename: str = "resideo_tokens.json") -> bool:
        """
        Save tokens to a file for persistence

        Args:
            filename (str): File to save tokens to

        Returns:
            bool: Success status
        """
        try:
            token_data = {
                'access_token': self.access_token,
                'refresh_token': self.refresh_token,
                'expires': self.token_expires.isoformat() if self.token_expires else None,
                'client_id': self.client_id
            }

            with open(filename, 'w') as f:
                json.dump(token_data, f, indent=2)

            return True
        except Exception as e:
            print(f"Failed to save tokens: {e}")
            return False

    def load_tokens(self, filename: str = "resideo_tokens.json") -> bool:
        """
        Load tokens from a file

        Args:
            filename (str): File to load tokens from

        Returns:
            bool: Success status
        """
        try:
            with open(filename, 'r') as f:
                token_data = json.load(f)

            self.access_token = token_data.get('access_token')
            self.refresh_token = token_data.get('refresh_token')

            if token_data.get('expires'):
                self.token_expires = datetime.fromisoformat(token_data['expires'])

            return True
        except Exception as e:
            print(f"Failed to load tokens: {e}")
            return False


def main():
    """
    Example usage - requires manual OAuth2 setup
    """
    print("Resideo Direct API Controller")
    print("=" * 40)
    print("This requires setting up OAuth2 credentials with Resideo.")
    print("Visit: https://developer.honeywellhome.com/")
    print()

    # You'll need to replace these with your actual API credentials
    CLIENT_ID = "your_client_id_here"
    CLIENT_SECRET = "your_client_secret_here"

    if CLIENT_ID == "your_client_id_here":
        print("❌ Please set up your Resideo API credentials first!")
        print("See the setup guide for instructions.")
        return

    controller = ResideoAPIController(CLIENT_ID, CLIENT_SECRET)

    # Try to load existing tokens
    if controller.load_tokens():
        print("✅ Loaded existing tokens")
    else:
        print("❌ No existing tokens found")
        print("You need to complete OAuth2 flow first")
        print(f"Visit: {controller.get_authorization_url()}")
        return

    # Test getting devices
    print("\n📡 Getting devices...")
    thermostats = controller.get_thermostats()

    if not thermostats:
        print("❌ No thermostats found")
        return

    print(f"✅ Found {len(thermostats)} thermostat(s):")
    for i, thermostat in enumerate(thermostats):
        print(f"{i+1}. {thermostat.get('userDefinedDeviceName', 'Unknown')}")
        print(f"   Device ID: {thermostat.get('deviceID')}")
        print(f"   Model: {thermostat.get('deviceModel')}")


if __name__ == "__main__":
    main()