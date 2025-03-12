package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.enums.ErrorCode
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.GetDocumentListFlowArgs
import com.r3.developers.csdetemplate.utxoexample.workflows.middleware.JWTManager
import com.r3.developers.csdetemplate.utxoexample.workflows.services.resolveMessagesFromBackchain
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.json.JSONObject
import org.slf4j.LoggerFactory

class GetUserPrivateKeyFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup
    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetDocumentListFlowArgs::class.java)
            validateFlowArguments(flowArgs)

            val myInfo = memberLookup.myInfo()

            val jwtManager = JWTManager()

            val args = jwtManager.verifyToken(flowArgs.token, myInfo.name.toString())

            if (args.isNullOrEmpty())
                throw CordaRuntimeException(ErrorCode.TOKEN_EXPIRED.message)

            val states = ledgerService.findUnconsumedStatesByType(UserState::class.java).firstOrNull { stateAndRef -> stateAndRef.state.contractState.username == myInfo.name}

            if (states == null)
                throw CordaRuntimeException(ErrorCode.USER_STATE_NOT_FOUND.message)

            val userState = resolveMessagesFromBackchain(states, ledgerService)

            val jsonObject = JSONObject().apply {
                put("ID", userState.id)
                put("Public", userState.publicKey)
                put("Private", userState.privateKey)
            }

            return jsonObject.toString()
        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }

    private fun validateFlowArguments(flowArgs: GetDocumentListFlowArgs) {
        if (flowArgs.token.isEmpty()) {
            throw CordaRuntimeException(ErrorCode.FLOW_ARGS_INVALID.message)
        }
    }
}