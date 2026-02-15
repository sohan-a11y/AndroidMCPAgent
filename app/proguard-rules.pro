# Android MCP Agent ProGuard Rules

# Keep Ktor classes (needed for WebSocket server)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.android.mcp.agent.**$$serializer { *; }
-keepclassmembers class com.android.mcp.agent.** {
    *** Companion;
}
-keepclasseswithmembers class com.android.mcp.agent.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep protocol data classes (used for JSON serialization)
-keep class com.android.mcp.agent.protocol.** { *; }
-keep class com.android.mcp.agent.logging.AuditEntry { *; }
