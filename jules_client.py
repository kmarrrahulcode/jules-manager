import os
import requests
import json

class JulesClient:
    BASE_URL = "https://jules.googleapis.com"

    def __init__(self, api_key=None):
        self.api_key = api_key or os.environ.get("JULES_API_KEY")
        if not self.api_key:
            raise ValueError("Jules API Key is required. Set JULES_API_KEY environment variable or pass it to the constructor.")

        self.headers = {
            "X-Goog-Api-Key": self.api_key,
            "Content-Type": "application/json"
        }

    def _get_url(self, path):
        return f"{self.BASE_URL}/{path.lstrip('/')}"

    def list_sessions(self):
        """Lists all sessions."""
        url = self._get_url("v1alpha/sessions")
        response = requests.get(url, headers=self.headers)
        response.raise_for_status()
        return response.json().get("sessions", [])

    def get_session(self, session_name):
        """Gets a single session by name (e.g., sessions/...)."""
        url = self._get_url(f"v1alpha/{session_name}")
        response = requests.get(url, headers=self.headers)
        response.raise_for_status()
        return response.json()

    def list_sources(self):
        """Lists all sources."""
        url = self._get_url("v1alpha/sources")
        response = requests.get(url, headers=self.headers)
        response.raise_for_status()
        return response.json().get("sources", [])

    def create_session(self, source_name, prompt):
        """Creates a new session."""
        url = self._get_url("v1alpha/sessions")
        payload = {
            "prompt": prompt,
            "sourceContext": {
                "source": source_name
            }
        }
        response = requests.post(url, headers=self.headers, json=payload)
        response.raise_for_status()
        return response.json()

    def list_activities(self, session_name):
        """Lists activities for a session."""
        url = self._get_url(f"v1alpha/{session_name}/activities")
        response = requests.get(url, headers=self.headers)
        response.raise_for_status()
        return response.json().get("activities", [])

    def approve_plan(self, session_name):
        """Approves the current plan in the session."""
        url = self._get_url(f"v1alpha/{session_name}:approvePlan")
        # Empty body as per assumption, or generic approval payload
        response = requests.post(url, headers=self.headers, json={})
        response.raise_for_status()
        return response.json()

    def send_message(self, session_name, message):
        """Sends a message to the session."""
        url = self._get_url(f"v1alpha/{session_name}:sendMessage")
        payload = {"message": message}
        response = requests.post(url, headers=self.headers, json=payload)
        response.raise_for_status()
        return response.json()
