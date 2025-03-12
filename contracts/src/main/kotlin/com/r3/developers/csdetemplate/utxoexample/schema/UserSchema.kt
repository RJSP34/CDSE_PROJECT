package com.r3.developers.csdetemplate.utxoexample.schema

import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import net.corda.v5.base.annotations.CordaSerializable
import java.time.Instant
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "userState")
@CordaSerializable
class UserSchema (
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,

    @Column(name = "username")
    val username: String,

    @Column(name = "publicKey")
    val publicKey: String,

    @Column(name = "loginName")
    val loginName: String,

    @Column(name = "password")
    val password: String,

    @Column(name = "role")
    val role: String,

    @Column(name = "createdAt")
    val createdAt: Instant,

    @Column(name = "lastUpdated")
    val lastUpdated: Instant,

    @Column(name = "recordState")
    val recordState: RecordState,

    @Column(name = "version")
    var version: Int,
) {
    constructor(
        username: String,
        publicKey: String,
        loginName: String,
        password: String,
        role: String,
        createdAt: Instant,
        lastUpdated: Instant,
        recordState: RecordState,
        version: Int
    ) : this(UUID.randomUUID(),username, publicKey, loginName, password, role, createdAt, lastUpdated, recordState, version)

    constructor() : this(
        UUID.randomUUID(),
        "",
        "",
        "",
        "",
        "",
        Instant.now(),
        Instant.now(),
        RecordState.Active,
        0
    )
}