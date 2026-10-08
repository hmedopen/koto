# Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl {
    public <init>();
    public <init>(...);
    *;
}
-keep class androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Koto Database DAOs & Entities
-keep class com.koto.app.feature.cards.data.db.** { *; }
-keepclassmembers class com.koto.app.feature.cards.data.db.** { *; }
-keep class com.koto.app.feature.translator.data.db.** { *; }
-keepclassmembers class com.koto.app.feature.translator.data.db.** { *; }

# Google ML Kit Translation
-keep class com.google.mlkit.** { *; }
-keepclassmembers class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Atilika Kuromoji Japanese Tokenizer & Dictionaries
-keep class com.atilika.kuromoji.** { *; }
-keepclassmembers class com.atilika.kuromoji.** { *; }
-dontwarn com.atilika.kuromoji.**

# Keep data models used with JSON / State / Room
-keep class com.koto.app.ui.screens.cards.CardContext { *; }
-keep class com.koto.app.ui.screens.cards.CardExample { *; }
-keep class com.koto.app.ui.screens.cards.Flashcard { *; }
-keep class com.koto.app.ui.screens.cards.FlashcardDeck { *; }
-keep class com.koto.app.feature.cards.data.** { *; }
-keep class com.koto.app.feature.lesson.data.** { *; }
-keep class com.koto.app.feature.translator.data.** { *; }

# Preserve reflection attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
