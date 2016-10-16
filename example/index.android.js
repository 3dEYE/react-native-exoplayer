/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View,
  TouchableOpacity
} from 'react-native';

import Video from 'react-native-3deye-video';

export default class VideoPlayer extends Component {

  constructor(props) {
    super(props);

    this.state = {
      url: 'http://playertest.longtailvideo.com/adaptive/bipbop/gear4/prog_index.m3u8'
    };
  }

  render() {
    const { url } = this.state;

    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!!!
        </Text>
        <Text style={styles.instructions}>
          To get started, edit index.android.js
        </Text>
        <View>
          <Video
            style={{width: 320, height: 240}}
            file={url}
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
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});

AppRegistry.registerComponent('VideoPlayer', () => VideoPlayer);
