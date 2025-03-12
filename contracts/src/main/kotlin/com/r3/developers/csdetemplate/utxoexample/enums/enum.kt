package com.r3.developers.csdetemplate.utxoexample.enums

import net.corda.v5.base.annotations.CordaSerializable
@CordaSerializable
enum class RecordState {
        Active,
        Deleted
}
@CordaSerializable
enum class Role {
        MedicalAuthority,
        Patient,
        None
}
@CordaSerializable
enum class ErrorCode(val message: String) {
        TOKEN_EXPIRED("Token expired"),
        FLOW_ARGS_INVALID("One or more flow arguments are empty."),
        USER_STATE_NOT_FOUND("USERSTATE doesnt exist."),
        INITIATOR_ONE_PARTICIPANT("Should be only one participant other than the initiator."),
        EXPECTING_ONE_DOCUMENT_STATE("Expecting one and only one DocumentState output for transaction."),
        TRANSACTION_NOT_FOUND("Transaction not found."),
        MULTIPLE_ZERO_CHAT_STATES("Multiple or zero Chat states found."),
        CORDA_MEMBER_NOT_FOUND("MemberLookup can't find otherMember specified in flow arguments."),
        OTHER_ERROR("An error occurred")
}

@CordaSerializable
enum class Permission {
        ALLOW_CREATE_DOCUMENT,
        ALLOW_SIGNATURE,
        ALLOW_READ_DOCUMENT,
        ALLOW_UPDATE_DOCUMENT,
        ALLOW_DELETE_DOCUMENT
}