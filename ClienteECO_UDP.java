package MultiProtocolo;
/**
 * Realizado por: Adrian Fernandez Claverias
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class ClienteECO_UDP {
	private int port = 7;
	private DatagramChannel socketChannel = null;
	private ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
	private ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
	private Charset charset = Charset.forName("UTF-8");
	private Selector selector;

	/**
	 * Constructor inicializador de variables
	 * @throws IOException
	 */
	public ClienteECO_UDP() throws IOException {
		socketChannel = DatagramChannel.open();
		InetSocketAddress isa = new InetSocketAddress("localhost", port);
		socketChannel.connect(isa);
		socketChannel.configureBlocking(false);
		
		System.out.println("Inicializado Cliente UDP");
		
		selector = Selector.open();
	}

	/**
	 * Lectura de lo introducido por teclado, para de leer cuando se introduce un punto
	 */
	public void receiveFromUser() {
		try {			
			BufferedReader localReader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Introduce el mensaje: ");
			String msg = null;
			
			boolean the_end=false;
			while ((msg = localReader.readLine()) != null && !the_end) {				
				synchronized (sendBuffer) {					
					sendBuffer.put(encode(msg + "\r\n"));
				}
				if (msg.equals("."))
					the_end=true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Registro de key
	 * @throws IOException
	 */
	public void talk() throws IOException {
		try {
			socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			
			while (selector.select() > 0) {
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = readyKeys.iterator();
				
				while (it.hasNext()) {
					SelectionKey key = null;
					try {
						key = (SelectionKey) it.next();
						it.remove();
						if (key.isReadable()) {
							receive(key);
						}
						if (key.isWritable()) {							
							send(key);
						}
					} catch (IOException e) {						
						try {
							if (key != null) {
								key.cancel();
								key.channel().close();
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				socketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Usado para el envio
	 * @param key
	 * @throws IOException
	 */
	public void send(SelectionKey key) throws IOException {
		DatagramChannel socketChannel = (DatagramChannel) key.channel();
		synchronized (sendBuffer) {
			sendBuffer.flip();
			socketChannel.write(sendBuffer);
			sendBuffer.compact();
		}
	}

	/**
	 * Usado para recibir
	 * @param key
	 * @throws IOException
	 */
	public void receive(SelectionKey key) throws IOException {
		DatagramChannel socketChannel = (DatagramChannel) key.channel();
		socketChannel.read(receiveBuffer);
		receiveBuffer.flip();
		String receiveData = decode(receiveBuffer);
		if (receiveData.indexOf("\n") == -1) {
			return;
		}
		String outputData = receiveData.substring(0,receiveData.indexOf("\n") + 1);
		
		System.out.print("Mensaje Recibido: "+ outputData);
		if (outputData.equals(".\r\n")) {
			key.cancel();
			socketChannel.close();
			System.out.print("Conexion Cerrada");
			selector.close();
			System.exit(0);
		}
		System.out.print("Introce el mensaje: ");
		ByteBuffer temp = encode(outputData);
		receiveBuffer.position(temp.limit());
		receiveBuffer.compact();
	}

	public String decode(ByteBuffer buffer) {
		CharBuffer charBuffer = charset.decode(buffer);
		return charBuffer.toString();
	}

	public ByteBuffer encode(String str) {
		return charset.encode(str);
	}

	public static void main(String[] args) throws IOException {
		 ClienteECO_UDP client = new ClienteECO_UDP();
		Thread receiver = new Thread() {
			public void run() {				
				client.receiveFromUser();
			}
		};
		receiver.start();
		client.talk();
	}
}
