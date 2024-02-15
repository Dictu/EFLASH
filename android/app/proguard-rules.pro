# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/<user>/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order service changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


#-dontobfuscate

-keepattributes Signature,EnclosingMethod,*Annotation*,InnerClasses,SourceFile,LineNumberTable

-keep class com.google.** { *; }
-dontwarn com.google.**
-keep class org.apache.** { *; }
-dontwarn org.apache.**
-keep class android.support.** { *; }
-dontwarn android.support.**
#-keep class android.support.v7.app.** { *; }
#-keep interface android.support.v7.app.** { *; }
-keep class android.util.** { *;}
-dontwarn android.util.**
-keep class android.app.** { *;}
-dontwarn android.app.**
-keep class android.content.** { *;}
-dontwarn android.content.**
-keep class java.lang.** { *;}
-dontwarn java.lang.**

-keep class com.mcxiaoke.** { *;}
-dontwarn com.mcxiaokes.**

-keep class com.jakewharton.** { *;}
-dontwarn com.jakewharton.**

-keep class org.ocpsoft.** { *;}
-dontwarn org.ocpsoft.**

-keep class butterknife.** { *;}
-dontwarn butterknife.**

-keep class net.danlew.android.joda.** { *;}
-dontwarn net.danlew.android.joda.**

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep public class * extends java.lang.Exception

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

##---------------Begin: proguard configuration for Countly  ----------
#https://resources.count.ly/docs/countly-sdk-for-android
-keep class org.openudid.** { *; }
-keep class ly.count.android.sdk.** { *; }
##---------------End: proguard configuration for Countly  ----------