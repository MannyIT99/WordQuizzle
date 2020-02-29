import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

//Questa classe permette di modellare l'inizializzazione della sfida (richiesta e avvio del thread gestore della sfida)
public class InizializzazioneSfida implements Runnable {
	int T1; //Millisecondi da attendere per la risposta alla richiesta di sfida
	String sfidante, sfidato; //Stringhe relative al nickUtente dello sfidante e dello sfidato
	SelectionKey skSfidante, skSfidato; //SelectionKey dello sfidante e dello sfidato
	InetSocketAddress isa; //InetSocketAddress dello sfidato 
	InetAddress address; //InetAddress dello sfidato
	int port; //Porta dello sfidato (per inviare richiesta UDP)
	SocketChannel scSfidato, scSfidante; //SocketChannel dello sfidato e dello sfidante
	
	//Costruttore
	public InizializzazioneSfida(String sfidante, String sfidato, SelectionKey skSfidante, SelectionKey skSfidato, SocketAddress saClient) {
		this.sfidante = sfidante; //Inizializzo il nickUtente dello sfidante con il valore passato
		this.sfidato = sfidato; //Inizializzo il nickUtente dello sfidato con il valore passato
		this.skSfidante = skSfidante; //Inizializzo la SelectionKey dello sfidante con il valore passato
		this.skSfidato = skSfidato; //Inizializzo la SelectionKey dello sfidato con il valore passato
		this.isa = (InetSocketAddress) saClient; //Inizializzo l'InetSocketAddress dello sfidato con il valore passato
		this.address = isa.getAddress(); //Inizializzo l'inetAddress con l'indirizzo associato all'InetSocketAddress
		this.port = isa.getPort(); //Inizializzo la porta con la porta associata all'InetSocketAddress
		this.scSfidato = (SocketChannel) skSfidato.channel(); //Inizializzo il socketChannel dello sfidato 
		this.scSfidante = (SocketChannel) skSfidante.channel(); //Inizializzo il socketChannel dello sfidante
		this.T1 = ParametriCondivisi.T1; //Inizializzo T1 con il valore presente in ParametriCondivisi
	}
	
	@Override
	public void run() {
		//Le chiavi del selector principale nel server, relative ai due player
		skSfidante.interestOps(0); //vengono modificate settando l'interestOps a 0
		skSfidato.interestOps(0);
		inviaRichiesta(); //Invio la richiesta all'avversario
		elaboroRisposta(); //Aspetto ed elaboro la risposta dell'avversario
	}

	//Questo metodo attende ed elabora la risposta dello sfidato
	public void elaboroRisposta() {
		ByteBuffer bB = ByteBuffer.allocate(2); //Alloco un ByteBuffer di 2 posizioni
		
		Long timeLeft = (long) T1; //Tempo per la richiesta
		Long time1 = System.currentTimeMillis(); //Tempo attuale
		Long time2;
		int r = -1; 
		do { //Ciclo
			try {
				r = scSfidato.read(bB); //Leggo la risposta alla richiesta di sfida
			} catch(IOException ioe) {
				//Se sollevato IOException
				StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(scSfidato); //disconnetto lo sfidato
				System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidato.read()]");
				skSfidato.cancel(); //Cancello la SelectionKey dello sfidato dal main selector del server
				try {
					scSfidato.close(); //Chiudo il socketChannel dello sfidato
				} catch(IOException ioe1) {
					//Se sollevato IOException, il server termina la sua esecuzione
					System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidato.close()]");
					System.out.println(ParametriCondivisi.msg_server_end);
					System.exit(0);
				}
				try {
					scSfidante.write(ByteBuffer.wrap(new String(ParametriCondivisi.msg_invite_off).getBytes())); //Avviso lo sfidante che il player è offline
				} catch(IOException ioe2) {
					//Se sollevato IOException
					StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(scSfidante); //disconnetto lo sfidante
					System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.write()]");
					skSfidante.cancel(); //e cancello la sua SelectionKey dal main selector del server
					try {
						scSfidante.close(); //chiudo il socketChannel dello sfidante
					} catch(IOException ioe3) {
						//Se sollevato IOException, il server termina la sua esecuzione
						System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.close()]");
						System.out.println(ParametriCondivisi.msg_server_end);
						System.exit(0);
					}
				}
				Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato); //I player non sono più impegnati
				skSfidante.interestOps(SelectionKey.OP_READ); //Modifico la SelectionKey dello sfidante, settando a OP_READ l'interestOps
				ServerWQ.sveglia(); //Sveglio il main selector del server
				return; //return
			}
			if(r == -1) { //Se non viene sollevata la IOException e vengono letti -1 caratteri
				StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(scSfidato); //disconnetto lo sfidato
				System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidato.read()]");
				skSfidato.cancel(); //Cancello la SelectionKey dello sfidato dal main selector del server
				try {
					scSfidato.close(); //Chiudo il socketChannel dello sfidato
				} catch(IOException ioe1) {
					//Se sollevato IOException, il server termina la sua esecuzione
					System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidato.close()]");
					System.out.println(ParametriCondivisi.msg_server_end);
					System.exit(0);
				}
				try {
					scSfidante.write(ByteBuffer.wrap(new String(ParametriCondivisi.msg_invite_off).getBytes())); //Avviso lo sfidante che il player è offline
				} catch(IOException ioe2) {
					StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(scSfidante);
					System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.close()]");
					skSfidante.cancel(); //e cancello la sua SelectionKey dal main selector del server
					try {
						scSfidante.close(); //chiudo il socketChannel dello sfidante
					} catch(IOException ioe3) {
						//Se sollevato IOException, il server termina la sua esecuzione
						System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.close()]");
						System.out.println(ParametriCondivisi.msg_server_end);
						System.exit(0);
					}
				}
				Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato); //I player non sono più impegnati
				skSfidante.interestOps(SelectionKey.OP_READ); //Modifico la SelectionKey dello sfidante, settando a OP_READ l'interestOps
				ServerWQ.sveglia(); //Sveglio il main selector del server
				return; //return
			}
			time2 = System.currentTimeMillis(); //Calcolo il tempo attuale
			if(timeLeft - (time2-time1) <= 0) { //Se il tempo per rispondere alla richiesta - il tempo passato da quando è stata inviata è <= 0
				try {
					scSfidante.write(ByteBuffer.wrap(new String("Errore - La richiesta di sfida a " + sfidato + " è scaduta").getBytes())); //Avviso lo sfidante che la richiesta è scaduta
				} catch(IOException ioe) {
					//Se sollevato IOException
					StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(scSfidante); //disconnetto lo sfidante
					System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.write()]");
					skSfidante.cancel(); //cancello la selectionKey dello sfidante dal main selector del server
					try {
						scSfidante.close(); //chiudo il socketChannel dello sfidante
					} catch (IOException e) {
						//Se sollevato IOException, il server termina la sua esecuzione
						System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.close()]");
						System.out.println(ParametriCondivisi.msg_server_end);
						System.exit(0);
					}
					skSfidato.interestOps(SelectionKey.OP_READ); //Modifico la selectionKey dello sfidato, settando a OP_READ l'interestOps
					ServerWQ.sveglia(); //Sveglio il main selector del server
					return; //return
				}
				Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato); //I player non sono più impegnati
				resettaMainSelector(); //Resetto le selectionKey dei player e sveglio il main selector del server
				return; //return
			}
			if(r != 0) {
				break; //Se leggo un numero di caratteri diverso da 0 esco dal ciclo
			}
		} while(true);
		//Se ottengo una risposta alla richiesta di sfida entro il limite di tempo
		String risposta = ""; //Creo una stringa che conterrà la risposta
		try {
			risposta = new String(bB.array(), "UTF-8"); //La inizializzo con la risposta dello sfidato
		} catch (UnsupportedEncodingException e) {
			//Se sollevato UnsupportedEncodingException, il server termina la sua esecuzione
			System.err.println("UnsupportedEncodingException in InizializzazioneSfida: elaboroRisposta()");
			System.out.println(ParametriCondivisi.msg_server_end);
			System.exit(0);
		}
		if(risposta.equals("SI")) { //Se la risposta è SI
			try {
				scSfidante.write(ByteBuffer.wrap(new String(sfidato + " ha accettato la tua sfida").getBytes())); //Avviso lo sfidante che lo sfidato ha accettato la sfida
			} catch(IOException ioe) {
				//Se sollevato IOException
				StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(scSfidante); //disconnetto lo sfidante
				System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.write()]");
				skSfidante.cancel(); //cancello la selectionKey dal main selector del server
				try {
					scSfidante.close(); //chiudo il socketChannel dello sfidante
				} catch(IOException ioe1) {
					//Se sollevato IOException, il server termina la sua esecuzione
					System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.close()]");
					System.out.println(ParametriCondivisi.msg_server_end);
					System.exit(0);
				}
				Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato); //I player non sono più impegnati
				skSfidato.interestOps(SelectionKey.OP_READ); //Modifico la selectionKey dello sfidato settando l'interestOps a OP_READ
				ServerWQ.sveglia(); //Sveglio il main selector del server
				return; //RETURN
			}
			//Avvio thread sfida, passando le selectionKeys dei player e i nickUtente
			Thread t = new Thread(new GestoreSfidaServer(skSfidante, skSfidato, sfidante, sfidato));
			t.start();
		} else { //Altrimenti
			try {
				scSfidante.write(ByteBuffer.wrap(new String(sfidato + " ha rifiutato la tua sfida").getBytes())); //Avviso lo sfidante che la richiesta è stata rifiutata
			} catch(IOException ioe) {
				//Se sollevato IOException
				Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato); //I player non sono più impegnati
				System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.write()]");
				skSfidante.cancel(); //Cancello la selectionkey dello sfidante dal main selector del server
				try {
					scSfidante.close(); //chiudo il socketChannel dello sfidante
				} catch(IOException ioe1) {
					//Se sollevato IOException, il server termina la sua esecuzione
					System.err.println("IOException in InizializzazioneSfida: elaboroRisposta() [scSfidante.close()]");
					System.out.println(ParametriCondivisi.msg_server_end);
					System.exit(0);
				}
				Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato); //I player non sono più impegnati
				skSfidato.interestOps(SelectionKey.OP_READ); //Modifico la selectionkey dello sfidato settando l'interestOps a OP_READ
				ServerWQ.sveglia(); //Sveglio il main selector del server
				return; //return
			}
			Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato); //I player non sono più impegnati
			resettaMainSelector(); //Resetto il main selector del server e lo sveglio
			return; //return
		}
	}

	public void inviaRichiesta() {
		DatagramSocket socket;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("SocketException in InizializzazioneSfida: inviaRichiesta() [... new DatagramSocket()]");
			Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato);
			resettaMainSelector();
			return;
		}
		byte[] buf;
		String richiesta = new String(sfidante);
		buf = richiesta.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			System.err.println("IOException in InizializzazioneSfida: inviaRichiesta() [socket.send()]");
			socket.close();
			Impegnati.ottieniImpegnati().eliminaCoppia(sfidante, sfidato);
			resettaMainSelector();
			return;
		}
		socket.close();
	}

	//Questo metodo è utilizzato per resettare le selectionKeys del main selector del server e risvegliarlo
	public void resettaMainSelector() {
		skSfidante.interestOps(SelectionKey.OP_READ);
		skSfidato.interestOps(SelectionKey.OP_READ);
		ServerWQ.sveglia();
	}
}
