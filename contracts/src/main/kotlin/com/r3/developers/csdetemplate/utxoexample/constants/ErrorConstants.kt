package com.r3.developers.csdetemplate.utxoexample.constants

object ErrorConstants {
    const val REQUIRE_SINGLE_COMMAND = "Requires a single command."
    const val UNKNOWN_COMMAND = "Command not allowed."
    const val OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS = "The output state should have two and only two participants."
    const val TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS = "The transaction should have been signed by both participants."

    const val CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES = "When command is Create there should be no input states."
    const val CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE =  "When command is Create there should be one and only one output state."

    const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Update there should be one and only one input state."
    const val UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Update there should be one and only one output state."
    const val UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Update id must not change."
    const val UPDATE_COMMAND_PDF_SHOULD_NOT_CHANGE = "When command is Update PDF must not change."
    const val UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Update participants must not change."
    const val DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE = "When command is Delete there should be one and only one input state."
    const val DELETE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE = "When command is Delete there should be one and only one output state."
    const val DELETE_COMMAND_ID_SHOULD_NOT_CHANGE = "When command is Delete id must not change."
    const val DELETE_COMMAND_PDF_SHOULD_NOT_CHANGE = "When command is Delete PDF must not change."
    const val DELETE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE = "When command is Delete participants must not change."
    const val DELETE_COMMAND_PARTICIPANTS_SHOULD_BE_RECORDSTATE_DELETED_BEFORE_CHANGE = "When command is Delete Before Change RecordState is Active."
    const val DELETE_COMMAND_PARTICIPANTS_SHOULD_BE_RECORDSTATE_DELETED_AFTER_CHANGE = "When command is Delete AFTER Change RecordState is DELETED."}