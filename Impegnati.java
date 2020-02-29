import java.util.ArrayList;
import java.util.List;

//Questa classe rappresenta la struttura dati che indica gli utenti impegnati in una sfida / richiesta di sfida
//Viene utilizzato il pattern Singleton
public class Impegnati {
	private static List<String> impegnati = null; //La struttura dati utilizzata è una lista di stringhe
	private static Impegnati myImpegnati;

	//Costruttore
	private Impegnati() {
		impegnati = new ArrayList<String>(); //la lista di stringhe è inizializzata come un arraylist di stringhe
	}

	//Questa metodo ritorna il riferimento alla struttura dati
	public synchronized static Impegnati ottieniImpegnati() {
		if(impegnati == null) { //Se la struttura dati è nulla
			myImpegnati = new Impegnati(); //Viene utilizzato il costruttore
		}
		return myImpegnati; //e ritornata la struttura dati
	}

	//Quessto metodo permette di aggiungere una coppia di player che si sfidanto
	public synchronized int aggiungiCoppia(String nickUtente1, String nickUtente2) {
		if(impegnati.contains(nickUtente2)) { //Se il nickUtente2, ovvero il nickUtente sfidato è impegnato
			return 1; //Ritorno 1
		}
		//Altrimenti
		impegnati.add(nickUtente1); //nickUtente1
		impegnati.add(nickUtente2); //e nickUtente2 vengono aggiunti alla struttura dati
		return 0; //Ora entrambi sono impegnati
	}

	//Questo metodo permette di eliminare una coppia di utenti precedentemente impegnata in una sfida
	public synchronized void eliminaCoppia(String nickUtente1, String nickUtente2) {
		impegnati.remove(nickUtente1); //Rimuovo nickUtente1
		impegnati.remove(nickUtente2); //e nickUtente2 dalla struttura dati
	}
	
	public void stampa() {
		for(String a: impegnati) {
			System.out.print(a + " ");
		}
	}
}
