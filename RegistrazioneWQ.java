import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrazioneWQ extends Remote {
	public String registra_utente(String nickUtente, String password) throws RemoteException;
}
