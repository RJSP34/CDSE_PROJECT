package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.enums.ErrorCode
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.GetUserFlowArgs
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.GetUserFlowMessage
import com.r3.developers.csdetemplate.utxoexample.workflows.middleware.JWTManager
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory

class GetUserFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup
    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("GetUserFlow.call() called")

        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetUserFlowArgs::class.java)
        validateFlowArguments(flowArgs)

        val myInfo = memberLookup.myInfo()

        val jwtManager = JWTManager()

        val args = jwtManager.verifyToken(flowArgs.token, myInfo.name.toString())

        if (args.isNullOrEmpty())
            throw CordaRuntimeException(ErrorCode.TOKEN_EXPIRED.message)

        val userstate = ledgerService.findUnconsumedStatesByType(UserState::class.java).firstOrNull { stateAndRef -> stateAndRef.state.contractState.username == myInfo.name}

        if (userstate == null)
            throw CordaRuntimeException(ErrorCode.USER_STATE_NOT_FOUND.message)

        return jsonMarshallingService.format(resolveMessagesFromBackchain(userstate, flowArgs.numberOfRecords))
    }

    @Suspendable
    private fun resolveMessagesFromBackchain(
        stateAndRef: StateAndRef<UserState>,
        numberOfRecords: Int
    ): List<GetUserFlowMessage> {
        val messages = mutableListOf<GetUserFlowMessage>()
        var currentStateAndRef = stateAndRef
        var recordsToFetch = numberOfRecords
        var moreBackchain = true

        while (moreBackchain) {
            val transactionId = currentStateAndRef.ref.transactionId
            val transaction = ledgerService.findLedgerTransaction(transactionId)
                ?: throw CordaRuntimeException(ErrorCode.TRANSACTION_NOT_FOUND.message)

            val output = transaction.getOutputStates(UserState::class.java).singleOrNull()
                ?: throw CordaRuntimeException(ErrorCode.EXPECTING_ONE_DOCUMENT_STATE.message)

            // Check if the record state is not "DELETED" before adding it to messages.
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
            recordsToFetch--

            val inputStateAndRefs = transaction.inputStateAndRefs

            if (inputStateAndRefs.isEmpty() || recordsToFetch == 0) {
                moreBackchain = false
            } else if (inputStateAndRefs.size > 1) {
                throw CordaRuntimeException(ErrorCode.MULTIPLE_ZERO_CHAT_STATES.message)
            } else {
                @Suppress("UNCHECKED_CAST")
                currentStateAndRef = inputStateAndRefs.single() as StateAndRef<UserState>
            }
        }
        return messages.toList()
    }

    private fun validateFlowArguments(flowArgs: GetUserFlowArgs) {
        if (flowArgs.numberOfRecords <= 0) {
            throw CordaRuntimeException(ErrorCode.FLOW_ARGS_INVALID.message)
        }
    }
}