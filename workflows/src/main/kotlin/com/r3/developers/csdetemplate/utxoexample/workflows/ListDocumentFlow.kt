package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.enums.ErrorCode
import com.r3.developers.csdetemplate.utxoexample.enums.Permission
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.DocumentState
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.DocumentMessage
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.GetDocumentListFlowArgs
import com.r3.developers.csdetemplate.utxoexample.workflows.middleware.JWTManager
import com.r3.developers.csdetemplate.utxoexample.workflows.services.proprietiesValues
import com.r3.developers.csdetemplate.utxoexample.workflows.sycKey.AsymmetricKey
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

class ListDocumentFlow : ClientStartableFlow {
    private companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetDocumentListFlowArgs::class.java)
        validateFlowArguments(flowArgs)

        log.info("ListDocumentFlow.call() called")

        val myInfo = memberLookup.myInfo()

        val jwtManager = JWTManager()

        val args = jwtManager.verifyToken(flowArgs.token, myInfo.name.toString())

        if (args.isNullOrEmpty())
            throw CordaRuntimeException(ErrorCode.TOKEN_EXPIRED.message)

        val userstate = ledgerService.findUnconsumedStatesByType(UserState::class.java).firstOrNull { stateAndRef -> stateAndRef.state.contractState.username == myInfo.name}

        if (userstate == null)
            throw CordaRuntimeException(ErrorCode.USER_STATE_NOT_FOUND.message)

        val userState = com.r3.developers.csdetemplate.utxoexample.workflows.services.resolveMessagesFromBackchain(
            userstate,
            ledgerService
        )

        if (!proprietiesValues.isPermissionAllowed(userState.role, Permission.ALLOW_READ_DOCUMENT))
            throw CordaRuntimeException(ErrorCode.OTHER_ERROR.message)

        val states = ledgerService.findUnconsumedStatesByType(DocumentState::class.java)
        val publicKey = myInfo.ledgerKeys.first()
        val pk = AsymmetricKey()
        val results = states
            .filter { it.state.contractState.recordState != RecordState.Deleted }
            .map {
                val participantNames = it.state.contractState.participants.mapNotNull { participant ->
                    memberLookup.lookup(participant)?.name?.toString()
                }
                DocumentMessage(
                    it.state.contractState.id,
                    it.state.contractState.documentName,
                        it.state.contractState.authorizationID,
                    "",
                        it.state.contractState.issuer.toString(),
                    it.state.contractState.hasSigned(publicKey),
                    it.state.contractState.allSigned(),
                    it.state.contractState.createdAt.toString(),
                    it.state.contractState.lastUpdated.toString(),
                    it.state.contractState.recordState,
                    it.state.contractState.version,
                    participantNames
                )
            }

        return jsonMarshallingService.format(results)
    }

    private fun validateFlowArguments(flowArgs: GetDocumentListFlowArgs) {
        if (flowArgs.token.isEmpty()) {
            throw CordaRuntimeException(ErrorCode.FLOW_ARGS_INVALID.message)
        }
    }
}