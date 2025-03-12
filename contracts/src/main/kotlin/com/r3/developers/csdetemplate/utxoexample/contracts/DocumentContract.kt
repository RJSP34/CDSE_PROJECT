package com.r3.developers.csdetemplate.utxoexample.contracts

import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_ID_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.REQUIRE_SINGLE_COMMAND
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.UNKNOWN_COMMAND
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_PARTICIPANTS_SHOULD_BE_RECORDSTATE_DELETED_AFTER_CHANGE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_PARTICIPANTS_SHOULD_BE_RECORDSTATE_DELETED_BEFORE_CHANGE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_PDF_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.DocumentState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class DocumentContract : Contract {

    class Create: Command

    class Update: Command

    class Delete: Command

    override fun verify(transaction: UtxoLedgerTransaction) {

        val command = transaction.commands.singleOrNull() ?: throw CordaRuntimeException(REQUIRE_SINGLE_COMMAND)

        OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as DocumentState
            output.participants.size== 2
        }

        TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as DocumentState
            transaction.signatories.containsAll(output.participants)
        }

        when(command) {
            is Create -> {
                CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES using (transaction.inputContractStates.isEmpty())
                CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
            }
            is Update -> {
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as DocumentState
                val output = transaction.outputContractStates.single() as DocumentState
                UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.id == output.id)
                UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (
                        input.participants.toSet().intersect(output.participants.toSet()).size == 2)
            }
            is Delete -> {
                DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as DocumentState
                val output = transaction.outputContractStates.single() as DocumentState
                DELETE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.id == output.id)
                DELETE_COMMAND_PDF_SHOULD_NOT_CHANGE using (input.pdfData.contentEquals(output.pdfData))
                DELETE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (
                        input.participants.toSet().intersect(output.participants.toSet()).size == 2)
                DELETE_COMMAND_PARTICIPANTS_SHOULD_BE_RECORDSTATE_DELETED_BEFORE_CHANGE using (input.recordState == RecordState.Active)
                DELETE_COMMAND_PARTICIPANTS_SHOULD_BE_RECORDSTATE_DELETED_AFTER_CHANGE using (output.recordState == RecordState.Deleted)
            }
            else -> {
                throw CordaRuntimeException(UNKNOWN_COMMAND)
            }
        }
    }

    // Helper function to allow writing constraints in the Corda 4 '"text" using (boolean)' style
    private infix fun String.using(expr: Boolean) {
        if (!expr) throw CordaRuntimeException("Failed requirement: $this")
    }

    // Helper function to allow writing constraints in '"text" using {lambda}' style where the last expression
    // in the lambda is a boolean.
    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed requirement: $this")
    }
}