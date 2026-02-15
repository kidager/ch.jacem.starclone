# StarClone ProGuard Rules

# Don't obfuscate - this is open source, and the biggest size
# contributor is native libs which we can't shrink.
-dontobfuscate

# Keep Jackson-serialized model classes (used in RcloneRcd)
-keep class ch.jacem.starclone.RcloneRcd$** { *; }

# Keep Items used with Jackson/serialization
-keep class ch.jacem.starclone.Items.** { *; }

# Keep rclone provider options (deserialized from JSON)
-keep class ch.jacem.starclone.rclone.ProviderOption { *; }
-keep class ch.jacem.starclone.rclone.Provider { *; }

# Keep BroadcastReceivers and Services referenced in AndroidManifest
-keep class ch.jacem.starclone.core.updates.UpdateUserchoiceReceiver { *; }
-keep class ch.jacem.starclone.Services.** { *; }

# Keep WorkManager workers (instantiated by reflection)
-keep class ch.jacem.starclone.workmanager.** { *; }
-keep class ch.jacem.starclone.core.updates.workmanager.** { *; }

# Keep VirtualContentProvider (registered in manifest)
-keep class ch.jacem.starclone.VirtualContentProvider { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# BouncyCastle/Conscrypt/OpenJSSE (OkHttp optional dependencies)
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
