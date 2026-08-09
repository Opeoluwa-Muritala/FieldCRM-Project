package com.fieldcrm.android.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MobileUserDirectoryDecoderTest {
    @Test
    fun decodesRawUserArray() {
        val payload = Json.parseToJsonElement(
            """[{"id":"u1","full_name":"Ada Okafor","email":"ada@example.com","role":"system_admin","active":true}]"""
        )

        val users = decodeMobileUserDirectory(payload)

        assertNotNull(users)
        assertEquals(1, users?.size)
        assertEquals("Ada Okafor", users?.single()?.full_name)
    }

    @Test
    fun decodesUsersEnvelopeAndDeployedAliases() {
        val payload = Json.parseToJsonElement(
            """{"data":{"users":[{"user_id":"u2","name":"David Musa","email":"david@example.com","db_role":"credit_analyst","role_label":"Credit Analyst","is_active":false,"last_login_at":"2026-08-08T14:32:10Z"}]}}"""
        )

        val user = decodeMobileUserDirectory(payload)?.single()

        assertNotNull(user)
        assertEquals("Credit Analyst", user?.display_role)
        assertEquals(false, user?.active)
        assertEquals("2026-08-08T14:32:10Z", user?.last_activity_at)
    }

    @Test
    fun rejectsUnknownDirectoryShape() {
        val payload = Json.parseToJsonElement("""{"count":4}""")

        assertEquals(null, decodeMobileUserDirectory(payload))
    }

    @Test
    fun decodesTypedBranchEnvelopeInDataLayer() {
        val payload = Json.parseToJsonElement(
            """{"data":{"branches":[{"id":"b1","name":"Lagos Main","code":"LAG","active":true}]}}"""
        )

        val branch = decodeMobileBranchDirectory(payload)?.single()

        assertNotNull(branch)
        assertEquals("Lagos Main", branch?.name)
        assertEquals("LAG", branch?.code)
    }
}
