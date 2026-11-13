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

    /** Decrypts only into the app cache for local playback or a future verified XTTS runtime. */
    fun copyDecryptedToCache(): File {
        val source = File(context.filesDir, "voice_samples/reference_audio.wsv.enc")
        require(source.exists()) { "لم تتم إضافة عينة صوتية." }
        val bytes = source.readBytes()
        require(bytes.size > 32 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(87, 86, 83, 49))) {
            "ملف العينة المحفوظ غير صالح."
        }
        val iv = bytes.copyOfRange(4, 16)
        val encrypted = bytes.copyOfRange(16, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val plain = cipher.doFinal(encrypted)
        val target = File(context.cacheDir, "waseel_voice_reference_preview.wav")
        target.writeBytes(plain)
        return target
    }

    fun delete() { File(context.filesDir, "voice_samples/reference_audio.wsv.enc").delete() }
}
