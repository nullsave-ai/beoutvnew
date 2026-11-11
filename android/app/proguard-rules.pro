-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.beouttv.app.**$$serializer { *; }
-keepclassmembers class com.beouttv.app.** { *** Companion; }
-keepclasseswithmembers class com.beouttv.app.** { kotlinx.serialization.KSerializer serializer(...); }

-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-assumenosideeffects class android.util.Log { public static *** d(...); public static *** v(...); }
