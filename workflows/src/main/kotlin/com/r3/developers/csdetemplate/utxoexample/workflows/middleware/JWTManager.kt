package com.r3.developers.csdetemplate.utxoexample.workflows.middleware

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.r3.developers.csdetemplate.utxoexample.workflows.services.readEnvFile
import java.util.*

class JWTManager {
    private val issuer: String
    private val validityMinutes: Long
    private val subject: String
    private val secretKey: String
    private val memberKey: String

    init {
        val env = readEnvFile("../.env") // Specify the correct path to your .env file
        issuer = env["ISSUER"] ?: "Corda"
        validityMinutes = env["VALIDITY_MINUTES"]?.toLong() ?: 10000
        subject = env["SUBJECT"] ?: "JWT"
        secretKey = env["SECRET_KEY"] ?: "password"
        memberKey = env["MEMBER_KEY"] ?: "memberName"
    }

    fun createToken(additionalParams: Map<String, Any>): String {
        val algorithm = Algorithm.HMAC256(secretKey)
        val issuedAt = Date()
        val expiresAt = Date(issuedAt.time + validityMinutes * 60 * 1000)

        val builder = JWT.create()
            .withIssuer(issuer)
            .withSubject(subject)
            .withIssuedAt(issuedAt)
            .withExpiresAt(expiresAt)

        additionalParams.forEach { (key, value) ->
            builder.withClaim(key, value.toString())
        }

        return builder.sign(algorithm)
    }

    fun verifyToken(token: String, expectedName: String): Map<String, Any>? {
        try {
            val algorithm = Algorithm.HMAC256(secretKey)
            val verifier: JWTVerifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
            val jwt = verifier.verify(token)

            val additionalParams = mutableMapOf<String, Any>()
            jwt.claims.forEach { (key, value) ->
                additionalParams[key] = value?.asString() ?: ""
            }

            return if (additionalParams.containsKey(memberKey) && additionalParams[memberKey] != null &&  additionalParams[memberKey] == expectedName) {
                additionalParams

            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }

}
