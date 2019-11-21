# Changelog

## 1.3.0 (unreleased)
- Updated the flow: instead of profiles you now select servers first. We don't connect to each server to fetch the latest profiles.
- Added some animations and updated loading indicators for a smoother user experience.
- You can remove servers by long pressing on them, instead of swiping.
- Update `ics-openvpn` submodule to v0.7.10 (#188). This adds support for TLSv1.3, Ed25519  keys, and includes OpenSSL 1.1.1d.
- Redirect URLs are now followed.
- Android 10 support, also updated most of our libraries to their latest version.
- Added license screen.
- Fixed some edge-cases where IPv4 / IPv6 / Duration would not be displayed when connected, and the back button would not appear.
- When connecting, the notifications tab is only shown when there are any notifications.
- Fixed a crash which happened on screen rotation.
- Removed mentions of 2FA.
- Deprecated the secure-preferences library, added migration.
- Updated documentations and build scripts.

## 1.2.3 (2018-06-28)
- fix Gradle build (#165)
- immediately show "add provider" page when starting the app and no
  providers were added before for Let's Connect! flavor
- show "Profiles" instead of "Institute Access" for Let's Connect! flavor
- use different "certificate name" through API for eduVPN / Let's Connect! 
  flavor

## 1.2.2 (2018-05-14)
- use different OAuth client information for Let's Connect! flavor

## 1.2.1 (2018-05-04)
- update `ics-openvpn` submodule to v0.7.5 (#153)

## 1.2.0 (2018-05-03)
- update `ics-openvpn` submodule to v0.7.4 (#133)
- update `client_id` (#127)
- fetch update server VPN configuration on connect (#123)
- fetch new client certificate if the old one expired (#130)
- fix connecting to 2FA enabled profile (#106)
- trigger new authorization on profiles page when OAuth client was revoked 
  (#141)
- many text changes in the application
- initial Let's Connect! branding changes, still WiP (#147)
- fix use of "refresh token" (#149)
- update VPN connection status icon to new artwork (#148)

## 1.1.1 (2017-11-23)
- N/A

## 1.0.1 (2017-02-01)
- N/A

## 1.0.0 (2017-02-01)
- initial release
