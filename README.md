# react-native-exoplayer
Video component for React Native

## Installation
```bash
npm install --save react-native-exoplayer
```

## Usage

```javascript
import Video from 'react-native-exoplayer';
```
Later within your render function

```javascript
<Video source={streamUrl}
       rate={1.0}
       volume={1.0}
       muted={false}
       paused={false}
       controls={false}
       onProgress={onProgress}
       onEnd={onEnd}
       onError={onError}
/>
```

## Example

Try included example:

Clone repository and install dependencies:
```bash
git clone https://github.com/3dEYE/react-native-exoplayer.git
cd react-native-exoplayer/example
npm install
```

Run example:
```bash
react-native run-android
```
