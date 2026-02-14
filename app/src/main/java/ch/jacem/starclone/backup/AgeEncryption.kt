package ch.jacem.starclone.backup

import com.exceptionfactory.jagged.DecryptingChannelFactory
import com.exceptionfactory.jagged.EncryptingChannelFactory
import com.exceptionfactory.jagged.framework.stream.StandardDecryptingChannelFactory
import com.exceptionfactory.jagged.framework.stream.StandardEncryptingChannelFactory
import com.exceptionfactory.jagged.x25519.X25519KeyPairGenerator
import com.exceptionfactory.jagged.x25519.X25519RecipientStanzaReaderFactory
import com.exceptionfactory.jagged.x25519.X25519RecipientStanzaWriterFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

data class AgeKeyPair(
    val publicKey: String,
    val secretKey: String,
)

object AgeEncryption {

    fun generateKeyPair(): AgeKeyPair {
        val generator = X25519KeyPairGenerator()
        val keyPair = generator.generateKeyPair()
        return AgeKeyPair(
            publicKey = keyPair.public.toString(),
            secretKey = keyPair.private.toString(),
        )
    }

    fun encrypt(publicKey: String, data: ByteArray): ByteArray {
        val stanzaWriter = X25519RecipientStanzaWriterFactory.newRecipientStanzaWriter(publicKey)
        val channelFactory: EncryptingChannelFactory = StandardEncryptingChannelFactory()

        val output = ByteArrayOutputStream()
        val inputChannel = Channels.newChannel(ByteArrayInputStream(data))
        val encryptingChannel = channelFactory.newEncryptingChannel(
            Channels.newChannel(output),
            listOf(stanzaWriter),
        )

        copy(inputChannel, encryptingChannel)
        encryptingChannel.close()
        return output.toByteArray()
    }

    fun decrypt(secretKey: String, encrypted: ByteArray): ByteArray {
        val stanzaReader = X25519RecipientStanzaReaderFactory.newRecipientStanzaReader(secretKey)
        val channelFactory: DecryptingChannelFactory = StandardDecryptingChannelFactory()

        val output = ByteArrayOutputStream()
        val decryptingChannel = channelFactory.newDecryptingChannel(
            Channels.newChannel(ByteArrayInputStream(encrypted)),
            listOf(stanzaReader),
        )

        copy(decryptingChannel, Channels.newChannel(output))
        decryptingChannel.close()
        return output.toByteArray()
    }

    private fun copy(input: ReadableByteChannel, output: WritableByteChannel) {
        val buffer = ByteBuffer.allocate(65536)
        while (input.read(buffer) != -1) {
            buffer.flip()
            while (buffer.hasRemaining()) {
                output.write(buffer)
            }
            buffer.clear()
        }
    }
}
