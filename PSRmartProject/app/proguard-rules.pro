-keep class com.psrmart.app.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Keep Room entities and DAOs
-keep class com.psrmart.app.data.model.** { *; }
-keep class com.psrmart.app.data.repository.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Gson model classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Coil
-keep class coil.** { *; }

# Keep Compose stability
-keep class androidx.compose.** { *; }
