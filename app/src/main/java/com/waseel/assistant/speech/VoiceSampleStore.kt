package com.waseel.assistant.speech

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** Keeps user-provided voice reference audio encrypted in app-private storage. */
class VoiceSampleStore(private val context: Context) {
    private val alias = "waseel_voice_sample_v1"

    private fun key(): java.security.Key {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.getKey(alias, null)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    fun save(bytes: ByteArray): File {
        require(bytes.size in 1024..(25 * 1024 * 1024)) { "حجم العينة يجب أن يكون بين 1KB و25MB." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val folder = File(context.filesDir, "voice_samples").apply { mkdirs() }
        val file = File(folder, "reference_audio.wsv.enc")
        FileOutputStream(file).use { out ->
            out.write(byteArrayOf(87, 86, 83, 49))
            out.write(cipher.iv)
            out.write(cipher.doFinal(bytes))
        }
        return file
    }

    fun hasSample(): Boolean = File(context.filesDir, "voice_samples/reference_audio.wsv.enc").exists()
    fun delete() { File(context.filesDir, "voice_samples/reference_audio.wsv.enc").delete() }
}
