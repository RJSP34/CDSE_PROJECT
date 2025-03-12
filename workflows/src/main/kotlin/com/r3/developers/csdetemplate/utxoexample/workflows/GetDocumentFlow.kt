package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.enums.ErrorCode
import com.r3.developers.csdetemplate.utxoexample.enums.Permission
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.DocumentState
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.DocumentMessage
import com.r3.developers.csdetemplate.utxoexample.workflows.dto.GetDocumentFlowArgs
import com.r3.developers.csdetemplate.utxoexample.workflows.middleware.JWTManager
import com.r3.developers.csdetemplate.utxoexample.workflows.services.proprietiesValues
import com.r3.developers.csdetemplate.utxoexample.workflows.sycKey.AsymmetricKey
import com.r3.developers.csdetemplate.utxoexample.workflows.sycKey.InternalCriptionClass
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.util.*

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class GetDocumentFlow : ClientStartableFlow {

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

        log.info("GetChatFlow.call() called")

        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetDocumentFlowArgs::class.java)
        validateFlowArguments(flowArgs)

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
        val state = states.singleOrNull { it.state.contractState.id == flowArgs.id }
            ?: throw CordaRuntimeException("Did not find an unique unconsumed DocumentState with id ${flowArgs.id}")

        return jsonMarshallingService.format(resolveMessagesFromBackchain(state, flowArgs.numberOfRecords, myInfo.ledgerKeys.first(), userState))
    }

    @Suspendable
    private fun resolveMessagesFromBackchain(
        stateAndRef: StateAndRef<DocumentState>,
        numberOfRecords: Int,
        publicKey: PublicKey,
        userState: UserState
    ): List<DocumentMessage> {
        val messages = mutableListOf<DocumentMessage>()
        var currentStateAndRef = stateAndRef
        var recordsToFetch = numberOfRecords
        var moreBackchain = true

        while (moreBackchain) {
            val transactionId = currentStateAndRef.ref.transactionId
            val transaction = ledgerService.findLedgerTransaction(transactionId)
                ?: throw CordaRuntimeException("Transaction $transactionId not found.")

            val output = transaction.getOutputStates(DocumentState::class.java).singleOrNull()
                ?: throw CordaRuntimeException("Expecting one and only one DocumentState output for transaction $transactionId.")
            val pk = AsymmetricKey()
            val public = pk.getPublicKey(userState.publicKey)

            if (output.recordState != RecordState.Deleted) {
                val bytes = output.pdfData
                val rsaKeyGenerator = InternalCriptionClass()
                val debytes = rsaKeyGenerator.decryptDocument(bytes)
                val encriptedBytes = pk.encrypt(debytes, publicKey = public)
                val encryptedBase64String = Base64.getEncoder().encodeToString(encriptedBytes)
                val participantNames = output.participants.mapNotNull { participant ->
                    memberLookup.lookup(participant)?.name?.toString()
                }
                messages.add(
                    DocumentMessage(
                        output.id,
                        output.documentName,
                        output.authorizationID,
                        encryptedBase64String,
                        output.issuer.toString(),
                        output.hasSigned(publicKey),
                        output.allSigned(),
                        output.createdAt.toString(),
                        output.lastUpdated.toString(),
                        output.recordState,
                        output.version,
                        participantNames
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
                currentStateAndRef = inputStateAndRefs.single() as StateAndRef<DocumentState>
            }
        }
        return messages.toList()
    }

    private fun validateFlowArguments(flowArgs: GetDocumentFlowArgs) {
        if (flowArgs.numberOfRecords <= 0 || flowArgs.token.isEmpty()) {
            throw CordaRuntimeException(ErrorCode.FLOW_ARGS_INVALID.message)
        }
    }
}