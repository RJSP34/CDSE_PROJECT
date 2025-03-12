package com.r3.developers.csdetemplate.utxoexample.states

import com.r3.developers.csdetemplate.utxoexample.contracts.DocumentContract
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.time.Instant
import java.util.*

@BelongsToContract(DocumentContract::class)
@CordaSerializable
data class DocumentState (
    val id: UUID = UUID.randomUUID(),
    val documentName: String = "",
    val authorizationID: Int = 0,
    val issuer: MemberX500Name,
    val pdfData: ByteArray = ByteArray(0),
    private val signatures: Map<PublicKey,Boolean>,
    private val participants: List<PublicKey>,
    val createdAt: Instant,
    val lastUpdated: Instant,
    val recordState: RecordState,
    var version: Int
) : ContractState {

    override fun getParticipants(): List<PublicKey> {
        return participants
    }

    fun updateDocument(documentName: String, pdfData: ByteArray, updaterPublicKey: PublicKey): DocumentState? {
        val updatedSignatures = participants.associateWith { it == updaterPublicKey }
            .mapValues { false }
            .toMutableMap()
        updatedSignatures[updaterPublicKey] = true
        val allSignaturesValid = signatures.values.all { it }
        val allParticipantsSigned = signatures.keys.containsAll(participants)
        if (allSignaturesValid && allParticipantsSigned) {
            return null
        }

        this.version++;

        return copy(documentName = documentName, pdfData = pdfData, lastUpdated = Instant.now(), version = version, signatures = updatedSignatures)
    }

    fun validateSignaturesDocument(updaterPublicKey: PublicKey): Boolean {
        val updatedSignatures = participants.associateWith { it == updaterPublicKey }
            .mapValues { false }
            .toMutableMap()
        updatedSignatures[updaterPublicKey] = true
        val allSignaturesValid = signatures.values.all { it }
        val allParticipantsSigned = signatures.keys.containsAll(participants)
        return !(allSignaturesValid && allParticipantsSigned)
    }

    fun updateSignature(publicKey: PublicKey): DocumentState {
        if (signatures[publicKey] == true) {
            throw IllegalStateException("Signature for the provided public key is already true.")
        }

        val updatedSignatures = signatures.toMutableMap()
        updatedSignatures[publicKey] = true
        return copy(signatures = updatedSignatures, version = version++)
    }

    fun getSignatures(): Map<PublicKey, Boolean> {
        return signatures
    }

    fun hasSigned(publicKey: PublicKey): Boolean {
        return signatures[publicKey] ?: false
    }

    fun allSigned(): Boolean {
        return signatures.values.all { it }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentState

        if (id != other.id) return false
        if (documentName != other.documentName) return false
        if (authorizationID != other.authorizationID) return false
        if (issuer != other.issuer) return false
        if (!pdfData.contentEquals(other.pdfData)) return false
        if (participants != other.participants) return false
        if (signatures != other.signatures) return false // Include signatures in equality check
        if (createdAt != other.createdAt) return false
        if (lastUpdated != other.lastUpdated) return false
        if (recordState != other.recordState) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentName.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + pdfData.contentHashCode()
        result = 31 * result + participants.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + recordState.hashCode()
        result = 31 * result + version
        return result
    }
}
