import java.nio.channels.SocketChannel;
import java.util.HashMap;

//Questa classe modella la struttura dati contenente gli utenti attualemnte connessi
//Utilizzato il pattern singleton
public class StrutturaLogin {
	private static HashMap<SocketChannel, String> connessi = null; //La struttura dati è una hashmap<SocketChannel, String>
	//SocketChannel e nickUtente al quale il socketChannel si riferisce
	private static StrutturaLogin myLogin; //Riferimento
	
	//Costruttore
	public StrutturaLogin() {
		connessi = new HashMap<SocketChannel, String>(); //La struttura dati è inizializzata
	}
	
	//Questo metodo permette di ottenere il riferimento alla struttura dati
	public synchronized static StrutturaLogin ottieniStrutturaLogin() {
		if(connessi == null) { //Se la struttura dati è nulla, viene inizializzata
			myLogin = new StrutturaLogin();
		}
		return myLogin; //e viene restituita
	}
	
	//Questo metodo permette di ottenre il socketChannel relativo al nickUtente passato
	public SocketChannel ottieniSocketChannel(String nickUtente) {
		for(SocketChannel sc: connessi.keySet()) { //Per ogni socketChannel 
			if(connessi.get(sc).equals(nickUtente)) { //se il socketChannel è associato al nickUtente passato
				return sc; //ritorno il socketChannel
			}
		}
		return null; //Altrimenti non ritorna nulla (Non viene mai effettuata questa operazione)
	}
	
	//Questo metodo permette di connettere un utente, ovvero, aggiungere un nuovo elemento alla struttura datu
	public synchronized int connettiUtente(SocketChannel sc, String nickUtente, String password) {
		if(utenteConnesso(nickUtente)) { //Se il nickUtente è già connesso
			return 10; //Errore 10 - Utente con questo nickName già connesso
		}
		if(!Grafo.ottieniGrafo().esisteUtente(nickUtente)) { //Se nel grado non esiste un utente con questo nickUtente
			return 20; //Errore 20 - Utente inesistente
		}
		if(!Grafo.ottieniGrafo().ottieniUtente(nickUtente).ottieniPassword().equals(password)) { //Se la password passata è sbagliata
			return 30; //Errore 30 - Password non corretta
		}
		connessi.put(sc, nickUtente); //Altrimenti inserisco correttamente l'utente nella struttura dati
		return 0; //Utente connesso
	}
	
	//Questo metodo permette di verificare se l'utente passato è online oppure no
	public synchronized boolean utenteConnesso(String nickUtente) {
		for(SocketChannel sc: connessi.keySet()) { //Per ogni socketChannel all'interno della struttura dati dei connessi
			if(connessi.get(sc).equals(nickUtente)) { //se esiste un socketChannel relativo al nickUtente passato
				return true; //Utente già connesso
			}
		}
		return false; //altrimenti utente non ancora connesso
	}
	
	//Questo metodo permette di eliminare un utente dalla struttura dati
	public synchronized int disconnettiUtente(SocketChannel sc) {
		connessi.remove(sc); //Rimuovo l'elemento dalla struttura dati
		return 0; //Utente disconnesso correttamente;
	}
	
	//Questo metodo permette di verificare se l'utente è già connesso
	public synchronized boolean connessioneExtra(SocketChannel sc) {
		if(!connessi.containsKey(sc)) { //Se il socketChannel passato non è contenuto nella struttura dati
			return false; //l'utente non è già connesso
		}
		return true; //altrimenti l'utente è già connesso
	}
	
	//Questo metodo permette di ottenere il nickUtente associato al socketChannel passato
	public synchronized String ottieniUtente(SocketChannel sc) {
		return connessi.get(sc);
	}
}
