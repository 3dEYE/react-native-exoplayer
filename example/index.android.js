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
	}

	render() {
		return (
			<View style={styles.container}>
				<View style={styles.playerContainer}>
					<View style={styles.player}>
						<Video
							style={{width: 320, height: 240}}
							file={'http://ios-streaming.cnet.com/mpx/hls/2016/08/19/747447363521/Baby_Monitor_Capsule_892578_800/Baby_Monitor_Capsule_892578_800.m3u8'}
						/>
					</View>
					<View style={styles.control}></View>
				</View>
				<View style={styles.playerContainer}>
					<View style={styles.player}>
						<Video
							style={{width: 320, height: 240}}
							file={'http://playertest.longtailvideo.com/adaptive/bipbop/gear4/prog_index.m3u8'}
						/>
					</View>
					<View style={styles.control}></View>
				</View>
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
