import { PropTypes } from 'react';
import { requireNativeComponent, View } from 'react-native';

var iface = {
  name: 'JWPlayer',
  propTypes: {
    file: PropTypes.string,
    // borderRadius: PropTypes.number,
    // resizeMode: PropTypes.oneOf(['cover', 'contain', 'stretch']),
    ...View.propTypes // include the default view properties
  },
};

module.exports = requireNativeComponent('RCTJWPlayer', iface);
