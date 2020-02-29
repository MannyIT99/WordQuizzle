import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.atomic.AtomicBoolean;

//Classe utilizzata per la ricezione delle richieste di sfida
public class SfidaUDPClient implements Runnable  {
	DatagramSocket socket; //DatagramSocket utilizzata
	SfidaRicevuta sfidaRicevuta; //Oggetto sfidaRicevuta
	String nickUtente; //nickUtente
	AtomicBoolean running; //AtomicBoolean utile alla terminazione del thread

	//Costruttore
	public SfidaUDPClient(SfidaRicevuta sfidaRicevuta, String nickUtente, DatagramSocket socket) {
		this.socket = socket; //Inizializzo il datagram socket con il parametro passato
		this.nickUtente = nickUtente; //Inizializzo il nickUtente con il parametro passato
		this.sfidaRicevuta = sfidaRicevuta; //Inizializzo la sfidaRicevuta con il parametro passato
		this.running = new AtomicBoolean(false); //Inizializzo l'atomic boolean
	}
	
	//Questo metodo permette di terminare l'esecuzione del thread in ascolto di richieste
	public void uccidi() {
		running.set(false); //settando l'atomicBoolean a false
	}
	
	public void run() {
		running.set(true); //Setto l'atomicBoolean a true
		while(running.get()) { //Fin quando è true
			byte[] buf = new byte[256]; //Inizializzo un array di byte
			DatagramPacket packet = new DatagramPacket(buf, buf.length); //Inizializzo un datagram packet
			try {
				socket.receive(packet); //Ricevo un packet
			} catch (IOException e) {
				System.err.println("Errore - IOException nella ricezione di richieste di sfida");
			}
			String received = new String(packet.getData(), 0, packet.getLength()); //Converto i dati nel pacchetto ricevuto in stringa
			System.out.println(received + " ti ha sfidato. Accetti la sfida?"); //Stampo la stringa (il nome dello sfidante) e la richiesta
			sfidaRicevuta.setSfida(received); //Setto la sfida in sfidaRicevuta
		}
	}
}
