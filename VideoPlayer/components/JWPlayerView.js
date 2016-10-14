import { PropTypes } from 'react';
import { requireNativeComponent, View } from 'react-native';

var iface = {
  name: 'JWPlayerView',
   propTypes: {
    url: PropTypes.string,
    ...View.propTypes
  },
};

module.exports = requireNativeComponent('JWPlayerComponent', iface);