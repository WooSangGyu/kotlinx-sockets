package kotlinx.sockets.examples

import kotlinx.coroutines.experimental.*
import kotlinx.sockets.*
import kotlinx.sockets.Socket
import java.net.*
import java.nio.*
import kotlin.system.*

fun main(args: Array<String>) {
    runBlocking {
        aSocket().tcp().connect(InetSocketAddress(9098)).use { socket ->
            println("Connected")

            val bb = ByteBuffer.allocate(8192)
            val cb = CharBuffer.allocate(8192)
            val decoder = Charsets.UTF_8.newDecoder()

            while (true) {
                val rc = socket.read(bb)

                bb.flip()
                decoder.decode(bb, cb, rc == -1)
                bb.compact()
                cb.flip()

                while (cb.hasRemaining()) {
                    val eolIndex = cb.indexOf('\n')
                    val lineChars = if (eolIndex != -1) (eolIndex + 1) else if (rc == -1) cb.remaining() else break

                    var endIndex = lineChars
                    while (endIndex > 0 && cb.get(endIndex - 1).isWhitespace()) {
                        endIndex--
                    }

                    if (endIndex > 0) {
                        processCommand(cb.subSequence(0, endIndex).toString(), socket)
                    }

                    cb.position(cb.position() + lineChars)
                }

                cb.compact()

                if (rc == -1) break
            }
        }
    }
}

private suspend fun processCommand(line: String, socket: Socket) {
    when {
        line.startsWith("exit") -> {
            println("Got exit")
            socket.respond("Bye.\n\n")
            socket.close()
            exitProcess(0)
        }
        line.startsWith("id") -> {
            socket.respond("ID: ${socket.remoteAddress}\n")
        }
        else -> {
            socket.respond("Unknown command: $line\n")
        }
    }
}

private suspend fun Socket.respond(text: String) {
    write(ByteBuffer.wrap(text.toByteArray()))
}
