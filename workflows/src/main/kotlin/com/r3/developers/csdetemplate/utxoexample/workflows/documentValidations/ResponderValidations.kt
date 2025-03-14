package com.r3.developers.csdetemplate.utxoexample.workflows.documentValidations

import com.r3.developers.csdetemplate.utxoexample.states.DocumentState
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name

// Note, these exceptions will only be visible in the logs if Corda logging is set to debug.

// Checks that the message does not contain banned words and throws and exception if it does.
@Suspendable
fun checkForBannedWords(str: String) {
    val bannedWords = listOf("banana", "apple", "pear")
    if (bannedWords.any { str.contains(it) })
        throw CordaRuntimeException("Failed verification - message contains banned words")
}

// Checks that the messageFrom field in the ChatState matches the initiators (otherMember)
// memberX500Name, if not it throws an exception.
