# Ruta: app/proguard-rules.pro (REEMPLAZA el archivo completo)

# =========================================================
# kotlinx.serialization
# =========================================================
# Sin estas reglas, R8 puede eliminar/renombrar los campos de LevelDto/NodeDto
# o el serializer generado en compilación, rompiendo la carga de niveles
# desde los JSON de assets (crash o niveles vacíos en producción).

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Mantiene los serializers generados por el compiler plugin de kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Mantiene las clases @Serializable del proyecto (LevelDto, NodeDto) y sus
# serializers generados (el "$$serializer" que genera el plugin).
-keep,includedescriptorclasses class com.hoshiraflow.data.**$$serializer { *; }
-keepclassmembers class com.hoshiraflow.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.hoshiraflow.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.hoshiraflow.data.** { *; }

# =========================================================
# AdMob / Google Play Services Ads
# =========================================================
# El SDK trae sus propias consumer-rules.pro dentro del AAR (se aplican
# automáticamente), pero estas de respaldo evitan romper callbacks por
# reflection en versiones futuras del SDK.
-keep public class com.google.android.gms.ads.** {
    public *;
}
-keep public class com.google.android.gms.internal.ads.** {
    public *;
}

# =========================================================
# Modelos de dominio (Board/Cell/Node/PuzzleColor)
# =========================================================
# No usan reflection ni serialización directa, pero al ser el "contrato"
# central del juego, se protegen de renombrado agresivo por seguridad -
# evita falsos positivos difíciles de rastrear si algo los usa por nombre
# en el futuro (ej. logging, debugging remoto).
-keep class com.hoshiraflow.domain.model.** { *; }

# =========================================================
# Líneas de stack trace legibles (útil para Crashlytics más adelante)
# =========================================================
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

