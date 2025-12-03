package com.swordfish.lemuroid.lib.core.assetsmanager

import android.content.Context
import android.content.SharedPreferences
import com.swordfish.lemuroid.lib.core.CoreUpdater
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipInputStream

class DolphinAssetsManager : CoreID.AssetsManager {

    override suspend fun clearAssets(directoriesManager: DirectoriesManager) {
        getAssetsDirectory(directoriesManager).deleteRecursively()
    }

    override suspend fun retrieveAssetsIfNeeded(
        coreUpdaterApi: CoreUpdater.CoreManagerApi,
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ) {
        if (!updatedRequested(directoriesManager, sharedPreferences)) {
            return
        }

        try {
            extractDolphinAssets(directoriesManager, sharedPreferences)
        } catch (e: Throwable) {
            Timber.e(e, "Failed to extract Dolphin assets")
            getAssetsDirectory(directoriesManager).deleteRecursively()
        }
    }

    private suspend fun extractDolphinAssets(
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ) = withContext(Dispatchers.IO) {
        val dolphinDir = getAssetsDirectory(directoriesManager)
        dolphinDir.deleteRecursively()
        dolphinDir.mkdirs()

        val context = directoriesManager.appContext
        context.assets.open("dolphin.zip").use { inputStream ->
            ZipInputStream(inputStream).use { zipInputStream ->
                while (true) {
                    val entry = zipInputStream.nextEntry ?: break

                    // Remover "dolphin-emu/" do início
                    val entryName = entry.name.removePrefix("dolphin-emu/")

                    // Pular se ficar vazio (a pasta raiz)
                    if (entryName.isEmpty()) continue

                    val destFile = File(dolphinDir, entryName)  // ← usar entryName

                    Timber.d("${entry.name} → ${destFile.absolutePath}")

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        destFile.outputStream().use { output ->
                            zipInputStream.copyTo(output)
                        }
                    }
                }

                Timber.d("=== VERIFICATION ===")
                val sysDir = File(dolphinDir, "Sys")
                Timber.d("Sys exists: ${sysDir.exists()} at ${sysDir.absolutePath}")
                sysDir.listFiles()?.forEach {
                    Timber.d("  - ${it.name}")
                }
            }
        }

        sharedPreferences.edit()
            .putString(DOLPHIN_ASSETS_VERSION_KEY, DOLPHIN_ASSETS_VERSION)
            .commit()

        Timber.i("Dolphin assets extracted successfully")
    }


    private suspend fun updatedRequested(
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ): Boolean = withContext(Dispatchers.IO) {
        val directoryExists = getAssetsDirectory(directoriesManager).exists()
        val currentVersion = sharedPreferences.getString(DOLPHIN_ASSETS_VERSION_KEY, "none")
        val hasCurrentVersion = currentVersion == DOLPHIN_ASSETS_VERSION

        !directoryExists || !hasCurrentVersion
    }

    private suspend fun getAssetsDirectory(directoriesManager: DirectoriesManager): File {
        return withContext(Dispatchers.IO) {
            File(directoriesManager.getSystemDirectory(), DOLPHIN_ASSETS_FOLDER_NAME)
        }
    }

    companion object {
        const val DOLPHIN_ASSETS_VERSION = "1.0"
        const val DOLPHIN_ASSETS_VERSION_KEY = "dolphin_assets_version_key"
        const val DOLPHIN_ASSETS_FOLDER_NAME = "dolphin-emu"
    }
}
