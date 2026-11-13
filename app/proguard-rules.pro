# Keep OkHttp/Coil defaults come from their consumer rules.
# We parse JSON with org.json and use no reflection or serialization.

# Keep Kotlin metadata for stack traces / future tooling.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, SourceFile, LineNumberTable

# OkHttp platform checks reference optional classes.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.api.client.http.**
