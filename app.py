import streamlit as st
import os
from jules_client import JulesClient
import time

st.set_page_config(page_title="Jules Session Manager", layout="wide", page_icon="🤖")

# --- Helper Functions ---

def get_client():
    if "api_key" not in st.session_state:
        st.session_state["api_key"] = os.environ.get("JULES_API_KEY", "")

    api_key = st.sidebar.text_input("Jules API Key", value=st.session_state["api_key"], type="password")

    if api_key:
        st.session_state["api_key"] = api_key
        try:
            return JulesClient(api_key)
        except Exception as e:
            st.sidebar.error(f"Error initializing client: {e}")
            return None
    else:
        st.sidebar.warning("Please enter your Jules API Key.")
        return None

def format_status(state):
    """Maps the API state enum to a user-friendly string."""
    mapping = {
        "STATE_UNSPECIFIED": "Unknown",
        "QUEUED": "Queued",
        "PLANNING": "Planning",
        "AWAITING_PLAN_APPROVAL": "Waiting for Approval",
        "AWAITING_USER_FEEDBACK": "Waiting for Feedback",
        "IN_PROGRESS": "In Progress",
        "PAUSED": "Paused",
        "FAILED": "Failed",
        "COMPLETED": "Completed"
    }
    return mapping.get(state, state)

def render_activity(activity):
    """Renders a single activity item."""
    create_time = activity.get("createTime", "")

    if "userMessaged" in activity:
        msg = activity["userMessaged"]["userMessage"]
        with st.chat_message("user"):
            st.write(msg)
            st.caption(f"{create_time}")

    elif "agentMessaged" in activity:
        msg = activity["agentMessaged"]["agentMessage"]
        with st.chat_message("assistant"):
            st.write(msg)
            st.caption(f"{create_time}")

    elif "planGenerated" in activity:
        plan = activity["planGenerated"].get("plan", {})
        steps = plan.get("steps", [])
        with st.chat_message("assistant"):
            with st.expander("📋 Plan Generated", expanded=False):
                for step in steps:
                    st.markdown(f"**{step.get('index', 0) + 1}. {step.get('title', 'Untitled')}**")
                    st.write(step.get("description", ""))
            st.caption(f"{create_time}")

    elif "progressUpdated" in activity:
        progress = activity["progressUpdated"]
        st.info(f"**Progress:** {progress.get('title', '')} - {progress.get('description', '')}")

    elif "sessionCompleted" in activity:
        st.success("✅ Session Completed")

    elif "sessionFailed" in activity:
        reason = activity["sessionFailed"].get("reason", "Unknown error")
        st.error(f"❌ Session Failed: {reason}")

    elif "planApproved" in activity:
         st.success(f"👍 Plan Approved")

    else:
        # Fallback for other types
        with st.expander(f"Activity: {activity.get('name', 'Unknown')}"):
            st.json(activity)


# --- Main App Logic ---

def main():
    st.title("🤖 Jules Session Manager")

    client = get_client()
    if not client:
        return

    tab1, tab2 = st.tabs(["Sessions", "Sources"])

    # --- Sessions Tab ---
    with tab1:
        st.header("Sessions")
        try:
            sessions = client.list_sessions()
        except Exception as e:
            st.error(f"Failed to list sessions: {e}")
            sessions = []

        if not sessions:
            st.info("No active sessions found.")
        else:
            # Create a selection box for sessions
            # Sort by createTime descending if possible, or just list
            session_options = {s.get("name"): s for s in sessions}
            # Use a selectbox in the main area or sidebar. Let's use sidebar for navigation if we want,
            # but since we have tabs, let's put it in the main area or a column.

            selected_session_name = st.selectbox(
                "Select a Session",
                options=list(session_options.keys()),
                format_func=lambda x: f"{session_options[x].get('title', 'Untitled')} ({format_status(session_options[x].get('state'))})"
            )

            if selected_session_name:
                session_data = session_options[selected_session_name]
                display_session_details(client, session_data)

    # --- Sources Tab ---
    with tab2:
        st.header("Sources")
        try:
            sources = client.list_sources()
        except Exception as e:
            st.error(f"Failed to list sources: {e}")
            sources = []

        if not sources:
            st.info("No sources found.")
        else:
            st.subheader("Available Sources")
            for src in sources:
                with st.expander(f"📦 {src.get('name')}"):
                    if "githubRepo" in src:
                        repo = src["githubRepo"]
                        st.markdown(f"**GitHub Repo:** [{repo.get('owner')}/{repo.get('repo')}](https://github.com/{repo.get('owner')}/{repo.get('repo')})")
                        st.write(f"**Branch:** {repo.get('defaultBranch', {}).get('displayName', 'default')}")

            st.divider()
            st.subheader("Start New Session")

            # Form to create session
            with st.form("create_session_form"):
                source_options = {s.get("name"): s for s in sources}
                selected_source = st.selectbox("Choose Source", list(source_options.keys()))
                prompt = st.text_area("Prompt", placeholder="e.g., Fix the bug in utils.py...")
                submitted = st.form_submit_button("Start Session")

                if submitted and prompt and selected_source:
                    try:
                        new_session = client.create_session(selected_source, prompt)
                        st.success(f"Session created! ID: {new_session.get('name')}")
                        time.sleep(1)
                        st.rerun()
                    except Exception as e:
                        st.error(f"Failed to create session: {e}")


def display_session_details(client, session_data):
    session_name = session_data["name"]
    st.divider()

    # Header Info
    col1, col2 = st.columns([3, 1])
    with col1:
        st.subheader(session_data.get("title", "Untitled Session"))
        st.caption(f"ID: {session_name}")
    with col2:
        status = format_status(session_data.get("state"))
        st.metric("Status", status)

    # Refresh
    if st.button("🔄 Refresh Session"):
        try:
            session_data = client.get_session(session_name)
            st.rerun()
        except Exception as e:
            st.error(f"Error refreshing: {e}")

    # Actions Bar
    st.subheader("Actions")
    act_col1, act_col2, act_col3 = st.columns(3)

    with act_col1:
        if st.button("Approve Plan", disabled=session_data.get("state") != "AWAITING_PLAN_APPROVAL"):
            try:
                client.approve_plan(session_name)
                st.success("Plan Approved!")
                time.sleep(1)
                st.rerun()
            except Exception as e:
                st.error(f"Failed: {e}")

    with act_col2:
        if st.button("Request PR Publication"):
             try:
                client.send_message(session_name, "Please publish the PR.")
                st.success("Requested PR publication.")
             except Exception as e:
                st.error(f"Failed: {e}")

    with act_col3:
        if st.button("Accept Review"):
            try:
                client.send_message(session_name, "I accept the review. Please proceed.")
                st.success("Accepted review.")
            except Exception as e:
                st.error(f"Failed: {e}")

    # Chat / Activity Stream
    st.divider()
    st.subheader("Activity Log")

    # Message Input
    if st.session_state.get("last_session") != session_name:
        st.session_state["chat_input"] = ""
        st.session_state["last_session"] = session_name

    chat_input = st.chat_input("Message Jules...")
    if chat_input:
        try:
            client.send_message(session_name, chat_input)
            st.success("Message sent")
            time.sleep(0.5)
            st.rerun()
        except Exception as e:
            st.error(f"Failed to send: {e}")

    # Load and Render Activities
    try:
        activities = client.list_activities(session_name)
        # Reverse order? Usually APIs return newest first or oldest first.
        # Chat interface usually expects oldest at top.
        # Assuming list_activities returns chronological order (oldest first) or reverse.
        # Often APIs return newest first. If so, we might want to reverse it for chat view.
        # Let's assume default order is chronological for now, or check timestamps if needed.
        # If it looks backwards, we can flip it.

        # Rendering activities
        for activity in activities:
            render_activity(activity)

    except Exception as e:
        st.warning(f"Could not load activities: {e}")

if __name__ == "__main__":
    main()
