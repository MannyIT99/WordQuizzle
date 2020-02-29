import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

@SuppressWarnings("serial")
public class ImplementazioneRegistrazioneWQ extends RemoteServer implements RegistrazioneWQ {
	public String registra_utente(String nickUtente, String password) throws RemoteException {
		int res = Grafo.ottieniGrafo().aggiungiUtente(nickUtente, password);
		if(res == 10) {
			return "Errore - nickUtente già utilizzato da un altro utente";
		} else {
			return "Registrazione eseguita con successo";
		}
	}
}
