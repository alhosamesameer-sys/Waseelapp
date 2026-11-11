package com.waseel.assistant.privacy

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** Stores PCM frames supplied by an approved duplex call-audio route; it never captures the microphone itself. */
class EncryptedAudioStore(private val context: Context) {
    private val alias = "waseel_call_audio_v1"
    private fun key(): java.security.Key {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.getKey(alias, null)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }

    fun save(callId: Long, pcm16: ByteArray): File {
        require(callId > 0 && pcm16.isNotEmpty())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val file = File(context.filesDir, "call_${callId}.wpcm.enc")
        FileOutputStream(file).use { out ->
            out.write(byteArrayOf('W'.code.toByte(), 'S'.code.toByte(), 'A'.code.toByte(), '1'.code.toByte()))
            out.write(cipher.iv)
            out.write(cipher.doFinal(pcm16))
        }
        return file
    }

    fun decrypt(file: File): ByteArray {
        val bytes = file.readBytes()
        require(bytes.size > 32 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(87,83,65,49)))
        val iv = bytes.copyOfRange(4, 16)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(bytes.copyOfRange(16, bytes.size))
    }
}
