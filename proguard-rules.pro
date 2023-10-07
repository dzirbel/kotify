# do not obfuscate code
-dontobfuscate

# do not create notes, which clutter the log
-dontnote **

# keep main method
-keepclasseswithmembers public class com.dzirbel.kotify.MainKt {
    public static void main(java.lang.String[]);
}

# ignore usages of android @SuppressLint annotation
# -dontwarn android.annotation.SuppressLint


### Exposed
-keep class org.jetbrains.exposed.** { *; }
-keep class org.sqlite.** { *; }

# keep reflectively-invoked constructors for Entity classes
-keep class com.dzirbel.kotify.** { <init>(...); }


### Ktor
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# workaround for https://youtrack.jetbrains.com/issue/KTOR-5985
-dontwarn io.ktor.events.Events$HandlerRegistration


### OkHttp
# A resource is loaded with a relative path so the package of this class must be preserved.
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**


### kotlinx.serialization
# Kotlin serialization looks up the generated serializer classes through a function on companion
# objects. The companions are looked up reflectively so we need to explicitly keep these functions.
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# If a companion has the serializer function, keep the companion field on the original type so that
# the reflective lookup succeeds.
-if class **.*$Companion {
  kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class <1>.<2> {
  <1>.<2>$Companion Companion;
}
