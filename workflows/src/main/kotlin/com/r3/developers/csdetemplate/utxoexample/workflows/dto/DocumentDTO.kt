package com.r3.developers.csdetemplate.utxoexample.workflows.dto

import com.r3.developers.csdetemplate.utxoexample.enums.RecordState
import net.corda.v5.base.types.MemberX500Name
import java.util.*

data class CreateNewDocumentFlowArgs(val documentName: String, val pdfData: String, val authorizationID: Int, val otherMember: String, val token: String)
data class DeleteDocumentFlowArgs(val id: UUID, val token: String)
data class SignDocumentFlowArgs(val id: UUID, val token: String)

data class GetDocumentFlowArgs(val id: UUID, val numberOfRecords: Int, val token: String)
data class GetDocumentListFlowArgs(val token: String)
data class GetDocumentListByCriteriaFlowArgs(val token: String, val criteria: String)
data class DocumentMessage(
    val id: UUID,
    val documentName: String,
    val authorizationID: Int,
    val documentPDF: String,
    val issuer: String,
    val isSigned: Boolean,
    val isCompleted: Boolean,
    val createdAt: String,
    val lastUpdated: String,
    val recordState: RecordState,
    val version: Int,
    val participants: List<String>
)

data class UpdateDocumentFlowArgs(
    val id: UUID,
    val documentname: String,
    val pdfData: String,
    val token: String
)
