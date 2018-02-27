package MultiProtocolo;

/**
 * Realizado por: Adrian Fernandez Claverias
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClienteECO_TCP {
	public static void actionClient() throws IOException {

		SocketChannel client = SocketChannel.open();
		client.connect(new InetSocketAddress("localhost", 7));

		BufferedReader ent = new BufferedReader(new InputStreamReader(System.in));
		String entrada;

		System.out.println("Inicializado Cliente TCP");
		// Bucle de envio y recepcion (ECO)
		do {
			System.out.print("Inserte texto de entrada: ");
			entrada = ent.readLine();

			byte[] message = new String(entrada).getBytes();
			ByteBuffer buffer = ByteBuffer.wrap(message);
			client.write(buffer);
			buffer.clear();

			ByteBuffer bufferRespuesta = ByteBuffer.allocate(1024);
			bufferRespuesta.clear();

			int bytesread = client.read(bufferRespuesta);
			if (bytesread == -1) {
				client.close();
			} else {
				bufferRespuesta.flip(); // read from the buffer
				byte[] received = new byte[buffer.remaining()];
				bufferRespuesta.get(received);

				System.out.println("Recibido del servidor: " + new String(received));
				bufferRespuesta.clear();

			}
		} while (!entrada.equals("."));
		System.out.println("Conexion Cerrada");
	}

	public static void main(String[] args) throws IOException {
		actionClient();
	}
}