package com.android.ai.mcp.storage.automation

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CredentialCipherManager {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "mcp_credential_vault_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE_BYTES = 12
        private const val AUTH_TAG_SIZE_BITS = 128
    }

    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val ivEncoded = Base64.encodeToString(iv, Base64.NO_WRAP)
        val dataEncoded = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        return "$ivEncoded:$dataEncoded"
    }

    fun decrypt(ciphertext: String): String {
        val parts = ciphertext.split(":")
        require(parts.size == 2) { "Invalid ciphertext format" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)
        require(iv.size == IV_SIZE_BYTES) { "Invalid IV length" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(AUTH_TAG_SIZE_BITS, iv)
        )
        val plaintextBytes = cipher.doFinal(encryptedBytes)
        return String(plaintextBytes, StandardCharsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
