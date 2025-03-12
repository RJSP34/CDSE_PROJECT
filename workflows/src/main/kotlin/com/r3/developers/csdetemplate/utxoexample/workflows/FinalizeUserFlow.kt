package com.r3.developers.csdetemplate.utxoexample.workflows

import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "finalize-user-protocol")
class FinalizeUserFlow(private val signedTransaction: UtxoSignedTransaction, private val otherMember: MemberX500Name) :
    SubFlow<String> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): String {
        log.info("FinalizeUserFlow.call() called")

        val session = flowMessaging.initiateFlow(otherMember)

        return try {
            checkSessionActive(session)

            val finalizedSignedTransaction = ledgerService.finalize(
                signedTransaction,
                listOf(session)
            )

            finalizedSignedTransaction.transaction.id.toString().also {
                log.info("Success! Transaction ID: $it")
            }
        } catch (e: Exception) {
            log.warn("Finality failed", e)
            "Finality failed, ${e.message}"
        }
    }

    private fun checkSessionActive(session: FlowSession) {
        if (session.counterparty == null) {
            throw CordaRuntimeException("Session with ${otherMember.commonName} is not active or already closed.")
        }
    }
}
