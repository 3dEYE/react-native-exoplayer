# react-native-3deye-video
Video component for react-native

## Installation
TODO

## Usage

```javascript
import Video from 'react-native-3deye-video';
```
Later within your render function

```javascript
<Video source={streamUrl}   // Can be a URL or a local file.
       rate={1.0}
       volume={1.0}
       muted={false}
       paused={false}
       onProgress={onProgress}
       onEnd={onEnd}
       onError={onError}
/>
```
