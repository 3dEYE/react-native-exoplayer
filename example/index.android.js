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
	View
} from 'react-native';
import { RNEP } from 'react-native-exoplayer';
import PlayerView from './js/PlayerView';

import data from './data.json';


export default class VideoPlayer extends Component {

	componentDidMount() {
		//RNEP.isRateSupported().then((result) => { alert("Change of speed is supported: " + result);});
		// RNEP.getMaxSupportedVideoPlayersCount("Calculation")
		// 	.then((result) => {alert(" max= "+result.maxSupported+" heap="+result.heapSize+" Mb")});
	}

	render() {
		const players = data.map((props, idx) => <PlayerView key={idx} {...props} />);

		return (
			<View style={styles.container}>
				{players}
			</View>
		);

	}
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		backgroundColor: '#F5FCFF',
		padding: 48
	}
});

AppRegistry.registerComponent('VideoPlayer', () => VideoPlayer);
