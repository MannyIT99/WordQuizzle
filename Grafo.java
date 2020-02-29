import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

//La classe Grafo rappresenta la struttura dati che mantiene tutte le informazioni sugli utenti del gioco
public class Grafo {
	//Utilizzato il pattern Singleton
	private static ConcurrentHashMap<String, Utente> grafo = null; //La struttura è implementata come una concurrentHashMap<String, Utente>
	//Ad ogni username è associato un oggetto utente
	private static Grafo myGrafo;
	
	//Costruttore
	private Grafo() {
		//Riferimento al file contenente la struttura dati in formato JSON
		File fileGrafo = null;
		try {
			//Provo ad aprire il file con il pathname specificato in ParametriCondivisi
			fileGrafo = new File(ParametriCondivisi.pathFileJSON);
		} catch(NullPointerException npe) {
			//Se viene sollevata l'eccezione NullPointerException, stampo l'eccezione e termino il server
			System.err.println("NullPointerException in Grafo: Grafo()");
			System.err.println(ParametriCondivisi.msg_server_end);
			System.exit(0);
		}
		//Try-with-resources: Apro un FileInputStream passando il riferimento al file json
		//e poi apro un FileChannel
		try(FileInputStream fis = new FileInputStream(fileGrafo);
				FileChannel fc = fis.getChannel()) {
			long size_inFile = fc.size(); //Ottengo la dimensione del file
			if(size_inFile == 0) { //Se la dimensione del file è uguale a 0
				grafo = new ConcurrentHashMap<String, Utente>(); //Inizializzo la struttura dati con una new concurrentHashMap
			} else { //Altrimenti
				char[] s = new char[(int)size_inFile]; //Creo un array di char di dimensione uguale alla dimensione del file json
				ByteBuffer bf = ByteBuffer.allocate((int)size_inFile); //Alloco il bytebuffer con la stessa dimensione del file json
				fc.read(bf); //Leggo tutto il file e lo scrivo nel buffer bf
				bf.flip(); //Cambio modalità
				for(int i=0; i<size_inFile; i++) {
					s[i] = (char)bf.get(); //Riempio l'array di char byte per byte
				}
				String stringaJSON = new String(s); //Converto l'array in stringa
				Gson gson = new Gson(); //Creo un oggetto Gson e deserializzo
				Type type = new TypeToken<ConcurrentHashMap<String, Utente>>(){}.getType();
				grafo = gson.fromJson(stringaJSON, type);
			}
		} catch(IOException ioe) {
			//Se sollevata la IOException, il server stampa l'eccezione e termina la sua esecuzione
			System.err.println("IOException in Grafo: Grafo()");
			System.err.println(ParametriCondivisi.msg_server_end);
			System.exit(0);
		}
	}
	
	//Metodo per ottenere un riferimento alla struttura dati
	public synchronized static Grafo ottieniGrafo() {
		if(grafo == null) { //Se il grafo è nullo
			myGrafo = new Grafo(); //viene chiamato il costruttore per inizializzarlo
		}
		return myGrafo; //Viene restituito il riferimento
	}
	
	//Questa funzione permette di salvare la struttura dati su file json (Serializzazione)
	public void serializza() {
		Gson gson = new Gson(); //Creo un oggetto Gson
		String jsonString = gson.toJson(grafo); //Converto la struttura dati in una stringa in formato JSON
		Path path = Paths.get(ParametriCondivisi.pathFileJSON); //Ottengo il riferimento al file con path specificato in ParametriCondivisi
		try {
			Files.write(path, jsonString.getBytes()); //Scrivo la stringa nel file
		} catch(IOException ioe) {
			//Se sollevata l'eccezione IOException, il server stampa l'eccezione e termina la sua esecuzione
			System.err.println("IOException in Grafo: serializza()");
			System.err.println(ParametriCondivisi.msg_server_end);
			System.exit(0);
		}
	}
	
	//Questa funzione permette di ottenere un utente
	public synchronized Utente ottieniUtente(String nickUtente) {
		//Passando il nickUtente, attraverso questo metodo è possibile ottenere l'oggetto di tipo Utente relativo al nickUtente
		return grafo.get(nickUtente);
	}
	
	//Questa funzione permette di aggiungere nodi al grafo (Per il comando registra_utente)
	public synchronized int aggiungiUtente(String nickUtente, String password) {
		if(esisteUtente(nickUtente)) { //Se esiste già un utente con il nickUtente passato
			return 10; //Errore 10: Esiste già un utente con questo nickUtente
		}
		//Non esiste un utente con questo nickUtente
		Utente utente = new Utente(password); //Quindi creo un nuovo oggetto Utente
		grafo.put(nickUtente, utente); //e aggiungo l'utente al grafo
		serializza(); //Serializzo
		return 0; //Utente aggiunto correttamente
	}
	
	//Questa funzione permette di verificare l'esistenza di un determinato utente
	public synchronized boolean esisteUtente(String nickUtente) {
		if(grafo.containsKey(nickUtente)) {
			return true; //Il grafo contiene già un nodo con quel nickUtente
		}
		return false; //Il grafo non contiene un nodo con quel nickUtente
	}
	
	public synchronized int aggiungiAmico(String nickUtente, String nickAmico) {
		if(!grafo.containsKey(nickAmico)) {
			return 10; //Errore 10: nickAmico non esistente
		}
		//nickAmico esistente
		if(esisteRelazione(nickUtente, nickAmico)) {
			return 20; //Errore 20: La relazione di amicizia esiste già
		}
		//Non esiste una relazione di amicizia
		//Aggiungo una relazione di amicizia
		grafo.get(nickUtente).aggiungiAmico(nickAmico);
		grafo.get(nickAmico).aggiungiAmico(nickUtente);
		serializza(); //Serializzo
		return 0; //Relazione di amicizia aggiunta correttamente
	}
	
	//Questa funzione permette di verificare l'esistenza di una relazione di amicizia tra due utenti
	public synchronized boolean esisteRelazione(String nickUtente, String nickAmico) {
		if(grafo.get(nickUtente).ottieniListaAmici().contains(nickAmico)) { //Se l'oggetto Utente associato alla nickUtente passato, contiene nella sua lista amici il nickAmico
			return true; //La relazione di amicizia esiste già
		}
		//altrimenti la relazione di amicizia non esiste
		return false;
	}
	
	//Questa funzione permette di ottenere la classifica relativa al nickUtente passato
	public synchronized LinkedHashMap<String, Integer> ottieniClassifica(String nickUtente) {
		HashMap<String, Integer> classifica = new HashMap<String, Integer>(); //Viene creata una hashMap<String, Integer> (nickUtente, Punteggio);
		classifica.put(nickUtente, ottieniUtente(nickUtente).ottieniPunteggioUtente()); //Al suo interno viene subito inserito il nickUtente passato e il suo punteggio
		for(String amico: ottieniUtente(nickUtente).ottieniListaAmici()) { //Viene eseguita la stessa azione per ogni amico
			classifica.put(amico, ottieniUtente(amico).ottieniPunteggioUtente()); //(Per ogni nickUtente all'interno della sua lista amici)
		}
		return ordinaHashMap(classifica); //Il metodo ritorna il valore restituito dalla funzione ordinaHashMap
	}
	
	//Questa funzione permette di ordinare una HashMap (Utilizzato nel caso di calcolo di classifica)
	public LinkedHashMap<String, Integer> ordinaHashMap(HashMap<String, Integer> classifica) {
		List<String> chiavi = new ArrayList<String>(classifica.keySet()); //Viene creata una lista di chiavi, inizializzata con tutti i nickUtenti all'interno della hashmap passata
		List<Integer> valori = new ArrayList<Integer>(classifica.values()); //Viene creata una lista di valori inizializzata con tutti i punteggi all'interno della hashmap passata
		Collections.sort(chiavi); //La lista di chiavi viene ordianta
		Collections.sort(valori); //La lista di valori viene ordinata
		LinkedHashMap<String, Integer> classificaOrdinata = new LinkedHashMap<String, Integer>(); //Viene inizializzata una linkedhashmap che conterrà la classifica ordinata
		Iterator<Integer> valoriIt = valori.iterator(); //Viene creato un oggetto iteratore della lista dei valori
		while(valoriIt.hasNext()) { //Fino a quando c'è un oggetto
			int valore = valoriIt.next(); //Ottengo il valore dell'oggetto (un intero)
			Iterator<String> chiaviIt = chiavi.iterator(); //Viene creato un oggetto iteratore della lista delle chiavi
			while(chiaviIt.hasNext()) { //Fino a quando c'è un oggetto
				String chiave = chiaviIt.next(); //Ottengo il valore dell'oggetto (una stringa)
				int comp1 = classifica.get(chiave); //Inizializzo comp1 con il punteggio associato al nickUtente a cui si riferisce chiave
				int comp2 = valore; //Inizializzo comp2 con il valore attuale dell'iteratore
				if(comp1 == comp2) { //Se sono uguali
					chiaviIt.remove(); //Rimuovo l'oggetto attuale dell'iteratore delle chiavi
					classificaOrdinata.put(chiave, valore); //e inserisco nella nuova linkedhashmap l'associazione tra chiave e valore
					break; //ed esco dal ciclo per passare al prossimo
				}
			}
		}
		return classificaOrdinata; //Infine restituisco la classificaOrdinata
	}
	
	//Questa procedura permette di aggiornare il punteggio di un player
	public synchronized void aggiornaPunteggio(SocketChannel socketClient, int punteggio) {
		grafo.get(StrutturaLogin.ottieniStrutturaLogin().ottieniUtente(socketClient)).aggiornaPunteggio(punteggio); //Aggiorno il punteggio del player con socketClient passato
		serializza(); //e serializzo
	}
}
