# Structure-from-Motion (SFM) on Android

This project implements a **Structure-from-Motion (SFM)** pipeline on Android devices. The goal of the application is to perform 3D scene reconstruction using images captured on a mobile device, leveraging computer vision techniques to process and reconstruct the 3D models in real-time.

## Features
- **3D Reconstruction:** Using a series of 2D images, the app generates a 3D model of the scene.
- **Real-Time Processing:** Implemented algorithms allow the real-time extraction of feature points and camera pose estimation on mobile devices.
- **Efficient Feature Matching:** Implements feature extraction and matching using state-of-the-art algorithms to ensure robustness in challenging environments.
- **Camera Calibration:** Includes methods for camera calibration to improve the accuracy of pose estimation.

## Installation

To run this project, you'll need to set up Android Studio and install the necessary dependencies.

### Prerequisites
- **Android Studio** (latest version)
- **Android SDK**
- **Java Development Kit (JDK)** version 8 or higher

### Steps
1. **Clone the repository:**
   ```bash
   git clone https://github.com/iamkrunalrk/3D_Reconstruction_Android.git
   ```

2. **Open the project in Android Studio:**
   - Launch Android Studio.
   - Choose `Open an existing project`.
   - Navigate to the cloned directory and select the project.

3. **Sync Gradle:**
   Once the project is opened, Android Studio will prompt you to sync the Gradle files. Click `Sync Now`.

4. **Set up Android Device/Emulator:**
   - Ensure you have a physical Android device connected or set up an Android Emulator.
   - Make sure USB debugging is enabled on the device if using a physical device.

5. **Build and Run the Application:**
   - Click on the green `Run` button in Android Studio to build and run the app on your device.

## Usage

After the app is launched on your Android device, follow these steps:

1. **Capture Images:**
   - Take multiple images of the scene you want to reconstruct. Ensure that there is sufficient overlap between the images for the algorithm to detect common feature points.
   
2. **Process the Images:**
   - The app will automatically process the images to extract features and perform the matching.

3. **View the 3D Reconstruction:**
   - Once processing is complete, the app will display a 3D reconstruction of the scene based on the captured images.

### User Interface
- The app includes a simple interface with options to capture images, view the results, and control settings like image resolution and feature matching parameters.