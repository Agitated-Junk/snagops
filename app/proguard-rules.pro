# Apache POI rules
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.xmlbeans.** { *; }

# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.sitesnag.tracker.domain.model.** { *; }
-keep class com.sitesnag.tracker.data.local.entity.** { *; }
