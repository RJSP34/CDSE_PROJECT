package com.r3.developers.csdetemplate.utxoexample.workflows

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory


@InitiatingFlow(protocol = "finalize-document-protocol")
class FinalizeDocumentSubFlow(private val signedTransaction: UtxoSignedTransaction, private val otherMember: MemberX500Name): SubFlow<String> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): String {

        log.info("FinalizeDocumentFlow.call() called")

        // Initiates a session with the other Member.
        val session = flowMessaging.initiateFlow(otherMember)

        return try {
            val finalizedSignedTransaction = ledgerService.finalize(
                signedTransaction,
                listOf(session)
            )

            finalizedSignedTransaction.transaction.id.toString().also {
                log.info("Success! Response: $it")
            }
        }
        // Soft fails the flow and returns the error message without throwing a flow exception.
        catch (e: Exception) {
            log.warn("Finality failed", e)
            "Finality failed, ${e.message}"
        }
    }
}
