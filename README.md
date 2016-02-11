# Introduction

Simple App to import OpenVPN configurations in OpenVPN Connect or OpenVPN for Android.

# Requires

- [Barcode Scanner](https://play.google.com/store/apps/details?id=com.google.zxing.client.android)
- [OpenVPN Connect](https://play.google.com/store/apps/details?id=net.openvpn.openvpn) 
  or [OpenVPN for Android](https://play.google.com/store/apps/details?id=de.blinkt.openvpn)
  
# TODO

- the app downloads an OVPN file to external storage and then launches OpenVPN, can 
  this be also done differently? I don't want to leave OVPNs all around the external
  storage, and I want to not require this permission
- do not use Username/Password but use single token
- maybe only support QR code and not manual entering data, or at least not on main screen
- cleanup a lot of code
  - error handling!
- do not overwrite existing config in OpenVPN connect
- higher resolution icon
- branding
- add to play store | F-Droid
- currently the OVPN file needs to remain stored on the SD card, that is 
  really not good!
