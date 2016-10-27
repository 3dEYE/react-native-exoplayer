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

import { RNEP, Video } from 'react-native-3deye-video';

export default class VideoPlayer extends Component {


	constructor(props) {
		super(props);
		this._onEnd = this._onEnd.bind(this);
		this._onProgress = this._onProgress.bind(this);
		this._onError = this._onError.bind(this);
		this.state = {
			url: 'http://playertest.longtailvideo.com/adaptive/bipbop/gear4/prog_index.m3u8',
			volume:0.1,
			muted:false,
			paused:false,
			speed:1.0
		};
	}

	_onEnd(){
		alert('end');

	}

	_onError(data){
		alert('error '+data.error);
	}

	_onProgress(data){
		//alert('progress: duration = '+ data.duration +" current time = " + data.currentTime);
	}

	render() {
		const { url } = this.state;
		RNEP.isRateSupported().then((result) => { alert("Change of speed is supported: " + result);});
		return (
			<View style={styles.container}>
				<View style={styles.playerContainer}>
					<View style={styles.player}>
						<Video
							style={{width: 320, height: 180}}
							source={this.state.url}
							onEnd={this._onEnd}
							onError={this._onError}
							volume={this.state.volume}
							muted={this.state.muted}
							paused={this.state.paused}	
							rate={this.state.speed}						
							onProgress={this._onProgress}
							/>
					</View>				
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
