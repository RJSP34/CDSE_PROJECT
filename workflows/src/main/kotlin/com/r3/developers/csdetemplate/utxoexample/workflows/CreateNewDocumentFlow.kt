package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.contracts.DocumentContract
import com.r3.developers.csdetemplate.utxoexample.enums.ErrorCode
import com.r3.developers.csdetemplate.utxoexample.enums.Permission
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.DocumentState
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.CreateNewDocumentFlowArgs
import com.r3.developers.csdetemplate.utxoexample.workflows.middleware.JWTManager
import com.r3.developers.csdetemplate.utxoexample.workflows.services.proprietiesValues
import com.r3.developers.csdetemplate.utxoexample.workflows.services.resolveMessagesFromBackchain
import com.r3.developers.csdetemplate.utxoexample.workflows.sycKey.InternalCriptionClass
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

class CreateNewDocumentFlow : ClientStartableFlow {

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
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("CreateNewChatFlow.call() called")

        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateNewDocumentFlowArgs::class.java)
            validateFlowArguments(flowArgs)

            val myInfo = memberLookup.myInfo()
            val otherMember = memberLookup.lookup(MemberX500Name.parse(flowArgs.otherMember)) ?:
            throw CordaRuntimeException(ErrorCode.CORDA_MEMBER_NOT_FOUND.message)

            val jwtManager = JWTManager()

            val args = jwtManager.verifyToken(flowArgs.token, myInfo.name.toString())

            if (args.isNullOrEmpty())
                throw CordaRuntimeException(ErrorCode.TOKEN_EXPIRED.message)

            val states = ledgerService.findUnconsumedStatesByType(UserState::class.java).firstOrNull { stateAndRef -> stateAndRef.state.contractState.username == myInfo.name}

            if (states == null)
                throw CordaRuntimeException(ErrorCode.USER_STATE_NOT_FOUND.message)

            val userState = resolveMessagesFromBackchain(states, ledgerService)

            if (!proprietiesValues.isPermissionAllowed(userState.role, Permission.ALLOW_CREATE_DOCUMENT))
                throw CordaRuntimeException(ErrorCode.OTHER_ERROR.message)

            val bytes = Base64.getDecoder().decode(flowArgs.pdfData)
            val rsaKeyGenerator = InternalCriptionClass()
            val enPDF = rsaKeyGenerator.encryptDocument(bytes)
            val participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
            val signatures = participants.associateWith { it == myInfo.ledgerKeys.first() }

            val documentState = DocumentState(
                documentName = flowArgs.documentName,
                authorizationID = flowArgs.authorizationID,
                issuer = myInfo.name,
                pdfData = enPDF,
                signatures = signatures,
                participants = participants,
                createdAt = Instant.now(),
                lastUpdated = Instant.now(),
                recordState = RecordState.Active,
                version = 1
            )

            val notary = notaryLookup.notaryServices.single()
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(notary.name)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(documentState)
                .addCommand(DocumentContract.Create())
                .addSignatories(documentState.participants)

            val signedTransaction = txBuilder.toSignedTransaction()
            val transactionId = flowEngine.subFlow(FinalizeDocumentSubFlow(signedTransaction, otherMember.name))
            val jsonResponse = "{\"transactionId\": \"$transactionId\", \"documentId\": \"${documentState.id}\"}"

            return jsonResponse
        }
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw CordaRuntimeException(ErrorCode.OTHER_ERROR.message + e)
        }
    }

    private fun validateFlowArguments(flowArgs: CreateNewDocumentFlowArgs) {
        if (flowArgs.documentName.isEmpty() || flowArgs.pdfData.isEmpty() || flowArgs.otherMember.isEmpty() || flowArgs.token.isEmpty()) {
            throw CordaRuntimeException(ErrorCode.FLOW_ARGS_INVALID.message)
        }
    }
}