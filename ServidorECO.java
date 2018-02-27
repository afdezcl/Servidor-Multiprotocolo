package MultiProtocolo;

/**
 * Realizado por: Adrian Fernandez Claverias
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ServidorECO {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {

		// Abrir el selector
		Selector selector = Selector.open();
		// Abrir el canal pasivo en el selector TCP
		ServerSocketChannel server = ServerSocketChannel.open();
		server.socket().bind(new java.net.InetSocketAddress(7)); // Establecer lectura no bloqueante
		server.configureBlocking(false);

		SelectionKey serverkey = server.register(selector, SelectionKey.OP_ACCEPT);

		// Abrir canal para UDP pasivo
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new java.net.InetSocketAddress(7));
		channel.register(selector, SelectionKey.OP_READ, new UDPEchoSelectorProtocol.ClientRecord());

		System.out.println("Servidor ECO inicializado, esperando conexiones...");

		UDPEchoSelectorProtocol echoSelectorProtocol = new UDPEchoSelectorProtocol();

		while (true) {
			// Llamada bloqueante
			selector.select();
			// Iterar sobre el conjunto de canales activos
			Set<SelectionKey> keys = selector.selectedKeys();
			for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
				SelectionKey key = (SelectionKey) i.next();
				i.remove();

				if (key.isAcceptable()) { // Para TCP
					SocketChannel client = server.accept();
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

				} else if (key.isReadable()) { // Para TCP y UDP
					String tipo = key.channel().toString();

					if (tipo.indexOf("SocketChannel") != -1) { // TCP

						SocketChannel client = (SocketChannel) key.channel();
						ByteBuffer buffer = (ByteBuffer) key.attachment();
						try {
							buffer.clear();
							int bytesread = client.read(buffer);
							if (bytesread == -1) { // Se ha cerrado la conexion
								key.cancel();
								client.close();
								continue;
							} else {
								buffer.flip(); 
								byte[] received = new byte[buffer.remaining()];
								buffer.get(received);

								System.out.println("Mensaje TCP Recibido");

								client.write(ByteBuffer.wrap(received));
								buffer.clear(); 
							}
						} catch (Exception e) {
						}

					} else { //udp
						echoSelectorProtocol.handleRead(key);
					}

				} else if (key.isValid() && key.isWritable()) { // para UDP
					echoSelectorProtocol.handleWrite(key);
				}
			}
		}
	}
}
