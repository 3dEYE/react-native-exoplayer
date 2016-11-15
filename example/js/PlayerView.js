import React, { PropTypes } from 'react';

import {
	AppRegistry,
	StyleSheet,
	Text,
	View,
	TouchableHighlight
} from 'react-native';

import { Video } from 'react-native-3deye-video';

class PlayerView extends React.Component {

	constructor(props) {
		super(props);

		this.state = {
			url: 'http://playertest.longtailvideo.com/adaptive/bipbop/gear4/prog_index.m3u8',
			volume: 1.0,
			muted: false,
			paused: false,
			speed: 4.0
		};
	}

	_onPressPauseButton = () => {
		const paused = !this.state.paused;
		this.setState({ paused });
	}

	_onPressRemoveButton = () => {
		const removed = !this.state.removed;
		this.setState({ removed });
	}

	_onEnd() {
		console.warn('end');
	}

	_onError(data) {
		console.error('error ' + data.error);
	}

	_onProgress = (data) => {
		this.setState({...data});
		if (this.state.volume > 0.0){
			this.setState({volume:this.state.volume-0.05});
		}
	}

	_onSeek = (data) => {
		console.warn('seekTime = ' + data.seekTime);
	}

	render() {
		const { url, width, height } = this.props;
		const { duration, currentTime, removed } = this.state;

		return (
			<View style={styles.playerContainer}>
				<View style={styles.player}>
					{removed ? null : (
						<Video
						ref={(ref) => {
         					this.player = ref
      					}}    
						style={{width, height}}
						source={url}
						onEnd={this._onEnd}
						onError={this._onError}
						volume={this.state.volume}
						onSeek={this._onSeek}
						controls={true}
						muted={this.state.muted}
						paused={this.state.paused}
						rate={this.state.speed}
						onProgress={this._onProgress}
						/>
					)}
				</View>
				<View style={styles.controls}>
					<View><Text>{currentTime}/{duration}s</Text></View>

					<TouchableHighlight onPress={this._onPressPauseButton}>
						<View style={styles.buttonContainer}>
							<Text style={styles.buttonLabel}>{this.state.paused ? 'Play' : 'Pause'}</Text>
						</View>
					</TouchableHighlight>

					<TouchableHighlight onPress={this._onPressRemoveButton}>
						<View style={styles.buttonContainer}>
							<Text style={styles.buttonLabel}>{this.state.removed ? 'Get back' : 'Remove'}</Text>
						</View>
					</TouchableHighlight>
				</View>
			</View>
		);
	}
}

const styles = StyleSheet.create({
	playerContainer: {
		flex: 1,
		flexDirection: 'column'
	},
	player: {
		justifyContent: 'center',
		alignItems: 'center'
	},
	controls: {
		alignItems: 'center',
		justifyContent: 'center',
		flexDirection: 'row'
	},
	buttonContainer: {
		margin: 24
	},
	buttonLabel: {
		fontSize: 24
	}
});

export default PlayerView;
