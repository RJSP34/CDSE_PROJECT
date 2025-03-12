package com.r3.developers.csdetemplate.utxoexample.workflows.services

import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.GetUserFlowMessage
import com.r3.developers.csdetemplate.utxoexample.workflows.sycKey.InternalCriptionClass
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.io.File
import java.io.IOException
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec


@Suspendable
fun resolveMessagesFromBackchain(
    stateAndRef: StateAndRef<UserState>,
    ledgerService: UtxoLedgerService
): UserState {
    val messages = mutableListOf<GetUserFlowMessage>()

    val transactionId = stateAndRef.ref.transactionId
    val transaction = ledgerService.findLedgerTransaction(transactionId)
        ?: throw CordaRuntimeException("Transaction $transactionId not found.")

    val output = transaction.getOutputStates(UserState::class.java).singleOrNull()
        ?: throw CordaRuntimeException("Expecting one and only one DocumentState output for transaction $transactionId.")

    if (output.recordState != RecordState.Deleted) {
        messages.add(
            GetUserFlowMessage(
                output.id,
                output.username,
                output.publicKey,
                output.createdAt.toString(),
                output.lastUpdated.toString(),
                output.recordState,
                output.version
            )
        )
    }

    return output
}

@Suspendable
fun readEnvFile(filePath: String): Map<String, String> {
    val envVars = mutableMapOf<String, String>()
    val file = File(filePath)

    try {
        if (file.exists()) {
            file.forEachLine { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                    val parts = trimmedLine.split("=", limit = 2)
                    if (parts.size == 2) {
                        envVars[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        } else {
            throw IOException("File not found: $filePath")
        }
    } catch (e: IOException) {
        println("Error reading .env file: ${e.message}")
    } catch (e: Exception) {
        println("An unexpected error occurred: ${e.message}")
    }

    return envVars
}