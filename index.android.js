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
  NativeModules
} from 'react-native';

var Main = React.createClass({ 

  getInitialState: function () {
    return {
      count: 0,
      error: ""
    };
  },

  render: function() {
	  {
      if (this.state.count == 0){
        NativeModules.RNVideoManager.getMaxSupportedVideoPlayersCount("Calculation...")
        .then((result) => {this.setState({count: result})})
        .catch((err) => {
          
        });
      }
    }
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>Max = {this.state.count}</Text>
      </View>
    );
  }
});

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

AppRegistry.registerComponent('MultiVideoLib', () => Main);
