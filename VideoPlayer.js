import React, { PropTypes, Component } from 'react';
import { requireNativeComponent, View, NativeModules} from 'react-native';

export class Video extends Component {

	setNativeProps(nativeProps) {
		this._root.setNativeProps(nativeProps);
	}

	_onError = (event:Event) => {
		if (this.props.onError) {
			this.props.onError(event.nativeEvent);
		}
	}

	_assignRoot = (component) => {
		this._root = component;
	}

	_onProgress = (event:Event) => {
		if (this.props.onProgress) {
			this.props.onProgress(event.nativeEvent);
		}
	}

	_onEnd = (event:Event) => {
		if (this.props.onEnd) {
			this.props.onEnd(event.nativeEvent);
		}
	}

	_onWarning = (event : Event) => {
		console.warn(event.nativeEvent.warningMessage);
	}

	render() {
		const nativeProps = Object.assign({}, this.props);

		Object.assign(nativeProps, {
			onError: this._onError,
			onProgress: this._onProgress,
			onEnd: this._onEnd,
			onWarning: this._onWarning
		});

		return (
			<RNExoPlayer
				ref={this._assignRoot}
				{...nativeProps}
			/>
		);
	}
}

Video.propTypes = {
	source: PropTypes.string,
	rate: PropTypes.number,
	volume: PropTypes.number,
	paused: PropTypes.bool,
	muted: PropTypes.bool,
	onProgress: PropTypes.func,
	onError: PropTypes.func,
	onEnd: PropTypes.func,
	...View.propTypes // include the default view properties
};

const RNExoPlayer = requireNativeComponent(`RNExoPlayer`, Video);

const RNEPManager = NativeModules.RNEPManager;

export var RNEP = {
	isRateSupported():Promise<boolean> {
		return RNEPManager.isRateSupported();
	},

	getMaxSupportedVideoPlayersCount(message:string):Promise<Object> {
		return RNEPManager.getMaxSupportedVideoPlayersCount(message);
	}
};
