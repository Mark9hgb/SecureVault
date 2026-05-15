# Keep application classes
-keep class com.vaultapp.securevault.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ActivityContextWrapper { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class * implements androidx.room.RoomDatabase$Callback { *; }
-dontwarn androidx.room.paging.**

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class net.zetetic.** { *; }

# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Keep Biometric
-keep class androidx.biometric.** { *; }

# Keep Kotlin Metadata
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Keep JSR 305 annotations (for nullability)
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# Keep GSON / Moshi / other JSON
-keepattributes Signature
-keepattributes *Annotation*

# Keep view binding
-keep class * implements androidx.viewbinding.ViewBinding { *; }

# Keep AndroidX
-keep class androidx.** { *; }
