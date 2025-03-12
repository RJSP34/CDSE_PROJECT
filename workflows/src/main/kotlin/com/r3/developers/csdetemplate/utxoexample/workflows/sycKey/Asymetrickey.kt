package com.r3.developers.csdetemplate.utxoexample.workflows.sycKey

import net.corda.v5.base.annotations.CordaSerializable
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@CordaSerializable
class AsymmetricKey {
    @Transient
    private var publicKey: PublicKey
    @Transient
    private var privateKey: PrivateKey

    init {
        val keys = generateKeyPair()
        publicKey = keys.public
        privateKey = keys.private
    }

    private fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }

    fun getPublicKey(): PublicKey {
        return publicKey
    }

    fun getPrivateKey(): PrivateKey {
        return privateKey
    }

    fun getPublicKeyAsString(): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    fun getPrivateKeyAsString(): String {
        return Base64.getEncoder().encodeToString(privateKey.encoded)
    }

    fun getPublicKey(publicKeyString: String): PublicKey {
        val keyBytes = Base64.getDecoder().decode(publicKeyString)
        val keySpec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    fun getPrivateKey(privateKeyString: String): PrivateKey {
        val keyBytes = Base64.getDecoder().decode(privateKeyString)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    fun encrypt(data: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    fun decrypt(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(data)
    }

    fun publicKeyToBase64String(): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    fun privateKeyToBase64String(): String {
        return Base64.getEncoder().encodeToString(privateKey.encoded)
    }

    fun base64StringToPublicKey(base64String: String): PublicKey {
        val keyBytes = Base64.getDecoder().decode(base64String)
        val keySpec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    fun base64StringToPrivateKey(base64String: String): PrivateKey {
        val keyBytes = Base64.getDecoder().decode(base64String)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }
}