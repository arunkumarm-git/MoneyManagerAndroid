# R8 / ProGuard rules for Money Manager

# Optimization settings
-repackageclasses
-allowaccessmodification

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Android Architecture Components & Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}

# WorkManager
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Jetpack Compose & ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
-keep class androidx.compose.material.icons.** { *; }

# Data Models & Entities (Ensure JSON backup / Room persistence field names are preserved)
-keepclassmembers class com.moneymanagement.app.data.** {
    <fields>;
    <methods>;
}

# Biometrics
-keep class androidx.biometric.** { *; }
