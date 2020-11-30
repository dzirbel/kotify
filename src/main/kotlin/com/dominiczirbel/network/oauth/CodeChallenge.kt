package com.dominiczirbel.network.oauth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Encapsulates a code verifier and challenge for the Authorization Code Flow with Proof Key for Code Exchange (PKCE).
 *
 * This approach, described in [RFC-7636](https://tools.ietf.org/html/rfc7636), uses a secret, random [verifier] to
 * obtain an authorization code; and a derived [challenge] to verify subsequent requests for an access token.
 *
 * The [verifier] is a cryptographically random string between 43 and 128 characters in length, using the base64url
 * alphabet (letters, digits, hyphens, and underscores).
 *
 * The [challenge] is then generated deterministically from the [verifier] as the SHA-256 hash of the [verifier]'s ASCII
 * representation, then itself encoded in base64url.
 */
class CodeChallenge private constructor(val verifier: String, val challenge: String) {
    companion object {
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        // number of bytes in the verifier buffer; 32 bytes -> 43 characters
        private const val VERIFIER_BUFFER_SIZE = 32

        private fun generateVerifier(random: SecureRandom): String {
            val buffer = ByteArray(VERIFIER_BUFFER_SIZE)
            random.nextBytes(buffer)
            return encoder.encodeToString(buffer)
        }

        private fun generateChallenge(verifier: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(verifier.toByteArray(Charsets.US_ASCII))
            return encoder.encodeToString(md.digest())
        }

        /**
         * Generates a new [CodeChallenge] from the given [random] seed.
         */
        fun generate(random: SecureRandom = SecureRandom()): CodeChallenge {
            val verifier = generateVerifier(random)
            val challenge = generateChallenge(verifier)
            return CodeChallenge(verifier = verifier, challenge = challenge)
        }
    }
}
