import streamlit as st
import os
from jules_client import JulesClient
import time

st.set_page_config(page_title="Jules Session Manager", layout="wide")

def get_client():
    api_key = st.sidebar.text_input("Jules API Key", value=os.environ.get("JULES_API_KEY", ""), type="password")
    if not api_key:
        st.sidebar.warning("Please enter your Jules API Key.")
        return None
    try:
        return JulesClient(api_key)
    except Exception as e:
        st.sidebar.error(f"Error initializing client: {e}")
        return None

def main():
    st.title("Jules Session Manager")

    client = get_client()
    if not client:
        return

    # Sidebar: List Sessions
    st.sidebar.header("Sessions")
    try:
        sessions = client.list_sessions()
    except Exception as e:
        st.error(f"Failed to list sessions: {e}")
        return

    if not sessions:
        st.sidebar.info("No sessions found.")
        return

    # Create a mapping of session display name to full name
    session_options = {s.get("name", "Unknown"): s for s in sessions}
    selected_session_name = st.sidebar.selectbox("Select Session", list(session_options.keys()))

    if selected_session_name:
        session_data = session_options[selected_session_name]
        display_session(client, session_data)

def display_session(client, session_data):
    session_name = session_data["name"]
    st.header(f"Session: {session_name}")

    # Refresh Button
    if st.button("Refresh Status"):
        try:
            session_data = client.get_session(session_name)
            st.success("Refreshed!")
        except Exception as e:
            st.error(f"Error refreshing session: {e}")

    # Display Status
    st.subheader("Status")
    st.json(session_data) # Display raw JSON for now as structure is variable

    # Actions
    st.subheader("Actions")
    col1, col2, col3 = st.columns(3)

    with col1:
        if st.button("Approve Plan"):
            try:
                client.approve_plan(session_name)
                st.success("Plan Approved!")
            except Exception as e:
                st.error(f"Failed to approve plan: {e}")

    with col2:
        if st.button("Publish PR"):
            try:
                client.send_message(session_name, "Please publish the PR.")
                st.success("Request to publish PR sent.")
            except Exception as e:
                st.error(f"Failed to send publish request: {e}")

    with col3:
        if st.button("Accept Review"):
            try:
                client.send_message(session_name, "I accept the review. Please proceed.")
                st.success("Accepted review.")
            except Exception as e:
                st.error(f"Failed to accept review: {e}")

    # Chat / Messages
    st.subheader("Chat")

    # Input for new message
    new_message = st.text_input("Send a message to Jules")
    if st.button("Send"):
        if new_message:
            try:
                client.send_message(session_name, new_message)
                st.success("Message sent!")
                # Optional: clear input or refresh activities
            except Exception as e:
                st.error(f"Failed to send message: {e}")

    # Display Activities (History)
    st.subheader("Activity Log")
    try:
        activities = client.list_activities(session_name)
        for activity in activities:
            with st.expander(f"{activity.get('type', 'Activity')} - {activity.get('createTime', '')}"):
                st.json(activity)
    except Exception as e:
        st.warning(f"Could not load activities: {e}")

if __name__ == "__main__":
    main()
