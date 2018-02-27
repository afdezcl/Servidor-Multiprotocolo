package MultiProtocolo;

/**
 * Realizado por: Adrian Fernandez Claverias
 */
import java.net.SocketAddress;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.io.IOException;

public class UDPEchoSelectorProtocol implements EchoProtocol {
	private static final int ECHOMAX = 255; // Maximo size del Datagrama
	private Charset charset = Charset.forName("UTF-8");

	static class ClientRecord {
		public SocketAddress clientAddress;
		public ByteBuffer buffer = ByteBuffer.allocate(ECHOMAX);
	}

	public void handleAccept(SelectionKey key) throws IOException {

	}

	public void handleRead(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		ClientRecord clntRec = (ClientRecord) key.attachment();

		System.out.println("Mensaje UDP Recibido ");

		clntRec.buffer.clear(); // Prepara el buffer para recibir
		clntRec.clientAddress = channel.receive(clntRec.buffer);

		if (clntRec.clientAddress != null) {
			// Regista la escritura con el selector
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	public void handleWrite(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		ClientRecord clntRec = (ClientRecord) key.attachment();
		clntRec.buffer.flip(); // Prepara el buffer para enviar
		int bytesSent = channel.send(clntRec.buffer, clntRec.clientAddress);
		if (bytesSent != 0) {
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	public String decode(ByteBuffer buffer) {
		CharBuffer charBuffer = charset.decode(buffer);
		return charBuffer.toString();
	}

	public ByteBuffer encode(String str) {
		return charset.encode(str);
	}
}
