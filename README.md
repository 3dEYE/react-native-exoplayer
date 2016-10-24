# react-native-3deye-video
Video component for React Native

## Installation
```bash
npm install --save react-native-3deye-video
```

Add your JW License Key within <application> section of your application AndroidManifest.xml:

```xml
<meta-data
    android:name="JW_LICENSE_KEY"
    android:value="{YOUR_LICENCE_KEY}" />
```

## Usage

```javascript
import Video from 'react-native-3deye-video';
```
Later within your render function

```javascript
<Video source={streamUrl}
       rate={1.0}
       volume={1.0}
       muted={false}
       paused={false}
       onProgress={onProgress}
       onEnd={onEnd}
       onError={onError}
/>
```

## Example

Try included example:

Clone repository and install dependencies:
```bash
git clone https://github.com/3dEYE/react-native-3deye-video.git
cd react-native-3deye-video/example
npm install
```

Replace '{YOUR_LICENCE_KEY}' with your actual JWPlayer license key at `example/android/app/src/main/AndroidManifest.xml`

Run example:
```bash
react-native run-android
```
