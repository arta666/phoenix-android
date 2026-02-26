package com.phoenix.client.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GeneratedKeyPair(
    /** File name (relative to filesDir) where the private key PEM was written. */
    val privateKeyFile: String,
    /** Base64-encoded Ed25519 public key to paste into the server's authorized_clients. */
    val publicKey: String,
)

/**
 * Generates an Ed25519 keypair by running the bundled Go binary with `-gen-keys`.
 * Each config gets its own key file named `client-{configId}.private.key` so that
 * generating a new key for one config never overwrites another config's key.
 */
object KeyManager {

    /**
     * Generates a keypair for [configId] and writes it to
     * `filesDir/client-{configId}.private.key`.
     *
     * Parses structured stdout lines:
     *   KEY_PATH=/data/.../files/client-<id>.private.key
     *   PUBLIC_KEY=<base64>
     *
     * @throws IllegalStateException if the process fails or output is malformed.
     */
    suspend fun generateKeys(context: Context, configId: String): GeneratedKeyPair =
        withContext(Dispatchers.IO) {
            val binary = BinaryExtractor.extract(context)
            val keyFileName = keyFileNameFor(configId)

            val process = ProcessBuilder(
                binary.absolutePath,
                "-gen-keys",
                "-files-dir", context.filesDir.absolutePath,
                "-key-name", keyFileName,
            )
                .redirectErrorStream(false)
                .start()

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode != 0) {
                throw IllegalStateException("Key generation failed (exit $exitCode): $stderr")
            }

            val publicKey = stdout.lines()
                .firstOrNull { it.startsWith("PUBLIC_KEY=") }
                ?.removePrefix("PUBLIC_KEY=")
                ?.trim()
                ?: throw IllegalStateException("PUBLIC_KEY not found in binary output:\n$stdout")

            GeneratedKeyPair(
                privateKeyFile = keyFileName,
                publicKey = publicKey,
            )
        }

    /** Returns the per-config private key filename for [configId]. */
    fun keyFileNameFor(configId: String): String = "client-$configId.private.key"
}
