# Media3 / ExoPlayer keep rules (ship with the library's own consumer rules,
# these are extra safety nets for reflectively accessed codec classes).
-keep class androidx.media3.decoder.** { *; }
-dontwarn androidx.media3.**
