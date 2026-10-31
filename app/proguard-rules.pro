# Kotlinx serialization: keep generated serializers for backup DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.synfusion.snipnote.data.backup.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.synfusion.snipnote.data.backup.**$$serializer { *; }
-keepclasseswithmembers class com.synfusion.snipnote.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Room generated implementations are referenced reflectively by name only at build time.
-keep class * extends androidx.room.RoomDatabase { *; }
