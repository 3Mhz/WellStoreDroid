# WellStoreDroid

WellStoreDroid is an Android application designed to collect per-app usage statistics and send them to a user-configured remote server. It operates efficiently in the background, respects Android's power-saving features, and provides a simple UI for configuration and monitoring.

## Features

- **Background Data Collection**: Periodically collects app usage data using Android's `UsageStatsManager`.
- **Local Persistence**: Stores collected data in a local Room database before uploading.
- **Automatic Uploads**: Uploads data in batches to a user-defined HTTP/S endpoint.
- **Resilient Networking**: Uses WorkManager with constraints to handle uploads, with automatic retries and exponential backoff on failure.
- **User Configuration**: Allows users to set the destination server URL and enable or disable data collection.
- **Manual Testing**: Includes a button to test the connection to the configured endpoint.
- **Status Monitoring**: The main screen displays key information like the last collection time, last successful upload, pending item queue size, and any recent errors.

## How It Works

1.  **Permission**: On first launch, the app guides the user to grant the "Usage Access" permission, which is required to read app usage statistics.
2.  **Collection**: A periodic `WorkManager` task runs approximately every 15 minutes. It calculates the foreground time for each app since the last collection and saves this data as a `Sample` in the local Room database.
3.  **Upload**: After each collection, a one-time upload task is enqueued. Additionally, a periodic upload task runs every hour to ensure pending data is sent. The uploader batches pending samples into a single JSON payload and POSTs it to the configured endpoint. On a successful upload, samples are marked as `SENT`. If the upload fails, the app will retry later.

## Technical Details

- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: Jetpack Compose
- **Background Processing**: WorkManager
- **Database**: Room
- **Settings**: Jetpack DataStore
- **Networking**: Retrofit & OkHttp
- **JSON Parsing**: Kotlinx Serialization

## Setup and Running

1.  **Clone the repository.**
2.  **Open the project in Android Studio.**
3.  **Build the project.** Gradle sync will download the required dependencies.
4.  **Run the app on a device or emulator (API 26+).**
5.  **Grant Usage Access**: On the main screen, click the button to open system settings and grant the required permission.
6.  **Configure Endpoint**: Enter a valid HTTP or HTTPS URL where the app should send the data.
7.  **Enable Collection**: Use the toggle switch to start the background collection process.

### Note on Periodic Work

WorkManager schedules periodic tasks to run in a battery-efficient manner. The operating system may delay the execution of these tasks (e.g., during Doze mode). While the collection interval is set to 15 minutes, the actual time between runs may vary. To ensure data accuracy, the app records the precise start and end time of each collection interval (`intervalStart`, `intervalEnd`) in every sample.
