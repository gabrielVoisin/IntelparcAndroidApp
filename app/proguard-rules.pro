# Release builds are not shrunk in this prototype (isMinifyEnabled = false).
# Before enabling R8, keep the kotlinx.serialization models and the Retrofit interfaces:
-keepattributes *Annotation*, InnerClasses, Signature
-keep,includedescriptorclasses class com.drautomation.intelpark.data.**$$serializer { *; }
-keepclassmembers class com.drautomation.intelpark.data.** { *** Companion; }
