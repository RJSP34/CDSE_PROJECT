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
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.constants.ErrorConstants.UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.states.UserState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class UserContract : Contract {

    // Use an internal scoped constant to hold the error messages
    // This allows the tests to use them, meaning if they are updated you won't need to fix tests just because the wording was updated
    // Command Class used to indicate that the transaction should start a new chat.
    class Create: Command
    // Command Class used to indicate that the transaction should append a new ChatState to an existing chat.
    class Update: Command

    class Delete: Command

    // verify() function is used to apply contract rules to the transaction.
    override fun verify(transaction: UtxoLedgerTransaction) {

        // Ensures that there is only one command in the transaction
        val command = transaction.commands.singleOrNull() ?: throw CordaRuntimeException(REQUIRE_SINGLE_COMMAND)

        // Applies a universal constraint (applies to all transactions irrespective of command)
        OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as UserState
            output.participants.size== 1
        }

        TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS using {
            val output = transaction.outputContractStates.first() as UserState
            transaction.signatories.containsAll(output.participants)
        }

        // Switches case based on the command
        when(command) {
            // Rules applied only to transactions with the Create Command.
            is Create -> {
                CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES using (transaction.inputContractStates.isEmpty())
                CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)
            }
            // Rules applied only to transactions with the Update Command.
            is Update -> {
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as UserState
                val output = transaction.outputContractStates.single() as UserState
                UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.id == output.id)
                UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (
                        input.participants.toSet().intersect(output.participants.toSet()).size == 1)
            }
            is Delete -> {
                DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE using (transaction.inputContractStates.size == 1)
                DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE using (transaction.outputContractStates.size == 1)

                val input = transaction.inputContractStates.single() as UserState
                val output = transaction.outputContractStates.single() as UserState
                DELETE_COMMAND_ID_SHOULD_NOT_CHANGE using (input.id == output.id)
                DELETE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE using (
                        input.participants.toSet().intersect(output.participants.toSet()).size == 1)
                DELETE_COMMAND_PARTICIPANTS_SHOULD_BE_RECORDSTATE_DELETED_BEFORE_CHANGE using (input.recordState == RecordState.Active)
                DELETE_COMMAND_PARTICIPANTS_SHOULD_BE_RECORDSTATE_DELETED_AFTER_CHANGE using (output.recordState == RecordState.Deleted)
            }
            else -> {
                throw CordaRuntimeException(UNKNOWN_COMMAND)
            }
        }
    }

    private infix fun String.using(expr: Boolean) {
        if (!expr) throw CordaRuntimeException("Failed requirement: $this")
    }

    private infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw CordaRuntimeException("Failed requirement: $this")
    }
}