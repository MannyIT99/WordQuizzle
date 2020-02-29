import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;

//Classe che modella il server di Word Quizzle
public class ServerWQ {
	public static final int DIM_BYTEBUFFER = 10240;
	public static HashMap<String, SelectionKey> skUtenti = null; //HashMap contenente le associazioni tra nickUtente e SelectionKey
	public static ExecutorService executor; //Executor per i thread che gestiscono le sfide
	private static Selector selector; //Main selector
	
	//Questo metodo permette di svegliare il main selector
	public synchronized static void sveglia() {
		selector.wakeup();
	}

	//Questo metodo permette di avviare il servizio di registrazione
	private static void avviaServizioDiRegistrazioneWQ() {
		try {
			//Creo l'oggetto ImplementazioneCongresso
			ImplementazioneRegistrazioneWQ ic = new ImplementazioneRegistrazioneWQ();
			//Esporto l'oggetto
			RegistrazioneWQ stub = (RegistrazioneWQ) UnicastRemoteObject.exportObject(ic, 0);
			//Creo il registry sulla porta
			LocateRegistry.createRegistry(ParametriCondivisi.portaServizioRegistrazione);
			//Ottengo il registry
			Registry r = LocateRegistry.getRegistry(ParametriCondivisi.portaServizioRegistrazione);
			//Assegno un nome al servizio
			r.bind(ParametriCondivisi.nomeServizioRegistrazione, stub);
			//Il servizio è pronto
		} catch(RemoteException re) {
			//Se viene sollevata la RemoteException, stampo l'errore
			System.out.println("Errore nell'avvio del server di registrazione WQ");
		} catch(AlreadyBoundException abe) {
			System.out.println("Errore in bind (" + ParametriCondivisi.nomeServizioRegistrazione + ")");
		}
	}

	@SuppressWarnings("rawtypes")
	//Questo metodo permette di avviare il server
	private static void avviaSistema(String serverAddress, int serverPort) {
		executor = Executors.newCachedThreadPool(); //Inizializza l'executor ad un newCachedThreadPool
		skUtenti = new HashMap<String, SelectionKey>(); //Inizailizza l'hashMap <nickUtente, SelectionKey>

		try {
			//Creo selector e channel
			selector = Selector.open(); //Apro il selector
			ServerSocketChannel serverChannel = ServerSocketChannel.open(); //Creo il server Socket
			serverChannel.configureBlocking(false); //Configuro la modalità bloccante a false

			//bind sulla porta
			InetSocketAddress listenAddr = new InetSocketAddress(serverAddress, serverPort);
			serverChannel.socket().bind(listenAddr);
			serverChannel.register(selector, SelectionKey.OP_ACCEPT); //registro il serverChannel al main selector

			//Server Pronto
			while(true) {
				//Attendo eventi
				selector.select();

				//Lavoro sulle selected keys
				Iterator keys = selector.selectedKeys().iterator();
				while(keys.hasNext()) {
					SelectionKey key = (SelectionKey) keys.next();

					//Necessario per evitare di riselezionare la stessa chiave al prossimo ciclo
					keys.remove();

					if(!key.isValid()) { //Se la key non è valida
						continue; //passo alla prossima iterazione
					}

					if(key.isAcceptable()) { //Se la key è pronta per una nuova connessione
						accept(key, selector); //chiamo il metodo accept
					} else if(key.isReadable()) { //Se la key è pronta per un'operazione di lettura
						read(key); //chiamo il metodo read
					} else if(key.isWritable()) { //Se la key è pronta per un'operazione di scrittura
						write(key); //chiamo il metodo write
					}
				}
			}
		} catch(IOException ioe) {
			//Se IOException, il server termina la sua esecuzione
			System.err.println("IOException in ServerWQ: avviaSistema()");
			System.err.println(ParametriCondivisi.msg_server_end);
			System.exit(0);
		}
	}

	//Questo metodo permette di accettare nuove connessioni da client
	private static void accept(SelectionKey key, Selector selector) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel(); //Ottengo la serverSocket
		SocketChannel channel = serverChannel.accept(); //Accetto la connessione e ottengo il channel relativo al client
		channel.configureBlocking(false); //Lo configuro come non bloccante
		channel.register(selector, SelectionKey.OP_READ); //Lo registro al main selector con interestOps settata a OP_READ
	}

	//Questo metodo permette di leggere i comandi inviati dai client
	private static void read(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel(); //Ottengo il channel associato alla key
		ByteBuffer buffer = ByteBuffer.allocate(DIM_BYTEBUFFER); //Alloco il ByteBuffer
		int numRead = -1;
		try {
			numRead = channel.read(buffer); //Leggo il comando del client
		} catch(IOException ioe) {
			//Se IOException
			StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(channel); //disconnetto l'utente
			System.err.println("IOException in ServerWQ: read() [channel.read()]");
			try {
				channel.close(); //chiudo il channel del client
			} catch (IOException e) {
				//Se IOException, il server termina la sua esecuzione
				System.err.println("IOException in ServerWQ: read() [channel.close()]");
				System.err.println(ParametriCondivisi.msg_server_end);
				System.exit(0);
			}
			key.cancel(); //cancello la key dal main selector
			return; //return
		}
		if(numRead == -1) { //Se non viene sollevata la IOException ma vengono letti -1 caratteri
			StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(channel); //disconnetto l'utente
			System.err.println("Letti -1 caratteri in ServerWQ: read() [channel.read()]");
			try {
				channel.close(); //chiudo il channel
			} catch (IOException e) {
				//Se IOException, il server termina la sua esecuzione
				System.err.println("IOException in ServerWQ: read() [channel.close()]");
				System.err.println(ParametriCondivisi.msg_server_end);
				System.exit(0);
			}
			key.cancel(); //cancello la key
			return; //ritorno
		}
		elabora(key, buffer); //elaboro il contenuto del buffer
	}

	private static void write(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel(); //Ottengo il channel associato alla key
		ByteBuffer byteBuffer = (ByteBuffer) key.attachment(); //Ottengo l'attachment
		try {
			channel.write(byteBuffer); //lo scrivo al client
		} catch (IOException ioe) {
			//Se IOException
			System.err.println("IOException in ServerWQ: write() [channel.write()]");
			StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(channel); //disconnetto il client
			try {
				channel.close(); //chiudo il socket
			} catch (IOException ioe1) {
				//Se IOException, il server termina la sua esecuzione
				System.err.println("IOException in ServerWQ: write() [channel.close()]");
				System.err.println(ParametriCondivisi.msg_server_end);
				System.exit(0);
			}
			key.cancel(); //cancello la key dal main selector
			return; //return
		}
		key.interestOps(SelectionKey.OP_READ); //Setto l'interestOps della key a OP_READ
	}

	//Questo metodo elabora il comando ricevuto da parte del client
	private static void elabora(SelectionKey key, ByteBuffer buffer) {
		String comando = new String(buffer.array()).trim(); //Ottengo il comando
		StringTokenizer st = new StringTokenizer(comando); //StringTokenizer per il comando
		String risposta = null;
		String nickUtente = null;
		SocketChannel channel = (SocketChannel) key.channel(); //Ottengo il channel associato alla key
		Gson gson = new Gson(); //Creo un oggetto Gson
		if(st.hasMoreTokens()) { //Se ci sono token
			String finalCommand = st.nextToken(); //Il primo token è quello del comando
			if(finalCommand.equals(ParametriCondivisi.cmd_sfida)) { //Se il comando è sfida
				nickUtente = StrutturaLogin.ottieniStrutturaLogin().ottieniUtente(channel); //Ottengo il nickUtente dello sfidante
				String avversario = st.nextToken(); //Il secondo token è il nickUtente dello sfidato
				if(!Grafo.ottieniGrafo().esisteRelazione(nickUtente, avversario)) { //Se non sono amici
					risposta = new String(ParametriCondivisi.msg_no_friendship); //creo la risposta relativa
					key.attach(ByteBuffer.wrap(risposta.getBytes())); //la inserisco come attachment
					key.interestOps(SelectionKey.OP_WRITE); //e setto l'interestOps della key a OP_WRITE
				} else { //altrimenti
					if(!StrutturaLogin.ottieniStrutturaLogin().utenteConnesso(avversario)) { //se l'utente sfidato non è connesso
						risposta = new String(ParametriCondivisi.msg_player_offline); //Creo la risposta relativa 
						key.attach(ByteBuffer.wrap(risposta.getBytes())); //La inserisco come attachment
						key.interestOps(SelectionKey.OP_WRITE); //Setto l'interestOps della key a OP_WRITE
					} else { //altrimenti
						switch(Impegnati.ottieniImpegnati().aggiungiCoppia(nickUtente, avversario)) { //Impegno gli sfidanti
						case 1: //Se ritorna 1
							risposta = new String(ParametriCondivisi.msg_player_in_game); //Il player sfidato è impegnato, e quindi creo la risposta relativa
							key.attach(ByteBuffer.wrap(risposta.getBytes())); //la inserisco come attachment
							key.interestOps(SelectionKey.OP_WRITE); //setto l'interestOps della key a OP_WRITE
							break; //esco dal case
						default: //altrimenti
							SocketAddress saAvversario = StrutturaLogin.ottieniStrutturaLogin().ottieniSocketChannel(avversario).socket().getRemoteSocketAddress(); //Ottengo il socketAddress dell'avverssario
							executor.submit(new InizializzazioneSfida(nickUtente, avversario, key, skUtenti.get(avversario), saAvversario)); //Avvio un thread per inizializzare la sfida
						}
					}
				}
			} else { //altrimenti
				switch(finalCommand) {
				case ParametriCondivisi.cmd_login: //login
					nickUtente = st.nextToken(); //il primo token è il nickUtente
					switch(StrutturaLogin.ottieniStrutturaLogin().connettiUtente(channel, nickUtente, st.nextToken())) { //Lo connetto
					case 10: //Se ritorna 10
						risposta = new String(ParametriCondivisi.msg_double_conn); //Un utente è già connesso con lo stesso nickUtente
						break; //esco dal case
					case 20: //se ritorna 20
						risposta = new String(ParametriCondivisi.msg_no_nick); //Non esiste questo nickUtente
						break; //esco dal case
					case 30: //se ritorna 30
						risposta = new String(ParametriCondivisi.msg_psw_err); //La password è sbagliata
						break; //esco dal case
					default: //altrimenti
						skUtenti.put(nickUtente, key); //Inserisco un'associazione tra il nickUtente e la SelectionKey
						risposta = new String(ParametriCondivisi.msg_ok_login); //Creo la risposta
						break; //esco dal case
					}
					break; //esco dal case login
				case ParametriCondivisi.cmd_logout: //logout
					nickUtente = StrutturaLogin.ottieniStrutturaLogin().ottieniUtente(channel); //Ottengo il nickUtente
					StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(channel); //Disconnetto l'utente
					risposta = new String(ParametriCondivisi.msg_ok_logout); //Creo la risposta
					skUtenti.remove(nickUtente); //Rimuovo l'associazione nickUtente - SelectionKey
					break; //esco dal case
				case ParametriCondivisi.cmd_mostra_punteggio: //mostra_punteggio
					nickUtente = StrutturaLogin.ottieniStrutturaLogin().ottieniUtente(channel); //Ottengo il nickUtente
					risposta = new String("Punteggio: " + Grafo.ottieniGrafo().ottieniUtente(nickUtente).ottieniPunteggioUtente()); //Creo la risposta
					break; //esco dal case
				case ParametriCondivisi.cmd_lista_amici: //lista_amici
					nickUtente = StrutturaLogin.ottieniStrutturaLogin().ottieniUtente(channel); //Ottengo il nickUtente
					List<String> amici = Grafo.ottieniGrafo().ottieniUtente(nickUtente).ottieniListaAmici(); //Ottengo la lista di amici
					risposta = new String(gson.toJson(amici)); //Creo la risposta
					break; //esco dal case
				case ParametriCondivisi.cmd_mostra_classifica: //mostra_classifica
					nickUtente = StrutturaLogin.ottieniStrutturaLogin().ottieniUtente(channel); //Ottengo il nickUtente
					risposta = new String(gson.toJson(Grafo.ottieniGrafo().ottieniClassifica(nickUtente))); //Ottengo la classifica e creo la risposta
					break; //esco dal case
				case ParametriCondivisi.cmd_aggiungi_amico: //aggiungi_amico
					nickUtente = StrutturaLogin.ottieniStrutturaLogin().ottieniUtente(channel); //ottengo il nickUtente
					String amico = st.nextToken(); //l'amico è il secondo token
					switch(Grafo.ottieniGrafo().aggiungiAmico(nickUtente, amico)) { //Aggiungo l'amico
					case 10: //se ritorna 10
						risposta = new String(ParametriCondivisi.msg_no_friend_nick); //Creo la risposta
						break; //esco dal case
					case 20: //se ritorna 20
						risposta = new String("Relazione di amicizia con " + amico + " già esistente"); //Creo la risposta
						break; //esco dal case
					default: //altrimenti
						risposta = new String("Amicizia " + nickUtente + "-" + amico + " creata"); //creo la risposta
						break; //esco dal case
					}
					break; //esco dal case aggiungi_amico
				}
				key.attach(ByteBuffer.wrap(risposta.getBytes())); //Inserisco la risposta nell'attachment
				key.interestOps(SelectionKey.OP_WRITE); //Setto l'interestOps della selectionKey a OP_WRITE
			} 
		}
	}

	//Main
	public static void main(String[] args) {
		avviaServizioDiRegistrazioneWQ(); //avvio il servizio di registrazione
		String serverAddress;
		int serverPort;
		//Inizializzo i parametri per il binding del server
		try {
			serverAddress = args[0];
			serverPort = Integer.parseInt(args[1]);
		} catch(ArrayIndexOutOfBoundsException aioobe) {
			serverAddress = ParametriCondivisi.serverAddress;
			serverPort = ParametriCondivisi.serverPort;
		}
		avviaSistema(serverAddress, serverPort); //chiamo il metodo avvioSistema
	}
}
