# Project-specific ProGuard rules for the release (minified) build type.
# The app talks over Retrofit + reflective Moshi (KotlinJsonAdapterFactory) and uses Room (KSP),
# both of which rely on reflection over Kotlin metadata — keep the pieces R8 can't infer statically.

# Kotlin metadata (needed by reflective Moshi)
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Moshi (reflective KotlinJsonAdapterFactory)
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class * { @com.squareup.moshi.Json <fields>; }
-keepclassmembers class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# KEEP ALL DTOs used by reflection (Moshi reads/writes these via reflection, not codegen)
-keep class uz.ownsms.sender.data.remote.** { *; }
-keepclassmembers class uz.ownsms.sender.data.remote.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**
