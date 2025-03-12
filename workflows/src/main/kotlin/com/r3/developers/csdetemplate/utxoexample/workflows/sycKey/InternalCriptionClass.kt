package com.r3.developers.csdetemplate.utxoexample.workflows.sycKey

import com.r3.developers.csdetemplate.utxoexample.workflows.services.readEnvFile
import net.corda.v5.base.annotations.CordaSerializable
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@CordaSerializable
class InternalCriptionClass {

    @Transient
    private var PASSWORD: String

    @Transient
    private var HASH_ALGORITHM: String

    @Transient
    private var CIPHER_ALGORITHM: String

    @Transient
    private var SECRETKEYSPEC_ALGORITHM: String

    @Transient
    private var secretKey: SecretKeySpec? = null

    @Transient
    private var KEY_SIZE: Int

    init {
        val env = readEnvFile("../.env") // Specify the correct path to your .env file
        PASSWORD = env["PASSWORD"] ?: "password12345678"
        HASH_ALGORITHM = env["HASH_ALGORITHM"] ?: "SHA-256"
        CIPHER_ALGORITHM = env["CIPHER_ALGORITHM"] ?: "AES/ECB/PKCS5Padding"
        SECRETKEYSPEC_ALGORITHM = env["SECRETKEYSPEC_ALGORITHM"] ?: "AES"
        KEY_SIZE = env["KEY_SIZE"]?.toIntOrNull() ?: 16
    }

    private fun getSecretKey(): SecretKeySpec {
        if (secretKey == null) {
            val keyBytes = PASSWORD.toByteArray()
            val sha = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashedBytes = sha.digest(keyBytes)
            val truncatedBytes = hashedBytes.copyOf(KEY_SIZE)
            secretKey = SecretKeySpec(truncatedBytes, SECRETKEYSPEC_ALGORITHM)
        }
        return secretKey!!
    }

    fun encryptDocument(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encryptedBytes = cipher.doFinal(plainText)
        return encryptedBytes
    }

    fun decryptDocument(encryptedText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey())
        val decryptedBytes = cipher.doFinal(encryptedText)
        return decryptedBytes
    }
}
