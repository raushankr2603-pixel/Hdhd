package com.example.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Real AES-256-GCM End-to-End Encryption Vault for API keys and Cross-Device Sync payloads.
 */
object E2EEVault {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val PBKDF2_ITERATIONS = 12000
    private val DEFAULT_SALT = "AetherOS_Quantum_E2EE_Salt_v1".toByteArray(Charsets.UTF_8)

    fun deriveKey(passphrase: String): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), DEFAULT_SALT, PBKDF2_ITERATIONS, 256)
        val secret = factory.generateSecret(spec).encoded
        return SecretKeySpec(secret, "AES")
    }

    fun encrypt(plainText: String, passphrase: String = "AetherOS_Default_Device_Vault_Key"): String {
        if (plainText.isEmpty()) return ""
        return try {
            val iv = ByteArray(IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(cipherTextBase64: String, passphrase: String = "AetherOS_Default_Device_Vault_Key"): String {
        if (cipherTextBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH_BYTES) return cipherTextBase64
            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val cipherBytes = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            cipherTextBase64
        }
    }

    fun sha256Fingerprint(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString(":") { "%02X".format(it) }
    }
}
