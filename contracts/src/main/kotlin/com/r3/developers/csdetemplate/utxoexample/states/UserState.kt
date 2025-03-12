package com.r3.developers.csdetemplate.utxoexample.states

import com.r3.developers.csdetemplate.utxoexample.contracts.UserContract
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.enums.Role
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*


@BelongsToContract(UserContract::class)
@CordaSerializable
data class UserState (
    val id: UUID = UUID.randomUUID(),
    val username: MemberX500Name,
    val publicKey: String,
    val privateKey: String,
    val loginName: String,
    val password: String,
    val role: Role,
    val createdAt: Instant,
    val lastUpdated: Instant,
    val recordState: RecordState,
    var version: Int,
    private val participants: List<PublicKey>,
    ) : ContractState {

    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    fun convertPublicKey(): PublicKey {
        val keyBytes: ByteArray = Base64.getDecoder().decode(publicKey)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(keySpec)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserState

        if (id != other.id) return false
        if (username != other.username) return false
        if (publicKey != other.publicKey) return false
        if (privateKey != other.privateKey) return false
        if (loginName != other.loginName) return false
        if (password != other.password) return false
        if (role != other.role) return false
        if (createdAt != other.createdAt) return false
        if (lastUpdated != other.lastUpdated) return false
        if (recordState != other.recordState) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + publicKey.hashCode()
        result = 31 * result + privateKey.hashCode()
        result = 31 * result + loginName.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + recordState.hashCode()
        result = 31 * result + version
        return result
    }

    fun updateUser(publicKey: String, privateKey: String): Any {
        this.version++;
        return copy(publicKey = publicKey, privateKey = privateKey, lastUpdated = Instant.now(), version = version)
    }
}