package com.r3.developers.csdetemplate.utxoexample.workflows.dto

import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import com.r3.developers.csdetemplate.utxoexample.enums.Role
import net.corda.v5.base.types.MemberX500Name
import java.time.Instant
import java.util.*

data class GetUserFlowArgs(val numberOfRecords: Int, val token: String)
// A class to pair the messageFrom and message together.
data class GetUserFlowMessage(
    val id: UUID = UUID.randomUUID(),
    val username: MemberX500Name,
    val publicKey: String,
    val createdAt: String,
    val lastUpdated: String,
    val recordState: RecordState,
    var version: Int
)
data class GetTokenMessage(
    val token: String
)

data class CreateUserFlowArgs(
    val loginName: String,
    val password: String,
    val role: Role = Role.None
)

data class GetTokenFlowArgs(
    val loginName: String,
    val password: String
)
