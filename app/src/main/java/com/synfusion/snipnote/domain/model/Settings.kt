package com.synfusion.snipnote.domain.model

enum class ThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "Follow system"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

/**
 * How long the Safe workspace stays unlocked after the app leaves the foreground.
 * [NEVER] disables the timer but keeps the manual "Lock now" action available.
 */
enum class AutoLockTimeout(val seconds: Long, val label: String) {
    IMMEDIATELY(0, "Immediately"),
    FIFTEEN_SECONDS(15, "After 15 seconds"),
    ONE_MINUTE(60, "After 1 minute"),
    FIVE_MINUTES(300, "After 5 minutes"),
    NEVER(Long.MAX_VALUE, "Never (manual lock only)"),
    ;

    companion object {
        fun fromSeconds(value: Long): AutoLockTimeout =
            entries.firstOrNull { it.seconds == value } ?: ONE_MINUTE
    }
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.ONE_MINUTE,
    val blockScreenCapture: Boolean = true,
    val defaultSort: SortOrder = SortOrder.UPDATED_DESC,
    val includeSafeInBackup: Boolean = false,
    val onboarded: Boolean = false,
)
