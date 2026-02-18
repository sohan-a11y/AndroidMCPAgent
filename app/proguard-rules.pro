# Android AI MCP ProGuard Rules

# Keep Room schema and generated adapters.
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Kotlin serialization metadata.
-keepclassmembers class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class *
