# OpenZilla proguard rules.
# Room entities/DAOs are annotation-processed at compile time, no extra keep rules needed
# for the generated code path. Kept minimal on purpose — smaller attack/maintenance surface.
-keepattributes *Annotation*
-keep class com.openzilla.app.data.** { *; }
