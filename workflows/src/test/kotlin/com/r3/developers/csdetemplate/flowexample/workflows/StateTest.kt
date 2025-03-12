package com.r3.developers.csdetemplate.flowexample.workflows

import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test


//Note: this simulator test has been commented out pending the merging of the UTXO code into the Gecko Branch.
class StateTest {

    // Names picked to match the corda network in config/static-network-config.json
    private val aliceX500 = MemberX500Name.parse("CN=Alice, OU=Test Dept, O=R3, L=London, C=GB")
    private val bobX500 = MemberX500Name.parse("CN=Bob, OU=Test Dept, O=R3, L=London, C=GB")

    @Test
    fun `test that MyFirstFLow returns correct message`() {

    }
}
