package com.encryptedsharedprefrencesdemo.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager


object EncryptedPreferences {

    private val EMPTY_ASSOCIATED_DATA = ByteArray(0)
    private const val DEFAULT_SUFFIX = "_defaultPref"
    private const val ENC_SUFFIX = "_defaultEnc"
    private const val DEBUG_SUFFIX = "_debug"
    private var KEY_URI = "android-keystore://"
    private var mDefaultPref: SharedPreferences? = null
    private var aead: Aead? = null
    private var isDebuggable: Boolean = false
  
    // Builder class to set up EncPref
    class Builder {
        private var mPrefName: String? = null
        private var mContext: Context? = null
        private var mUseDefaultPref = false

        // Setting the context
        fun setContext(mContext: Context): Builder {
            this.mContext = mContext
            return this
        }

        // Setting the preference name
        fun setPrefName(mPrefName: String): Builder {
            this.mPrefName = mPrefName
            return this
        }

        // Setting the use of default preference
        fun setUseDefaultPref(mUseDefaultPref: Boolean): Builder {
            this.mUseDefaultPref = mUseDefaultPref
            return this
        }
        
        fun setDebuggable(isDebuggable: Boolean): Builder {
            EncryptedPreferences.isDebuggable = isDebuggable
            return this
        }

        private fun getOrGenerateKeyHandle(): KeysetHandle? {
            return AndroidKeysetManager.Builder()
                .withSharedPref(
                    mContext,
                    base64Encode(mContext?.packageName!!.toByteArray(Charsets.US_ASCII)),
                    base64Encode("${mContext?.packageName}$ENC_SUFFIX".toByteArray(Charsets.US_ASCII))
                )
                .withKeyTemplate(KeyTemplates.get("AES128_GCM"))
                .withMasterKeyUri(KEY_URI)
                .build()
                .keysetHandle
        }

        // Building EncPref instance
        fun build(): EncryptedPreferences {
            // Validating context
            if (mContext == null) {
                throw RuntimeException("Context should not be null, please set context.")
            }
            // Setting default preference name if not provided
            if (mPrefName.isNullOrEmpty()) {
                mPrefName = mContext?.packageName
            }
            // Adding default suffix if using default preference
            if (mUseDefaultPref) {
                mPrefName = "${mPrefName}$DEFAULT_SUFFIX"
            }
            // Initializing cryptographic settings
            if (aead == null) {
                HybridConfig.register()
                KEY_URI = "$KEY_URI${mContext?.packageName}"
                aead = getOrGenerateKeyHandle()?.getPrimitive(Aead::class.java)
            }
            initPref(
                mContext!!,
                mPrefName!!
            )
            return EncryptedPreferences
        }
    }

    // Initializing the EncryptedSharedPreferences
    private fun initPref(mContext: Context, mPrefName: String) {
        val masterKey = MasterKey.Builder(mContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        // Creating EncryptedSharedPreferences instance
        mDefaultPref = EncryptedSharedPreferences.create(
            mContext,
            mPrefName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Encoding byte array to Base64 String
    private fun base64Encode(input: ByteArray): String? {
        return Base64.encodeToString(input, Base64.DEFAULT)
    }

    // Decoding Base64 String to byte array
    private fun base64Decode(input: String): ByteArray? {
        return Base64.decode(input, Base64.DEFAULT)
    }

    // Encrypting text
    private fun encryptText(textToEncrypt: String?) = try {
        val cipherText =
            aead?.encrypt(
                textToEncrypt?.toByteArray(Charsets.UTF_8),
                EMPTY_ASSOCIATED_DATA
            )
        when {
            (cipherText == null) -> textToEncrypt
            else -> base64Encode(cipherText)
        }
    } catch (ignore: Exception) {
        null
    }

    // Decrypting text
    private fun decryptText(textToDecrypt: String?): String? = try {
        val plainTextArray =
            aead?.decrypt(
                base64Decode(
                    textToDecrypt ?: ""
                ),
                EMPTY_ASSOCIATED_DATA
            )
        when {
            (plainTextArray == null) -> textToDecrypt
            else -> String(plainTextArray, Charsets.UTF_8)
        }
    } catch (ignore: Exception) {
        null
    }

    // Getting SharedPreferences instance
    private val myPref: SharedPreferences
        get() {
            if (mDefaultPref != null) {
                return mDefaultPref!!
            }
            throw RuntimeException(
                "EncryptedPrefrences class not correctly instantiated. Please call Builder.setContext().build() in the Application class onCreate."
            )
        }

    // Getting value from SharedPreferences
    private fun getValueFromPref(key: String): String? {
        return decryptText(
            myPref.getString(
                base64Encode(
                    key.toByteArray(Charsets.US_ASCII)
                ), ""
            )
        )
    }

    // Setting value to SharedPreferences
    private fun setValueOnPref(key: String, value: String) {
        myPref.edit().apply {
            putString(
                base64Encode(
                    key.toByteArray(Charsets.US_ASCII)
                ), encryptText(
                    value
                ) ?: value
            )
            apply()
        }
    }

    // Functions to get and put different types of values to SharedPreferences
    fun getString(key: String, defaultValue: String = ""): String {
        return getValueFromPref(key) ?: defaultValue
    }

    fun putString(key: String, value: String) {
        setValueOnPref(
            key,
            value
        )
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val value = getValueFromPref(key)
        return if (value.isNullOrEmpty()) defaultValue else value.toBoolean()
    }

    fun putBoolean(key: String, value: Boolean) {
        setValueOnPref(
            key,
            value.toString()
        )
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        val value = getValueFromPref(key)
        return if (value.isNullOrEmpty()) defaultValue else value.toLong()
    }

    fun putLong(key: String, value: Long) {
        setValueOnPref(
            key,
            value.toString()
        )
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        val value = getValueFromPref(key)
        return if (value.isNullOrEmpty()) defaultValue else value.toInt()
    }

    fun putInt(key: String, value: Int) {
        setValueOnPref(
            key,
            value.toString()
        )
    }

    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        val value = getValueFromPref(key)
        return if (value.isNullOrEmpty()) defaultValue else value.toDouble()
    }

    fun putDouble(key: String, value: Double) {
        setValueOnPref(
            key,
            value.toString()
        )
    }

    fun getFloat(key: String, defaultValue: Float = 0.0f): Float {
        val value = getValueFromPref(key)
        return if (value.isNullOrEmpty()) defaultValue else value.toFloat()
    }

    fun putFloat(key: String, value: Float) {
        setValueOnPref(
            key,
            value.toString()
        )
    }
    
    //This code disable encryption for DEBUG builds.
    fun getAll(): MutableMap<String, Any?> {
          val map =
              (if (isDebuggable) myPref.all.filter { it.key.contains(DEBUG_SUFFIX) } else myPref.all.filter {
                  !it.key.contains(DEBUG_SUFFIX)
              }).toMutableMap()
          return if (isDebuggable) {
              map
          } else {
              val tmpMap = mutableMapOf<String, Any?>()
              for ((key, value) in map) {
                  tmpMap[String(base64Decode(key) ?: EMPTY_ASSOCIATED_DATA, Charsets.UTF_8)] =
                      decryptText(value.toString())
              }
              tmpMap
          }
      }

    // Function to clear SharedPreferences
    fun clear() {
        myPref.edit().

clear().apply()
    }

    // Function to remove a key from SharedPreferences
    fun removeKey(key: String) {
        myPref.edit().apply {
            remove(
                base64Encode(
                    key.toByteArray(Charsets.US_ASCII)
                )
            )
            apply()
        }
    }
}