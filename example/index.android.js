/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
	AppRegistry,---
	StyleSheet,
	Text,
	View,
	TouchableOpacity
} from 'react-native';

import Video from 'react-native-3deye-video';

export default class VideoPlayer extends Component {


  constructor(props) {
    super(props);
 	  this._onEnd = this._onEnd.bind(this); 
 	  this._onProgress = this._onProgress.bind(this);
    this._onError = this._onError.bind(this); 
    this.state = {
      url: 'http://playertest.longtailvideo.com/adaptive/bipbop/gear4/prog_index.m3u8'
    };
  }

  _onEnd(){
  	alert('end');
  }

  _onError(data){
  	alert('error');
  }

  _onProgress(data){
  	alert('progress: duration = '+ data.duration +" current time = " + data.currentTime);
  }

  render() {
    const { url } = this.state;
     return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!!!{this.state.text}
        </Text>
        <Text style={styles.instructions}>
          To get started, edit index.android.js
        </Text>
        <View>
          <Video
            style={{width: 320, height: 240}}
            source={url}
            onEnd={this._onEnd}
            onError={this._onError}
            onProgress={this._onProgress}
            /*rate={1.0}
            volume={1.0}
            muted={false}
            paused={true}*/
          />
        </View>
        <Text style={styles.instructions}>
          Double tap R on your keyboard to reload,{'\n'}
          Shake or press menu button for dev menu
        </Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: '#F5FCFF'
	},
	playerContainer: {
		flex: 1,
		flexDirection: 'column'
	},
	player: {
		justifyContent: 'center',
		alignItems: 'center',
		flex: 1
	}
});

AppRegistry.registerComponent('VideoPlayer', () => VideoPlayer);
