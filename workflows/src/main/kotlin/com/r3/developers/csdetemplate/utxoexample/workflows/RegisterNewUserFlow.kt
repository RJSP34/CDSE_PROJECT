package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.contracts.UserContract
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.CreateUserFlowArgs
import com.r3.developers.csdetemplate.utxoexample.workflows.sycKey.AsymmetricKey
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class RegisterNewUserFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var persistentService: PersistenceService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("CreateNewUserFlow.call() called")

        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateUserFlowArgs::class.java)
            validateFlowArguments(flowArgs)

            val myInfo = memberLookup.myInfo()

            val states = ledgerService.findUnconsumedStatesByType(UserState::class.java).filter { it.state.contractState.username == myInfo.name}

            if (states.isNotEmpty())
                throw CordaRuntimeException("Already Registered.")

            val asymmetricKey = AsymmetricKey()

            val userState = UserState(
                username = myInfo.name,
                publicKey = asymmetricKey.getPublicKeyAsString(),
                privateKey = asymmetricKey.getPrivateKeyAsString(),
                loginName = flowArgs.loginName,
                password = flowArgs.password,
                role = flowArgs.role,
                createdAt = Instant.now(),
                lastUpdated = Instant.now(),
                recordState = RecordState.Active,
                version = 1,
                participants = listOf(myInfo.ledgerKeys.first()),
                )

            val notary = notaryLookup.notaryServices.single()

            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(notary.name)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(userState)
                .addCommand(UserContract.Create())
                .addSignatories(myInfo.ledgerKeys.first())

            val signedTransaction = txBuilder.toSignedTransaction()

            val transactionId = flowEngine.subFlow(FinalizeUserFlow(signedTransaction, myInfo.name))
            val jsonObject = JSONObject().apply {
                put("TransactionID", transactionId)
                put("Public", asymmetricKey.getPublicKeyAsString())
                put("Private", asymmetricKey.getPrivateKeyAsString())
            }

            return jsonObject.toString()
        }
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }

    private fun validateFlowArguments(flowArgs: CreateUserFlowArgs) {
        if (flowArgs.loginName.isEmpty() || flowArgs.password.isEmpty()) {
            throw CordaRuntimeException("One or more flow arguments are empty.")
        }
    }
}