package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.contracts.DocumentContract
import com.r3.developers.csdetemplate.utxoexample.enums.ErrorCode
import com.r3.developers.csdetemplate.utxoexample.enums.Permission
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.DocumentState
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.SignDocumentFlowArgs
import com.r3.developers.csdetemplate.utxoexample.workflows.middleware.JWTManager
import com.r3.developers.csdetemplate.utxoexample.workflows.services.proprietiesValues
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class SignatureFlow: ClientStartableFlow {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("UpdateNewChatFlow.call() called")

        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, SignDocumentFlowArgs::class.java)
            validateInputArguments(flowArgs)

            val myInfo = memberLookup.myInfo()

            val jwtManager = JWTManager()

            val args = jwtManager.verifyToken(flowArgs.token, myInfo.name.toString())

            if (args.isNullOrEmpty())
                throw CordaRuntimeException(ErrorCode.TOKEN_EXPIRED.message)

            val Userstate = ledgerService.findUnconsumedStatesByType(UserState::class.java).firstOrNull { stateAndRef -> stateAndRef.state.contractState.username == myInfo.name}

            if (Userstate == null)
                throw CordaRuntimeException(ErrorCode.USER_STATE_NOT_FOUND.message)

            val userState = com.r3.developers.csdetemplate.utxoexample.workflows.services.resolveMessagesFromBackchain(
                Userstate,
                ledgerService
            )

            if (!proprietiesValues.isPermissionAllowed(userState.role, Permission.ALLOW_SIGNATURE))
                throw CordaRuntimeException(ErrorCode.OTHER_ERROR.message)

            val stateAndRef = ledgerService.findUnconsumedStatesByType(DocumentState::class.java).singleOrNull {
                it.state.contractState.id == flowArgs.id
            } ?: throw CordaRuntimeException("Multiple or zero states with id ${flowArgs.id} found.")

            val state = stateAndRef.state.contractState

            if (state.recordState != RecordState.Active)
                throw CordaRuntimeException("Document doesnt exist.")

            val members = state.participants.map {
                memberLookup.lookup(it) ?: throw CordaRuntimeException("Member not found from public key $it.")}
            val otherMember = (members - myInfo).singleOrNull()
                ?: throw CordaRuntimeException("Should be only one participant other than the initiator.")

            val newDocumentState = state.updateSignature(
                publicKey = myInfo.ledgerKeys.first()
            )

            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(stateAndRef.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(newDocumentState)
                .addInputState(stateAndRef.ref)
                .addCommand(DocumentContract.Update())
                .addSignatories(newDocumentState.participants)

            val signedTransaction = txBuilder.toSignedTransaction()
            return flowEngine.subFlow(FinalizeDocumentSubFlow(signedTransaction, otherMember.name))
        }
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
    private fun validateInputArguments(flowArgs: SignDocumentFlowArgs) {
        require(flowArgs.token.isNotEmpty()) { "Token must not be empty." }
    }
}
