# Changelog

## 3.3.4 (...)
- update WireGuard, OpenVPN and "common" sub-modules
- Move backend methods to background threads
- More crash fixes
- small README updates

## 3.3.3 (2024-10-04)
- embed WireGuard for Android
- fix crash when reconnecting

## 3.3.2 (2024-08-30)
- bump release for Google Play Store

## 3.3.1 (2024-08-28)
- Custom tabs will be always on, remove the option 
  ([#372](https://github.com/eduvpn/android/issues/372), 
  [#144](https://github.com/eduvpn/android/issues/144))
- Move prefer TCP from a setting to a button
- Sort country selector by country name ([#381](https://github.com/eduvpn/android/issues/381))
- Display WireGuard (TCP) if connected with ProxyGuard
- Fix connection state dialog staying on screen
- Fix missing server name
- Error handling for no connection case

## 3.3.0 (2024-08-12)
- Update eduvpn-common to release 2.0.2
- Implement support for ProxyGuard (WireGuard over TCP)
- Fix "Country Switcher"
- Improve error dialogs
- Do not crash on invalid WireGuard configuration files
- Update ics-openvpn (v0.7.5.1)

## 3.2.2 (2024-02-05)
- fix incorrect usage of seconds, should be ms 
  ([GH#411](https://github.com/eduvpn/android/issues/411))
- fix not being able to add secure internet servers ([GH#412](https://github.com/eduvpn/android/issues/412))

## 3.2.1 (2024-01-19)
- update all build scripts for new release
- create new tag to properly update CHANGES.md and fastlane files

## 3.2.0 (2023-12-22)
- Use common Go library for interfacing with VPN servers
- Add govVPN flavor
- Update dependencies
- Added Catalan translation
- Various bug fixes

## 3.1.1 (2023-05-24)
- Only sort when retrieving lists, not on every keypress 
  ([#394](https://github.com/eduvpn/android/pull/394))

## 3.1.0 (2023-05-22)
- Remove API v2 support ([#382](https://github.com/eduvpn/android/pull/383))
- German translation ([#386](https://github.com/eduvpn/android/pull/386))
- Support Minisign prehashed signature format ([#361](https://github.com/eduvpn/android/pull/361))
- Fix no keyword search for institutes ([#384](https://github.com/eduvpn/android/pull/384))
- Fix sorting Institute Access and Secure Internet ([#387](https://github.com/eduvpn/android/pull/387))
- Remove save button ([#383](https://github.com/eduvpn/android/pull/383))
- Fix "null" in notification when using secure internet ([#390](https://github.com/eduvpn/android/pull/390))
- Update OkHttp to 4.10.0 ([#390](https://github.com/eduvpn/android/pull/391))

## 3.0.1 (2022-11-04)
- update ics-openvpn
- Remove secure-preferences ([#117](https://github.com/eduvpn/android/issues/117))

## 3.0.0 (2022-05-09)
- add support for [APIv3](https://github.com/eduvpn/documentation/blob/v3/API.md)
- add support for [WireGuard](https://www.wireguard.com/) in combination with 
  eduVPN/Let's Connect! 3.x servers
- improve expiry notification
- allow user to request a new session
- update ics-openvpn

## 2.0.5 (2021-06-11)
- fix 'certificate expired' popup loop (issue #329)
- add fastlane for F-Droid/Google Play release automation
- update discovery signing keys

## 2.0.4 (2021-04-13)
- add Spanish (Latin America) translation
- fix parallel installation issue with other ics-openvpn based apps (#325)

## 2.0.3 (2021-03-18)
- update ics-openvpn

## 2.0.2 (2020-11-06)
- fix certificate expiry countdown (#311)
- Fix UI tests

## 2.0.1 (2020-09-22)
- update ics-openvpn
- reduce size of app (do not add all static libs)
- add additional public keys for eduVPN discovery
- add application tests

## 2.0.0 (2020-08-26)
- Redesigned the entire app
- Search for your organization or add your own server
- Update ics-openvpn to version v0.7.17a
- Added support for certificate expiry notifications
- Your added servers are now grouped by type

## 1.3.2 (2020-02-18)
- Continue to add server screen if selection screen is empty (#232)
- Fix app being stuck in creating keypair (#233)

## 1.3.1 (2020-02-11)
- Update ics-openvpn to version v0.7.13

## 1.3.0 (2019-11-26)
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
