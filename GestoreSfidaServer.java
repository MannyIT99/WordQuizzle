import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class GestoreSfidaServer implements Runnable {
	int T2; //Tempo per la sfida
	int K; //Numero di parole da inviare
	int end; //Intero che indica i client che hanno terminato
	boolean uscito = false; //Booleano che indica se qualche client è uscito
	String nickSfidante, nickSfidato; //nickUtenti dei player
	SelectionKey keySfidante, keySfidato; //SelectionKey dei player del main selector del server
	SocketChannel sfidante, sfidato; //SocketChannel dei player
	List<String> parole; //Lista di parole
	HashMap<String, String> paroleSelezionate = null; //HashMap contenente le parole selezionate
	HashMap<SocketChannel, HashMap<String, Integer>> statistiche = null; //HashMap contenente i dati temporanei della partita
	SelectionKey skSfidante, skSfidato = null; //SelectionKey dei player del selector della sfida
	Boolean sfidanteOut = false, sfidatoOut = false; //Booleani che indicano se i player sono usciti
	
	//Costruttore
	public GestoreSfidaServer(SelectionKey keySfidante, SelectionKey keySfidato, String nickSfidante, String nickSfidato) {
		//Inizializzaizoni
		this.nickSfidante = nickSfidante;
		this.nickSfidato = nickSfidato;
		this.keySfidante = keySfidante;
		this.keySfidato = keySfidato;
		this.end = 0;
		this.sfidante = (SocketChannel) keySfidante.channel();
		this.sfidato = (SocketChannel) keySfidato.channel();
		this.parole = null;
		paroleSelezionate = new HashMap<String, String>();
		K = ParametriCondivisi.K;
		statistiche = new HashMap<SocketChannel, HashMap<String, Integer>>();
		this.T2 = ParametriCondivisi.T2;
		inizializza(); //Chiamo il metodo inizializza()
	}
	
	//Questo metodo inserisce in statistiche una voce per entrambi i plauer
	void inizializza() {
		statistiche.put(sfidante, new HashMap<String, Integer>());
		statistiche.put(sfidato, new HashMap<String, Integer>());
		for(SocketChannel sc: statistiche.keySet()) { //per ogni player inizializza i dati
			statistiche.get(sc).put("PunteggioCorrente", 0);
			statistiche.get(sc).put("Giuste", 0);
			statistiche.get(sc).put("Sbagliate", 0);
		}
	}
	
	//Questo metodo sceglie le parole della sfida
	List<String> scegliParole() {
		//Inserisco ogni linea (parola) del file in una lista di stringhe
		try {
			parole = Files.readAllLines(Paths.get(ParametriCondivisi.pathFileParole));
		} catch(IOException ioe) {
			//Se IOException
			System.err.println("Errore - IOException in scegli parole");
			return null; //ritorno null
		}
		if(parole.isEmpty()) {
			System.err.println("File parole vuoto.");
			return null;
		}
		
		//Scelgo le parole
		Random rand = new Random(); //Creo oggetto random
		for(int i=0; i<K; i++) { //Ciclo un numero di volte pari al numero di parole da inviare al client
			//Ottengo indice random
			int randomIndex = rand.nextInt(parole.size());
			//Ottengo parola di posizione randomIndex
			String parola = parole.get(randomIndex);
			//Traduco la parola e la inserisco in una hashmap con la relativa traduzione
			try {
				paroleSelezionate.put(parola, traduciParola(parola));
			} catch(IOException ioe) {
				System.err.println("Errore - IOException in traduci parola");
				return null;
			}
			parole.remove(randomIndex);
		}
		return new ArrayList<String>(paroleSelezionate.keySet()); //Ritorno le parole selezionate   
	}

	//Questo metodo traduce le parole selezionate
	String traduciParola(String parola) throws IOException {
		String traduzione = "";
		String jsonString = "";
		String line;
		URL url;
		BufferedReader reader;
		url = new URL("https://api.mymemory.translated.net/get?q=" + parola + "!&langpair=it|en");  //Chiedo al servizio la traduzione della parola passata
		reader = new BufferedReader(new InputStreamReader(url.openStream()));
		//Leggo la risposta 
		while((line = reader.readLine()) != null) {
			if("".equals(jsonString)) {
				jsonString = new String(line);
			} else {
				jsonString.concat(line);
			}
		}
		reader.close(); //Chiudo il BufferedReader
		Gson gson = new Gson(); //Creo un oggetto Gson
		JsonObject jobj = gson.fromJson(jsonString, JsonObject.class); //Deserializzo
		jobj = jobj.get("responseData").getAsJsonObject(); 
		traduzione = jobj.get("translatedText").getAsString(); //Ottengo il valore del campo contenente la traduzione della parola inviata
		return traduzione.toLowerCase(); //Ritorno la traduzione in lowerCase
	}
	
	//Questo metodo è utilizzato per la lettura delle traduzioni da parte del client
	void read(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel(); //Ottengo il socketChannel associato alla key
		ByteBuffer buffer = ByteBuffer.allocate(10); //Alloco il ByteBuffer
		int numRead = -1;
		try {
			numRead = channel.read(buffer); //Leggo la parola inviata dal client
		} catch(IOException ioe) {
			//Se IOException
			uscito = true; //Setto uscito = true
			StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(channel); //disconnetto l'utente
			System.err.println("IOException in GestoreSfida: read() [channel.read()]");
			try {
				channel.close(); //Chiudo il channel
			} catch(IOException ioe1) {
				//Se IOException, il server termina la sua esecuzione
				System.err.println("IOException in GestoreSfida: read() [channel.close()]");
				System.err.println(ParametriCondivisi.msg_server_end);
				System.exit(0);
			}
			key.cancel(); //Cancello la key dal selettore della sfida
			SocketChannel altro = altroPlayer(channel); //Ottengo il socketChannel dell'altro player
			try {
				//Avviso il player che l'avverdario ha lasciato la sfida
				altro.write(ByteBuffer.wrap("REPORT \nIl tuo avversario ha lasciato la sfida. \nI punteggi rimangono inalterati.".getBytes()));
			} catch(IOException ioe2) {
				//Se iOException
				System.err.println("IOException in GestoreSfida: read() [altro.write()]");
				try {
					altro.close(); //chiudo il channel dell'altro player
				} catch(IOException ioe1) {
					//Se IOException il server termina la sua esecuzione
					System.err.println("IOException in GestoreSfida: read() [altro.close()]");
					System.err.println(ParametriCondivisi.msg_server_end);
					System.exit(0);
				}
				StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(altro); //Disconnetto il player
				//Cancello le chiavi dei palyer dal main selector del server
				keySfidante.cancel();
				keySfidato.cancel();
				ServerWQ.sveglia(); //Sveglio il main selector del server
				return; //return
			}
			if(key == skSfidante) { //Se la key è dello sfidante
				keySfidante.cancel(); //Cancello la key dal main selector del server
				keySfidato.interestOps(SelectionKey.OP_READ); //e setto quella dello sfidato a OP_WRITE
			} else { //altrimenti
				keySfidato.cancel(); //Se la key è dello sfidato, la cancello dal main selector del server
				keySfidante.interestOps(SelectionKey.OP_READ); //e setto quella dello sfidante a OP_WRITE
			}
			ServerWQ.sveglia(); //Sveglio il main selector del server
			return; //return
		}
		if(numRead == -1) { //Se sono stati letti -1 caratteri e non sollevata la IOException
			uscito = true; //Setto uscito a true
			StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(channel); //Disconnetto il player
			System.err.println("Letti -1 caratteri in GestoreSfida: read() [channel.read()]");
			try {
				channel.close(); //Chiudo il channel
			} catch(IOException ioe1) {
				//Se IOException, il server termina la sua esecuzione
				System.err.println("IOException in GestoreSfida: read() [channel.close()]");
				System.err.println(ParametriCondivisi.msg_server_end);
				System.exit(0);
			}
			key.cancel(); //cancello la key dal selector delal sfida
			SocketChannel altro = altroPlayer(channel); //Ottengo il socketChannel dell'altro player
			try {
				//Avviso il player che l'avversario ha lasciato la sfida
				altro.write(ByteBuffer.wrap("REPORT \nIl tuo avversario ha lasciato la sfida. \nI punteggi rimangono inalterati.".getBytes()));
			} catch(IOException ioe2) {
				//Se IOException
				System.err.println("IOException in GestoreSfida: read() [altro.write()]");
				try {
					altro.close(); //Chiudo il channel dell'altro player
				} catch(IOException ioe1) {
					//Se IOException, il server termina la sua esecuzione
					System.err.println("IOException in GestoreSfida: read() [altro.close()]");
					System.err.println(ParametriCondivisi.msg_server_end);
					System.exit(0);
				}
				StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(altro); //Disconnetto l'altro utente
				keySfidante.cancel(); //Cancello le key dal main selector del server e lo sveglio
				keySfidato.cancel();
				ServerWQ.sveglia();
				return; //return
			}
			if(key == skSfidante) { //Se la key è dello sfidante
				keySfidante.cancel(); //Cancello la key dal main selector del server
				keySfidato.interestOps(SelectionKey.OP_READ); //e setto quella dello sfidato a OP_WRITE
			} else { //altrimenti
				keySfidato.cancel(); //Se la key è dello sfidato, la cancello dal main selector del server
				keySfidante.interestOps(SelectionKey.OP_READ); //e setto quella dello sfidante a OP_WRITE
			}
			ServerWQ.sveglia(); //Sveglio il main selector del server
			return; //return
		}
		String risposta = ""; //Creo una stringa per la rispsota
		try {
			risposta = new String(buffer.array(), "UTF-8").trim().toLowerCase(); //La inizializzo con il contenuto del byteBuffer
		} catch(UnsupportedEncodingException uee) {
			//Se UnsupportedEncodingException, il server termina la sua esecuzione
			System.err.println("UnsupportedEncodingException in GestoreSfida: read()");
			System.err.println(ParametriCondivisi.msg_server_end);
			System.exit(0);
		}
		HashMap<String, Integer> rif = statistiche.get(channel); //Ottengo il riferimento ai dati temporanei del player associato al channel
		if(risposta.equals(paroleSelezionate.get(parole.get((int)key.attachment())))) { //Se la risposta è corretta
			rif.replace("Giuste", statistiche.get(channel).get("Giuste") + 1); //Incremento il numero di quelle corrette
			rif.replace("PunteggioCorrente", statistiche.get(channel).get("PunteggioCorrente") + ParametriCondivisi.X); //e incremento il punteggio
		} else { //altrimenti
			rif.replace("Sbagliate", statistiche.get(channel).get("Sbagliate") + 1); //Incremento il numero di quelle sbagliate
			rif.replace("PunteggioCorrente", statistiche.get(channel).get("PunteggioCorrente") - ParametriCondivisi.Y); //e decremento il punteggio
		}
		key.attach((int)key.attachment() + 1); //Aggiorno l'attachment della key
		key.interestOps(SelectionKey.OP_WRITE); //Aggiorno l'interestOps a OP_WRITE
	}
	
	//Questo metodo è utilizzato per l'invio delle parole al client
	void write(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel(); //Ottengo il channel associato alla key
		String parola;
		if(((int)key.attachment()) == K) { //Se abbiamo inviato tutte le parole
			end++; //Incremeneto end
			key.interestOps(0); //e setto l'interestOps della key a 0
		} else { //altrimenti
			parola = parole.get((int)key.attachment()); //Ottengo la parola da inviare
			ByteBuffer byteBuffer = ByteBuffer.wrap(parola.getBytes()); //Inizializzo il byteBuffer
			try {
				channel.write(byteBuffer); //Invio la parola al client
			} catch(IOException ioe) {
				//Se IOException
				uscito = true; //uscito = true
				StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(channel); //Disconnetto l'utente
				System.err.println("IOException in GestoreSfida: write() [channel.write()]");
				try {
					channel.close(); //Chiudo il channel
				} catch(IOException ioe1) {
					//Se IOException, il server termina la sua esecuzione
					System.err.println("IOException in GestoreSfida: write() [channel.close()]");
					System.err.println(ParametriCondivisi.msg_server_end);
					System.exit(0);
				}
				key.cancel(); //Cancello la key dal selector della sfida
				SocketChannel altro = altroPlayer(channel); //Ottengo il channel dell'altro player
				try {
					//Avviso l'altro player che l'avversario ha lasciato la sfida
					altro.write(ByteBuffer.wrap("REPORT \nIl tuo avversario ha lasciato la sfida. \nI punteggi rimangono inalterati.".getBytes()));
				} catch(IOException ioe2) {
					//Se IOException
					System.err.println("IOException in GestoreSfida: write() [altro.write()]");
					try {
						altro.close(); //Chiudo il channel dell'altro player
					} catch(IOException ioe1) {
						//Se IOException, il server termina la sua esecuzione
						System.err.println("IOException in GestoreSfida: write() [altro.close()]");
						System.err.println(ParametriCondivisi.msg_server_end);
						System.exit(0);
					}
					StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(altro); //Disconnetto l'altro player
					keySfidante.cancel(); //Cancello le due selectionKey dal main selector del server
					keySfidato.cancel();
					ServerWQ.sveglia(); //Sveglio il main selector del server
					return; //return
				}
				if(key == skSfidante) { //Se la key è dello sfidante
					keySfidante.cancel(); //Cancello la key dal main selector del server
					keySfidato.interestOps(SelectionKey.OP_READ); //e setto quella dello sfidato a OP_WRITE
				} else { //altrimenti
					keySfidato.cancel(); //Se la key è dello sfidato, la cancello dal main selector del server
					keySfidante.interestOps(SelectionKey.OP_READ); //e setto quella dello sfidante a OP_WRITE
				}
				ServerWQ.sveglia(); //Sveglio il main selector del server
				return; //return
			}
			key.interestOps(SelectionKey.OP_READ); //Setto interestOps della selectionKey a OP_READ
		}
	}
	
	//Metodo per calcolare il report e aggiornare il punteggio
	String calcolaReportEAggiornaPunteggio(SocketChannel sc) {
		HashMap<String, Integer> riferimento = statistiche.get(sc); //Ottengo il riferimento ai dati temporanei del player associato al channel
		int giuste = riferimento.get("Giuste"); //Ottengo il numero di quelle corrette
		int sbagliate = riferimento.get("Sbagliate"); //Ottengo il numero di quelle sbagliate
		int nonDate = K - giuste - sbagliate; //Calcolo il numero di quelle non date
		int punteggioMio = riferimento.get("PunteggioCorrente"); //Ottengo il punteggio corrente
		int punteggioAvversario = statistiche.get(altroPlayer(sc)).get("PunteggioCorrente"); //Ottengo il punteggio corrente dell'avversario
		StringBuilder report = new StringBuilder("REPORT\nHai tradotto correttamente " + giuste + " parole" +
								   ", ne hai sbagliate " + sbagliate +
								   " e non risposto a " + nonDate +
								   ".\nHai totalizzato " + punteggioMio + " punti.\n" +
								   "Il tuo avversario ha totalizzato " + punteggioAvversario + " punti.\n"); //Costruisco il report da inviare al client
		if(punteggioMio > punteggioAvversario) { //Se il punteggio del player è maggiore di quello dell'altro player
			punteggioMio = punteggioMio + ParametriCondivisi.Z; //Incremeneto il punteggio corrente con i punti extra
			riferimento.replace("PunteggioCorrente", punteggioMio); //Aggiorno il punteggio corrente con quello appena calcolato
			//Inserisco nel report il messaggio di vittoria
			report.append("Congratulazioni, hai vinto! Hai guadagnato " + ParametriCondivisi.Z + " punti extra, per un totale di " + punteggioMio + " punti!");
		} else if(punteggioMio < punteggioAvversario) { //Se il punteggio del player è minore di quello dell'altro player
			//Inserisco nel report il messaggio di sconfitta
			report.append("Peccato, hai perso! Gioca ancora per recuperare e vincere punti!");
		} else { //Altrimenti inserisco nel report il messaggio di parità
			report.append("Pareggio! Difficile, ma non impossibile. Complimenti ad entrambi!");
		}
		Grafo.ottieniGrafo().aggiornaPunteggio(sc, punteggioMio); //Aggiorno il punteggio nel grafo
		return report.toString(); //Restituisco il report
	}
	
	//Questo metodo permette di ottenere l'altro player 
	SocketChannel altroPlayer(SocketChannel sc) {
		for(SocketChannel player: statistiche.keySet()) {
			if(player != sc) {
				return player;
			}
		}
		return null;
	}
	
	public void run() {
		parole = scegliParole(); //Il thread all'avvio, sceglie le parole
		if(parole == null) { //Se = null
			System.out.println("IOException in GestoreSfida: run() [parole == null]");
			for(SocketChannel player: statistiche.keySet()) { //Per ogni player
				try {
					//Invia il messaggio di errore
					player.write(ByteBuffer.wrap("REPORT \nErrore nell'inizializzazione della sfida.".getBytes()));
				} catch(IOException ioe1) {
					//Se IOException
					try {
						player.close(); //chiudo il channel del player
					} catch(IOException ioe2) {
						//Se IOException, il server termina la sua esecuzione
						System.err.println("IOException in GestoreSfida: run() [player.close()]");
						System.err.println(ParametriCondivisi.msg_server_end);
						System.exit(0);
					}
					StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(player); //Disconnetto il player
					if(player == sfidante) { //Se il player è lo sfidante
						keySfidante.cancel(); //Cancello la key dello sfidante dal main selector del server
					} else { //se il player è lo sfidato
						keySfidato.cancel(); //cancello la key dello sfidato dal main selector del server
					}
					continue; //vado alla prossima iterazione
				}
				if(player == sfidante) { //Se il player è lo sfidante
					keySfidante.interestOps(SelectionKey.OP_READ); //Setto l'interestOps della key del main selector del server a OP_READ
				} else { //altrimenti
					keySfidato.interestOps(SelectionKey.OP_READ); //Setto l'interestOps della key del main selector del server a OP_READ
				}
			}
			Impegnati.ottieniImpegnati().eliminaCoppia(nickSfidato, nickSfidante); //I player non sono più impegnati
			ServerWQ.sveglia(); //Sveglio il main selector del server
			return; //return
		}
		Selector selettoreSfida; //Dichiaro il selettore della sfida
		try {
			selettoreSfida = Selector.open(); //Apro il selettore
			//Registro i channel al nuovo selettore
			skSfidante = sfidante.register(selettoreSfida, SelectionKey.OP_WRITE, 0);
			skSfidato = sfidato.register(selettoreSfida, SelectionKey.OP_WRITE, 0);
			Long timeLeft = (long) T2; //Tempo per la sfida
			Long passato = (long) 0;
			Long time1 = System.currentTimeMillis(); //Tempo attuale
			while(true) { //Ciclo infinitamente
				//Attendo eventi
				if(timeLeft - passato <= 0 || end == 2) { //Se il tempo è finito oppure entrambi i player hanno finito di tradurre tutte le parole
					break; //esco dal ciclo
				}
				if(uscito) { //se un player è andato offline
					Impegnati.ottieniImpegnati().eliminaCoppia(nickSfidante, nickSfidato); //I player non sono più impegnati
					return; //ritorno
				}
				selettoreSfida.select(timeLeft); //seleziono
				
				//Lavoro sulle selected keys
				@SuppressWarnings("rawtypes")
				Iterator keys = selettoreSfida.selectedKeys().iterator();
				while(keys.hasNext()) {
					SelectionKey key = (SelectionKey) keys.next(); //ottengo la key corrente
					
					//Necessario per evitare di riselezionare la stessa chiave al prossimo ciclo
					keys.remove();
					if(!key.isValid()) { //se non è valida la key
						continue; //passo alla prossima iterazione
					}
					if(key.isReadable()) { //Se è pronta per un'operazione di lettura
						read(key); //chiamo il metodo read
					} else if(key.isWritable()) { //Se è pronta per un'operazione di scrittura
						write(key); //chiamo il metodo write
					}
				}
				Long time2 = System.currentTimeMillis(); //Calcolo il tempo attuale
				passato = time2 - time1; //Calcolo il tempo passato
			}
			//Setto gli interestOps delle key del selettore della sfida a OP_WRITE
			skSfidante.interestOps(SelectionKey.OP_WRITE);
			skSfidato.interestOps(SelectionKey.OP_WRITE);
			for(SocketChannel sc: statistiche.keySet()) { //Per ogni player
				String report = calcolaReportEAggiornaPunteggio(sc); //Calcolo il report e aggiorno il punteggio
				try {
					sc.write(ByteBuffer.wrap(report.trim().getBytes())); //Invio il report al player
				} catch(IOException ioe1) {
					//Se IOException
					StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(sc); //Disconnetto il player
					System.err.println("IOException in GestoreSfida: run() [sc.write()]");
					try {
						sc.close(); //chiudo il socket
					} catch(IOException ioe2) {
						//se IOException, il server termina la sua esecuzione
						System.err.println("IOException in GestoreSfida: run() [sc.close()]");
						System.err.println(ParametriCondivisi.msg_server_end);
						System.exit(0);
					}
					if(sc == sfidante) { //Controllo chi è il player out
						sfidanteOut = true; 
					} else {
						sfidatoOut = true;
					}
				}
			}
			Impegnati.ottieniImpegnati().eliminaCoppia(nickSfidante, nickSfidato); //I player non sono più impegnati
			if(sfidanteOut && sfidatoOut) { //Se entrambi sono out, li elimino dal main selector del server
				keySfidante.cancel();
				keySfidato.cancel();
			} else { //altrimenti
				if(sfidanteOut) { //se lo sfidante è out
					keySfidante.cancel(); //lo cancello dal main selector del server
					keySfidato.interestOps(SelectionKey.OP_READ); //e setto l'interestOps dello sfidato a oP_READ
				} else if(sfidatoOut) { //se lo sfidato è out
					keySfidato.cancel(); //lo cancello dal main selector del server
					keySfidante.interestOps(SelectionKey.OP_READ); //e setto l'interestOps dello sfidante a oP_READ
				} else { //Altrimenti setto l'interestOps delle due chiavi nel main selector del server a OP_READ
					keySfidante.interestOps(SelectionKey.OP_READ);
					keySfidato.interestOps(SelectionKey.OP_READ);
				}
			}
			ServerWQ.sveglia(); //e sveglio il main selector del server
		} catch (IOException ioe) {
			//Se IOException
			System.out.println("IOException in GestoreSfida: run()");
			for(SocketChannel player: statistiche.keySet()) { //Per ogni player
				try {
					//Invio l'errore al player
					player.write(ByteBuffer.wrap("REPORT \n La sfida si è arrestata improvvisamente.".getBytes()));
				} catch(IOException ioe1) {
					//Se IOException
					try {
						player.close(); //chiudo il socket del player
					} catch(IOException ioe2) {
						//Se IOException, il server termina la sua esecuzione
						System.err.println("IOException in GestoreSfida: run() [player.close()]");
						System.err.println(ParametriCondivisi.msg_server_end);
						System.exit(0);
					}
					StrutturaLogin.ottieniStrutturaLogin().disconnettiUtente(player); //Disconnetto il player
					if(player == sfidante) { //cancello la key del player dal main selector del server
						keySfidante.cancel(); 
					} else {
						keySfidato.cancel();
					}
					continue; //Passo alla prossima iterazione
				}
				if(player == sfidante) { //Setto l'interestOps del player nel main selector del server a OP_READ
					keySfidante.interestOps(SelectionKey.OP_READ);
				} else {
					keySfidato.interestOps(SelectionKey.OP_READ);
				}
			}
			Impegnati.ottieniImpegnati().eliminaCoppia(nickSfidato, nickSfidante); //I player non sono più impegnati
			ServerWQ.sveglia(); //Sveglio il main selector del server
			return; //return
		}
	}
}
