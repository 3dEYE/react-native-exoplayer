/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */
import React, { Component } from 'react';
import JWPlayerView from './components/JWPlayerView';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View
} from 'react-native';

export default class VideoPlayer extends Component {
	constructor(props) {
    super(props);

    this.state = {
        url: 'http://50.19.9.112/hls/c00007e00010p00121.m3u8'
    };
  }
  render() {
  	const {url} = this.state;
    return (
      <View style={styles.container}>
        <Text style={styles.titleText}>Заголовок</Text>
        <JWPlayerView style={styles.videoContainer} url={url}/>
        <Text style={styles.titleText}>Заголовок</Text>
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
  videoContainer: {
      flex: 2,
      flexDirection: 'row',
      height: 400,
      width: 320,
      backgroundColor: '#A5ACAA',
    },
    titleText:{
    fontSize: 20,
    fontWeight: 'bold',
    }
});

AppRegistry.registerComponent('VideoPlayer', () => VideoPlayer);
