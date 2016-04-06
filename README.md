# Introduction

Simple App to import OpenVPN configurations in OpenVPN Connect or OpenVPN for Android.

# Requires

If this application is missing, the app will request the user to install it.

- [OpenVPN Connect](https://play.google.com/store/apps/details?id=net.openvpn.openvpn) 
  or [OpenVPN for Android](https://play.google.com/store/apps/details?id=de.blinkt.openvpn)
  
# TODO

- the app downloads an OVPN file to external storage and then launches OpenVPN, can 
  this be also done differently? I don't want to leave OVPNs all around the external
  storage, and I want to not require this permission in the first place...
- maybe embedded OpenVPN for Android so we can keep the configuration in the 
  private data storage of the application?
- use PKCS#10? Seems very cumbersome to use on Android
- cleanup a lot of code
  - error handling!
- higher resolution icon
- branding
- add to play store | F-Droid
