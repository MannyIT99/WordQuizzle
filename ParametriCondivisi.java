//La classe ParametriCondivisi, contiene tutte le stringhe utilizzate all'interno delle classi che fanno
//riferimento al client e al server
public class ParametriCondivisi {
	//Nome
	static String main_nome = "Word Quizzle"; //Nome dell'applicazione
	
	//Parametri
	static int portaServizioRegistrazione = 1999; //Porta utilizzata per il servizio di registrazione
	static String nomeServizioRegistrazione = "REGISTRAZIONE-WQ-SERVICE"; //Nome utilizzato per il servizio di registrazione
	static String pathFileJSON = "fileGrafo.json"; //Path del file JSON contenente tutta la struttura dati con le informazioni sugli utenti
	static String serverAddress = "localhost"; //Indirizzo standard del server
	static int serverPort = 1997; //Porta standard del server
	static int T1 = 10000; //Millisecondi relativi al tempo disponibile per rispondere ad una richiesta di sfida
	static int T2 = 60000; //Millisecondi relativi al tempo totale di una partita
	static int X = 2; //Numero di punti assegnati per ogni parola tradotta correttamente
	static int Y = 1; //Numero di punti sottratti per ogni parola tradotta in modo errato
	static int Z = 3; //Numero di punti extra assegnati al vincitore della partita
	static int K = 5; //Numero di parola da tradurre per ogni partita
	static String pathFileParole = "parole.txt"; //Path del file contenente le parole
	
	//Comandi
	static final String cmd_login = "login"; //Comando login
	static final String cmd_registra_utente = "registra_utente"; //Comando registra_utente
	static final String cmd_logout = "logout"; //Comando logout
	static final String cmd_aggiungi_amico = "aggiungi_amico"; //Comando aggiungi_amico
	static final String cmd_lista_amici = "lista_amici"; //Comando lista_amici
	static final String cmd_mostra_punteggio = "mostra_punteggio"; //Comando mostra_punteggio
	static final String cmd_mostra_classifica = "mostra_classifica"; //Comando mostra_classifica
	static final String cmd_sfida = "sfida"; //Comando sfida
	static final String cmd_err = "Errore nell'invio del comando."; //Stampato nel caso di un errore nella digitazione del comando
	static final String cmd_nex = "Il comando digitato non esiste."; //Stampato nel caso di un comando inesistente
	
	//Messaggi
	static String msg_client_end = "Client terminato!"; //Stampato se sollevata un'eccezione che arresta il client
	static String msg_server_end = "Server terminato!"; //Stampato se sollevata un'eccezione che arresta il server
	static String msg_report = "REPORT"; //Report
	static String msg_continue_after_challenge = "PREMI INVIO PER CONTINUARE"; //Stampato per tornare al prompt dei comandi dopo una sfida
	static String msg_pre_word = "Challenge "; //Stampato prima di ogni parola da tradurre
	static String msg_null_response = "No, la risposta non può essere nulla!"; //Stampato nel caso di risposta nulla durante la sfida
	static String msg_sleep = "Attendi..."; //Stampato dopo aver tradotto tutte le parole di una sfida
	static String msg_req_reg = "Per registrare un nuovo utente, bisogna prima effettuare il logout."; //Stampato se si cerca di registrare un utente mentre si è loggati
	static String msg_req_login = "Devi prima effettuare il login."; //Stampato nel caso di richieste di comandi che prevedono il login
	static String msg_log_log = "Sei già loggato. Per effettuare il login con un altro utente, eseguire prima il logout."; //Stampato nel caso di login login mentre si è già loggati
	static String msg_req_logout = "Per effettuare il logout, è necessario essere prima loggati."; //Stampato nel caso si cerchi di logout senza essere loggati
	static String msg_no_friends = "La tua lista di amici è vuota."; //Stampato nel caso di lista di amici vuota
	static String msg_no_solo_challenge = "Non puoi sfidarti da solo."; //Stampato quando l'utente che si desidera sfidare è lo stesso che richiede la sfida
	static String msg_resp2challenge = "La risposta ad una sfida deve essere si o no."; //Stampato se la risposta alla richiesta di sfida è diversa da si o no
	
	//Usage
	static String usg_reg = "USAGE: registra_utente <nickUtente> <password>"; //Messaggio di usage per registra_utente
	static String usg_login = "USAGE: login <nickUtente> <password>"; //Messaggio di usage per login
	static String usg_aggiungi_amico = "USAGE: aggiugni_amico <nickAmico>"; //Messaggio di usage per aggiungi_amico
	static String usg_sfida = "USAGE: sfida <nickAmico>"; //Messaggio di usage per sfida
	
	//Messaggi Client-Server
	static String msg_ok_login = "Login eseguito con successo."; //Messaggio che indica che il login è stato effettuato con successo
	static String msg_double_conn = "Un utente con questo nickUtente è già connesso."; //Stampato quando si cerca di effettuare il login con un nickName già loggato
	static String msg_no_nick = "nickUtente inesistente."; //Stampato quando si cerca di effettuare il login con un nickName inesistente
	static String msg_psw_err = "Password non corretta."; //Stampato quando nel login, il nickUtente è corretto ma la password no
	static String msg_ok_logout = "Logout eseguito con successo."; //Messaggio che indica che il logout è stato effettuato con successo
	static String msg_err_logout = "Disconnessione utente non avvenuta con successo."; //Messaggio che indica che il logout non è stato effettuato correttamente
	static String msg_no_friend_nick = "nickAmico inesistente."; //Stampato quando si cerca di aggiungere un amico inesistente
	static String msg_no_friendship = "Il giocatore che vuoi sfidare non è tuo amico."; //Stampato quando si cerca di sfidare un utente non amico
	static String msg_player_offline = "Il giocatore che vuoi sfidare non è online."; //Stampato quando si cerca di sfidare un utente non online
	static String msg_player_in_game = "L'utente che desideri sfidare è già impegnato."; //Stampato quando si cerca di sfidare un utente che è già impegnato in un'altra sfida
	static String msg_invite_off = "Il client che hai sfidato non è più online."; //Stampato quando il player sfidato torna offline
}
