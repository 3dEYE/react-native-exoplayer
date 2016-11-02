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
			volume: 0.1,
			muted: false,
			paused: false,
			speed: 1.0
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
	}

	render() {
		const { url, width, height } = this.props;
		const { duration, currentTime, removed } = this.state;

		return (
			<View style={styles.playerContainer}>
				<View style={styles.player}>
					{removed ? null : (
						<Video
						style={{width, height}}
						source={url}
						onEnd={this._onEnd}
						onError={this._onError}
						volume={0.5}
						seekTo={50000}
						controls={false}
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
