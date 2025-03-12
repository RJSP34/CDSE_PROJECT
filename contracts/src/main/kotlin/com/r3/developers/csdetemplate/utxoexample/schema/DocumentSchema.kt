package com.r3.developers.csdetemplate.utxoexample.schema

import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import net.corda.v5.base.annotations.CordaSerializable
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "documentState")
@CordaSerializable
class DocumentSchema(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID ,
    @Column(name = "documentName")
    val documentName: String,
    @Column(name = "authorizationID")
    val authorizationID: Int,
    @Column(name = "issuer")
    val issuer: String,
    @Column(name = "pdfData")
    val pdfData: ByteArray,
    @Column(name = "createdAt")
    val createdAt: Instant,
    @Column(name = "lastUpdated")
    val lastUpdated: Instant,
    @Column(name = "recordState")
    val recordState: RecordState,
    @Column(name = "version")
    var version: Int
) {
    // Default constructor required by hibernate.
    constructor(
        documentName: String,
        authorizationID: Int,
        issuer: String,
        pdfData: ByteArray,
        createdAt: Instant,
        lastUpdated: Instant,
        recordState: RecordState,
        version: Int
    ) : this(
        UUID.randomUUID(),
        documentName,
        authorizationID,
        issuer,
        pdfData,
        createdAt,
        lastUpdated,
        recordState,
        version
    )
    constructor() : this(UUID.randomUUID(), "", 0, "", ByteArray(0), Instant.now(), Instant.now(), RecordState.Active, 0)
}