#!/bin/sh

# dnf -y install java-1.8.0-openjdk-devel for jarsigner and keytool (on Fedora 26)
VERSION=0.6.73

curl -o ics-openvpn.apk http://plai.de/android/ics-openvpn-${VERSION}.apk

jarsigner -verify ics-openvpn.apk
#jarsigner -verify -verbose:all -certs ics-openvpn.apk

unzip -q -o ics-openvpn.apk -d ics-openvpn
keytool -printcert -file ics-openvpn/META-INF/CERT.RSA

# ***NOTE*** manually verify the output of the script with this output, 
# especially the SHA256 hash.

# XXX fix the target path
#cp -r ics-openvpn/lib path/to/copy/to

#Owner: CN=Arne Schwabe, C=DE
#Issuer: CN=Arne Schwabe, C=DE
#Serial number: 4f821db4
#Valid from: Mon Apr 09 01:22:28 CEST 2012 until: Wed Mar 16 00:22:28 CET 2112
#Certificate fingerprints:
#	 MD5:  48:72:B5:8E:1A:40:89:0A:94:7D:D5:F5:20:E2:56:70
#	 SHA1: C6:D6:D4:96:5A:A8:A9:A8:CC:84:54:75:42:4F:90:91:D2:56:DB:6D
#	 SHA256: B2:64:83:EA:68:FB:0A:B6:CD:51:42:0C:7E:5E:84:BB:28:B9:80:D8:70:36:22:2F:69:27:B3:61:C7:CB:A9:02
#	 Signature algorithm name: SHA1withRSA
#	 Version: 3
