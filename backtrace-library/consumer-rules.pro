-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.InlineMe

-dontwarn com.squareup.tape.QueueFile

-keepattributes *Annotation*
-keep class backtraceio.** { *; }
-keepnames @interface backtraceio.**
-keepclassmembers class * {
    @backtraceio.gson.annotations.SerializedName <fields>;
}
