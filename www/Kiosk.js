var exec = require('cordova/exec');

exports.lockLauncher = function(enabled, success, error) {
    exec(success, error, 'Kiosk', 'lockLauncher', [!!enabled]);
};

exports.isLocked = function(success, error) {
    exec(success, error, 'Kiosk', 'isLocked', null);
};

exports.isKeepScreenOn = function(success, error) {
    exec(success, error, 'Kiosk', 'isKeepScreenOn', null);
};

exports.switchLauncher = function(success, error) {
    exec(success, error, 'Kiosk', 'switchLauncher', null);
};

exports.deleteDeviceAdmin = function(success, error) {
    exec(success, error, 'Kiosk', 'deleteDeviceAdmin', null);
};
