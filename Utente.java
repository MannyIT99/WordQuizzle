import java.util.ArrayList;
import java.util.List;

//Questa classe modella l'utente
public class Utente {
	//Ogni utente contiene
	String password; //una password
	int punteggio_utente; //un punteggio utente
	List<String> amici; //una lista di amici

	//Costruttore
	public Utente(String password) {
		this.password = password; //Inizializzata la password con quella passata al costruttore
		this.punteggio_utente = 0; //Inizializzato il punteggio utente a 0
		this.amici = new ArrayList<String>(); //Inizializzata la lista di amici con un arraylist di stringhe
	}

	//Questa funzione permette di ottenere la password
	public String ottieniPassword() {
		return this.password;
	}
	
	//Questa funzione permette di aggiornare il punteggio dell'utente
	public void aggiornaPunteggio(int punteggio) {
		this.punteggio_utente = this.punteggio_utente + punteggio;
	}

	
	
	//Questa funzione permette di ottenere il punteggio_utente
	public int ottieniPunteggioUtente() {
		return this.punteggio_utente;
	}
	
	//Questa funzione permette di ottenere la lista di amici
	public List<String> ottieniListaAmici() {
		return this.amici;
	}
	
	//Questa funzione permette di aggiungere amici alla lista di amici
	public void aggiungiAmico(String nickAmico) {
		this.amici.add(nickAmico);
	}
	
	@Override
	public String toString() {
		StringBuilder stringa = new StringBuilder();
		stringa.append("Utente [password=" + this.password + ", punteggio_utente=" + this.punteggio_utente + ", amici=[");
		for(int i=0; i<amici.size(); i++) {
			if(i==amici.size()-1) {
				stringa.append(amici.get(i));
			} else {
				stringa.append(amici.get(i) + ", ");
			}
		}
		stringa.append("]]");
		return stringa.toString();
	}
}
