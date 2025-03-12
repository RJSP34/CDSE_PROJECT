package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.enums.ErrorCode
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.GetTokenFlowArgs
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.GetTokenMessage
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GetTokenFlow : ClientStartableFlow {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
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

        log.info("GetUserFlow.call() called")
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetTokenFlowArgs::class.java)
        validateFlowArguments(flowArgs)

        val token: String
        val myInfo = memberLookup.myInfo()

        val states = ledgerService.findUnconsumedStatesByType(UserState::class.java).firstOrNull { stateAndRef -> stateAndRef.state.contractState.username == myInfo.name}

        if (states == null)
            throw CordaRuntimeException("Not Registered.")

        val userState = resolveMessagesFromBackchain(states, ledgerService)

        if (userState.loginName != flowArgs.loginName || userState.password != flowArgs.password)
            throw CordaRuntimeException("Login or password don't match.")

        val list: Map<String, Any> = emptyMap<String, Any>() + ("memberName" to myInfo.name) + ("role" to userState.role)

        val jwtManager = JWTManager()

        token = jwtManager.createToken(list)

        val tokenMessage = GetTokenMessage(token)

        return jsonMarshallingService.format(tokenMessage)
    }

    private fun validateFlowArguments(flowArgs: GetTokenFlowArgs) {
        if (flowArgs.loginName.isEmpty() || flowArgs.password.isEmpty()) {
            throw CordaRuntimeException(ErrorCode.FLOW_ARGS_INVALID.message)
        }
    }
}