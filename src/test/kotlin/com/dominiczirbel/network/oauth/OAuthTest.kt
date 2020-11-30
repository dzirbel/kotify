package com.dominiczirbel.network.oauth

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.security.SecureRandom

internal class OAuthTest {
    @Test
    fun testGenerateState() {
        val random1 = SecureRandom.getInstance("SHA1PRNG").also { it.setSeed("seed1".toByteArray()) }
        assertThat(OAuth.generateState(random1)).isEqualTo("l3OSTTk8nhG-Gz9Jsl4miw")

        val random2 = SecureRandom.getInstance("SHA1PRNG").also { it.setSeed("seed2".toByteArray()) }
        assertThat(OAuth.generateState(random2)).isEqualTo("k8rfUGHlkx59Ya-byswyyg")
    }

    @Test
    fun testAuthorizationUri() {
        val random = SecureRandom.getInstance("SHA1PRNG").also { it.setSeed("seed".toByteArray()) }
        val url = OAuth.authorizationUrl(
            clientId = "myClientId",
            scopes = listOf("scope1", "another_scope"),
            redirectUri = "http://example.com/my/redirect",
            codeChallenge = CodeChallenge.generate(random),
            state = "my_state"
        )

        assertThat(url.toString()).isEqualTo(
            "https://accounts.spotify.com/authorize?" +
                "client_id=myClientId&" +
                "response_type=code&" +
                "redirect_uri=http%3A%2F%2Fexample.com%2Fmy%2Fredirect&" +
                "code_challenge_method=S256&" +
                "code_challenge=sH2tESA7aIPu7pcXufG4P7jp_HWjstu3EFeYDU6yUPA&" +
                "state=my_state&" +
                "scope=scope1%20another_scope"
        )

        assertThat(url.toUri()).isNotNull()
    }
}
