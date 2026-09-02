package com.example

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val RELEASE_API = "https://api.github.com/repos/hoonex/drive/releases/tags/android-preview"
private const val APK_ASSET_NAME = "PCWheel-android-preview.apk"
private const val INSTALL_ACTION = "com.example.PCWHEEL_UPDATE_INSTALL_STATUS"

data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val expectedSha256: String?,
    val sizeBytes: Long,
    val sourceCommit: String,
)

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data object UpToDate : AppUpdateState
    data class Available(val info: AppUpdateInfo) : AppUpdateState
    data class Downloading(val info: AppUpdateInfo, val progress: Int) : AppUpdateState
    data class Ready(val info: AppUpdateInfo, val apk: File) : AppUpdateState
    data class PermissionRequired(val info: AppUpdateInfo, val apk: File) : AppUpdateState
    data class Installing(val info: AppUpdateInfo) : AppUpdateState
    data class Failed(val message: String) : AppUpdateState
}

class AppUpdater(private val context: Context) {
    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    suspend fun checkForUpdate(autoDownload: Boolean, wifiOnly: Boolean, force: Boolean = false) {
        val current = _state.value
        if (!force && (current is AppUpdateState.Checking || current is AppUpdateState.Downloading || current is AppUpdateState.Installing)) {
            return
        }

        _state.value = AppUpdateState.Checking
        runCatching {
            val info = fetchReleaseInfo()
            if (info.versionCode <= currentVersionCode()) {
                _state.value = AppUpdateState.UpToDate
                return
            }
            _state.value = AppUpdateState.Available(info)
            if (autoDownload && (!wifiOnly || isUnmeteredNetwork())) {
                download(info)
            }
        }.onFailure {
            _state.value = AppUpdateState.Failed(it.message ?: "Update check failed")
        }
    }

    suspend fun downloadAvailable(ignoreWifiPolicy: Boolean = false, wifiOnly: Boolean = true) {
        val info = when (val current = _state.value) {
            is AppUpdateState.Available -> current.info
            is AppUpdateState.Failed -> fetchReleaseInfo()
            else -> return
        }
        if (!ignoreWifiPolicy && wifiOnly && !isUnmeteredNetwork()) {
            _state.value = AppUpdateState.Available(info)
            return
        }
        runCatching { download(info) }
            .onFailure { _state.value = AppUpdateState.Failed(it.message ?: "Update download failed") }
    }

    fun installReadyUpdate() {
        val ready = when (val current = _state.value) {
            is AppUpdateState.Ready -> current
            is AppUpdateState.PermissionRequired -> AppUpdateState.Ready(current.info, current.apk)
            else -> return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            _state.value = AppUpdateState.PermissionRequired(ready.info, ready.apk)
            return
        }

        runCatching {
            installWithPackageInstaller(ready.info, ready.apk)
            _state.value = AppUpdateState.Installing(ready.info)
        }.onFailure {
            _state.value = AppUpdateState.Failed(it.message ?: "Unable to start update installation")
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            installReadyUpdate()
            return
        }
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { _state.value = AppUpdateState.Failed("Unable to open install permission settings") }
    }

    fun resumePendingInstall() {
        val current = _state.value
        if (current is AppUpdateState.PermissionRequired &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls())
        ) {
            installReadyUpdate()
        }
    }

    fun currentVersionName(): String = runCatching {
        installedPackageInfo().versionName ?: "unknown"
    }.getOrDefault("unknown")

    private suspend fun fetchReleaseInfo(): AppUpdateInfo = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "PCWheel-Android-Updater")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("GitHub release check failed (${connection.responseCode})")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            parseReleaseInfo(json)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun download(info: AppUpdateInfo) = withContext(Dispatchers.IO) {
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updateDir.listFiles()?.forEach { if (it.name != "pcwheel-${info.versionCode}.apk") it.delete() }
        val apk = File(updateDir, "pcwheel-${info.versionCode}.apk")
        val partial = File(updateDir, "pcwheel-${info.versionCode}.part")
        partial.delete()

        _state.value = AppUpdateState.Downloading(info, 0)
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "PCWheel-Android-Updater")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("APK download failed (${connection.responseCode})")
            }
            val total = connection.contentLengthLong.takeIf { it > 0L } ?: info.sizeBytes
            var downloaded = 0L
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        downloaded += count
                        if (total > 0L) {
                            val progress = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                            val previous = (_state.value as? AppUpdateState.Downloading)?.progress ?: -1
                            if (progress != previous) _state.value = AppUpdateState.Downloading(info, progress)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        if (partial.length() < 1_000_000L) error("Downloaded APK is unexpectedly small")
        if (info.sizeBytes > 0L && partial.length() != info.sizeBytes) {
            error("Downloaded APK size does not match release metadata")
        }

        val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
        val expectedSha = info.expectedSha256?.lowercase()?.removePrefix("sha256:")
        if (!expectedSha.isNullOrBlank() && actualSha != expectedSha) {
            error("Downloaded APK SHA-256 verification failed")
        }

        if (apk.exists()) apk.delete()
        if (!partial.renameTo(apk)) {
            partial.copyTo(apk, overwrite = true)
            partial.delete()
        }
        verifyDownloadedPackage(apk, info)
        _state.value = AppUpdateState.Ready(info, apk)
    }

    private fun verifyDownloadedPackage(apk: File, info: AppUpdateInfo) {
        val archive = archivePackageInfo(apk) ?: error("Downloaded APK metadata cannot be read")
        if (archive.packageName != context.packageName) error("Downloaded APK package name mismatch")
        if (archive.longVersionCodeCompat() != info.versionCode) error("Downloaded APK version mismatch")

        val installedCerts = signingDigests(installedPackageInfo())
        val archiveCerts = signingDigests(archive)
        if (installedCerts.isEmpty() || archiveCerts.isEmpty() || installedCerts.intersect(archiveCerts).isEmpty()) {
            error("Downloaded APK signing certificate does not match the installed app")
        }
    }

    private fun installWithPackageInstaller(info: AppUpdateInfo, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0L, apk.length()).use { output ->
                apk.inputStream().buffered().use { input -> input.copyTo(output, 64 * 1024) }
                session.fsync(output)
            }
            val callback = Intent(context, UpdateInstallReceiver::class.java).apply {
                action = INSTALL_ACTION
                putExtra("versionName", info.versionName)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, callback, flags)
            session.commit(pendingIntent.intentSender)
        }
    }

    private fun isUnmeteredNetwork(): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return runCatching { !connectivity.isActiveNetworkMetered }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private fun installedPackageInfo(): PackageInfo {
        val packageManager = context.packageManager
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            else -> packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun archivePackageInfo(apk: File): PackageInfo? {
        val packageManager = context.packageManager
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> packageManager.getPackageArchiveInfo(
                apk.absolutePath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> packageManager.getPackageArchiveInfo(
                apk.absolutePath,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            else -> packageManager.getPackageArchiveInfo(
                apk.absolutePath,
                PackageManager.GET_SIGNATURES,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun signingDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        } else {
            info.signatures.orEmpty()
        }
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun currentVersionCode(): Long = installedPackageInfo().longVersionCodeCompat()
}

internal fun parseReleaseInfo(json: JSONObject): AppUpdateInfo {
    val body = json.optString("body")
    val runNumber = Regex("Validated Android Build run:\\s*(\\d+)")
        .find(body)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
        ?: error("Release metadata is missing the validated build number")

    val assets = json.getJSONArray("assets")
    var asset: JSONObject? = null
    for (index in 0 until assets.length()) {
        val candidate = assets.getJSONObject(index)
        if (candidate.optString("name") == APK_ASSET_NAME) {
            asset = candidate
            break
        }
    }
    val apkAsset = asset ?: error("Release APK asset is missing")
    val downloadUrl = apkAsset.getString("browser_download_url")
    if (!downloadUrl.startsWith("https://github.com/hoonex/drive/releases/download/")) {
        error("Release APK URL is outside the expected GitHub repository")
    }

    return AppUpdateInfo(
        versionCode = 100_000L + runNumber,
        versionName = "1.2.$runNumber",
        downloadUrl = downloadUrl,
        expectedSha256 = apkAsset.optString("digest").takeIf { it.isNotBlank() },
        sizeBytes = apkAsset.optLong("size", -1L),
        sourceCommit = json.optString("target_commitish"),
    )
}

@Suppress("DEPRECATION")
private fun PackageInfo.longVersionCodeCompat(): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()
}

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != INSTALL_ACTION) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
                }
                confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirmation != null) context.startActivity(confirmation)
            }

            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: "Android rejected the update installation"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
