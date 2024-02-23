# This will allow us to get the correct file line numbers to the errors
-keepattributes SourceFile,LineNumberTable

-dontwarn com.google.errorprone.annotations.*
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.*
-dontwarn org.openjsse.net.ssl.*
