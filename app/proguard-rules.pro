# This will allow us to get the correct file line numbers to the errors
-keepattributes SourceFile,LineNumberTable

-dontwarn com.google.errorprone.annotations.*
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.*
-dontwarn org.openjsse.net.ssl.*

-keepclassmembers class com.wireguard.android.backend.GoBackend {
    <fields>;
}
-keepclassmembers class com.wireguard.android.backend.GoBackend$GhettoCompletableFuture {
    <methods>;
}
-keepclassmembers class android.net.VpnService {
    <methods>;
}