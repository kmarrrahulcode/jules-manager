# Jules Manager

A Python-based Streamlit application and a native Android application for managing Jules sessions.

## 🐍 Python Web App

### Setup
1.  Install dependencies:
    ```bash
    pip install -r requirements.txt
    ```
2.  Set your API Key:
    ```bash
    export JULES_API_KEY="your_api_key_here"
    ```

### Running
Run the Streamlit app:
```bash
streamlit run app.py
```

## 📱 Android App

The repository includes a native Android application located in `android_app/`.

### Features
*   View active sessions and status.
*   Chat with the Jules agent.
*   "Publish Feature Branch" action directly from the chat.
*   Secure API Key storage (local device).

### Building the APK (GitHub Actions)
Since the Android SDK might not be available in your local environment, this repository includes a **GitHub Actions workflow** to build the APK automatically.

1.  Push your changes to GitHub.
2.  Go to the **Actions** tab in your repository.
3.  Click on the **Android Build** workflow.
4.  Once the build finishes, click on the run to view details.
5.  Scroll down to the **Artifacts** section and download `app-debug`. This zip file contains the `app-debug.apk` which you can install on your Android device.

### Local Development (If Android Studio is available)
1.  Open the `android_app` folder in Android Studio.
2.  Sync Gradle project.
3.  Run on an Emulator or Physical Device.
