package com.omkarnub.kanri.data.profile

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UserProfilePreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userNameFlow = MutableStateFlow(loadUserName())
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    private val _userDobFlow = MutableStateFlow(loadUserDob())
    val userDobFlow: StateFlow<String> = _userDobFlow.asStateFlow()

    private val _profilePhotoPathFlow = MutableStateFlow(loadProfilePhotoPath())
    val profilePhotoPathFlow: StateFlow<String?> = _profilePhotoPathFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "kanri_user_profile_prefs"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_DOB = "key_user_dob"
        private const val KEY_PROFILE_PHOTO_PATH = "key_profile_photo_path"
        private const val PROFILE_IMAGE_FILENAME = "kanri_profile_avatar.jpg"
        private const val DEFAULT_NAME = "Alex"

        @Volatile
        private var INSTANCE: UserProfilePreferences? = null

        fun getInstance(context: Context): UserProfilePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserProfilePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun loadUserName(): String {
        return prefs.getString(KEY_USER_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
    }

    private fun loadUserDob(): String {
        return prefs.getString(KEY_USER_DOB, "") ?: ""
    }

    private fun loadProfilePhotoPath(): String? {
        val path = prefs.getString(KEY_PROFILE_PHOTO_PATH, null)
        if (path != null && File(path).exists()) {
            return path
        }
        return null
    }

    var userName: String
        get() = loadUserName()
        set(value) {
            val trimmed = value.trim()
            val finalName = if (trimmed.isBlank()) DEFAULT_NAME else trimmed
            prefs.edit().putString(KEY_USER_NAME, finalName).apply()
            _userNameFlow.value = finalName
        }

    var userDob: String
        get() = loadUserDob()
        set(value) {
            val trimmed = value.trim()
            prefs.edit().putString(KEY_USER_DOB, trimmed).apply()
            _userDobFlow.value = trimmed
        }

    var profilePhotoPath: String?
        get() = loadProfilePhotoPath()
        private set(value) {
            prefs.edit().putString(KEY_PROFILE_PHOTO_PATH, value).apply()
            _profilePhotoPathFlow.value = value
        }

    /**
     * Copies and compresses image from Uri into internal storage file so
     * permissions are retained across sessions without needing permanent uri grants.
     */
    fun saveBitmapInternal(bitmap: Bitmap): Boolean {
        return try {
            val file = File(context.filesDir, PROFILE_IMAGE_FILENAME)
            val outputStream = FileOutputStream(file)
            // Downscale to max 512x512 for performance & minimal memory
            val maxDim = 512
            val scale = (maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)).coerceAtMost(1f)
            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            profilePhotoPath = file.absolutePath
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun savePhotoFromUri(sourceUri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return false
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return false
            saveBitmapInternal(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun savePhotoFromUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()
            if (bitmap == null) return@withContext false
            saveBitmapInternal(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun removeProfilePhoto() {
        try {
            val file = File(context.filesDir, PROFILE_IMAGE_FILENAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        profilePhotoPath = null
    }

    fun reset() {
        removeProfilePhoto()
        prefs.edit().clear().apply()
        _userNameFlow.value = DEFAULT_NAME
        _userDobFlow.value = ""
        _profilePhotoPathFlow.value = null
    }
}
