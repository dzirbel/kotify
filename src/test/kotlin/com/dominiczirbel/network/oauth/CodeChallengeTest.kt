package com.dominiczirbel.network.oauth

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.security.SecureRandom

internal class CodeChallengeTest {
    @ParameterizedTest
    @MethodSource("seeds")
    fun testVerifier(seed: String) {
        val random = SecureRandom(seed.toByteArray())
        val challenge = CodeChallenge.generate(random)

        assertThat(challenge.verifier.length).isIn(VERIFIER_MIN_LENGTH..VERIFIER_MAX_LENGTH)
        assertThat(challenge.verifier).doesNotContainMatch("""[^\w\d-]""".toRegex().pattern)
    }

    @RepeatedTest(value = 3)
    fun testChallenge() {
        val random = SecureRandom.getInstance("SHA1PRNG").also { it.setSeed("seed".toByteArray()) }
        val challenge = CodeChallenge.generate(random)

        assertThat(challenge.verifier).isEqualTo("PBn_XUU8eJHtuS_nBmLV5FrvZY6fON-bBIP2ri2N5m4")
        assertThat(challenge.challenge).isEqualTo("sH2tESA7aIPu7pcXufG4P7jp_HWjstu3EFeYDU6yUPA")
    }

    companion object {
        private const val VERIFIER_MIN_LENGTH = 43
        private const val VERIFIER_MAX_LENGTH = 128

        @JvmStatic
        @Suppress("unused")
        fun seeds() = listOf("", "seed", "abc", "123")
    }
}
