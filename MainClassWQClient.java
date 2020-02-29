import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Scanner;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.ConnectException;

//Questa classe modella il client di Word Quizzle
public class MainClassWQClient {
	static int K = ParametriCondivisi.K; //Numero di parole da tradurre
	private static boolean loggato = false; //Booleano che indica se il client è loggato oppure no
	private static final int DIM_BYTEBUFFER = 10240; //Dimensione di ByteBuffer
	private static String myNick = null; //Riferimento al nickUtente attualemente loggato
	private static StringTokenizer st_command; //StringTokenizer per il comando da inviare al server
	private static String indirizzoServer; //Indirizzo del server
	private static int portaServer; //Porta del server
	private static DatagramSocket datagramSocket = null; //DatagramSocket
	static SfidaUDPClient sudpc = null; //SfidaUDPClient
	static SfidaRicevuta sfidaRicevuta = null; //SfidaRicevuta
	private static SocketChannel socketClient = null; //Socket client
	private static Scanner scanner = null; //Scanner per input da stdin

	//Classe utile per gestire la fine delle sfide
	static class FineSfida {
		static boolean fineSfida = false; //Inizializzata a false
		//Questo metodo ritorna il valore di fineSfida
		static synchronized boolean getFineSfida() {
			return fineSfida;
		}
		//Questo metodo setta la fine della sfida a true
		static synchronized void setFineSfida() {
			fineSfida = true;
		}
		//Questo metodo resetta la fine della sfida a false
		static synchronized void resetFineSfida() {
			fineSfida = false;
		}
	}

	//Thread avviato durante una sfida, in continua lettura
	static class LeggiSfida implements Runnable {
		@Override
		public void run() {
			ByteBuffer byteBuffer;
			int i = 0;
			int r = -1;
			try {
				while(true) { //Ciclo
					byteBuffer = ByteBuffer.allocate(512); //Alloco un byteBuffer
					try {
						r = socketClient.read(byteBuffer); //Leggo dal server
					} catch (IOException e) {
						//Se sollevato IOException
						stampa_errore("IOException in LeggiSfida: run [socketClient.read()]");
						try {
							socketClient.close(); //chiudo il socketClient
						} catch (IOException e1) {
							//Se sollevato IOException
							stampa_errore("IOException in LeggiSfida: run [socketClient.close()]");
						}
						//Il client termina la sua esecuzione
						stampa_errore(ParametriCondivisi.msg_client_end);
						System.exit(0);
					}
					if(r == -1) { //Se IOException non viene sollevato ma vengono letti -1 caratteri
						stampa_errore("Letti -1 caratteri in in LeggiSfida: run [socketClient.read()]");
						try {
							socketClient.close(); //chiudo il socketClient
						} catch (IOException e1) {
							//Se sollevato IOException
							stampa_errore("IOException in LeggiSfida: run [socketClient.close()]");
						}
						//Il client termina la sua esecuzione
						stampa_errore(ParametriCondivisi.msg_client_end);
						System.exit(0);
					}
					String ricevuto = ""; //Altrimenti creo una stringa che conterrà il contenuto ricevuto dal server
					try {
						ricevuto = new String(byteBuffer.array(), "UTF-8").trim(); //La inizializzo col contenuto del byteBuffer
					} catch (UnsupportedEncodingException e) {
						//Se sollevato UnsupportedEncodingException, il client termina ala sua esecuzione
						stampa_errore("UnsupportedEncodingException in LeggiSfida: run [... new String(..., \"UTF-8\"]");
						stampa_errore(ParametriCondivisi.msg_client_end);
						System.exit(0);
					}
					if(ricevuto.contains(ParametriCondivisi.msg_report)) { //Se contiene un messaggio di REPORT
						FineSfida.setFineSfida(); //Setto la fine della sfida
						stampa_messaggio("\n" + ricevuto); //stampo il messaggio ricevuto
						stampa_messaggio("\n" + ParametriCondivisi.msg_continue_after_challenge); //Chiedo di premere invio per tornare al prompt dei comandi
						return; //return
					} else { //Altrimenti
						stampa_messaggio(ParametriCondivisi.msg_pre_word + " " + (i+1) + "/" + K + ": " + ricevuto); //Indico che è una parola da tradurre
						i++; //e incremento il numero della parola
					}
				}
			} catch(Exception e) { }
		}
	}

	//Thread che permette di inviare la traduzione delle parole ricevute da parte del server
	static class ScriviSfida implements Runnable {
		@Override
		public void run() {
			try {
				String traduzione = null; //Stringa che conterrà la traduzione
				ByteBuffer byteBuffer = null; //Bytebuffer da inviare al server

				for(int i=0; i<K; i++) { //Ciclo un numero di volte pari al numero di parole da tradurre
					while(true) { //ciclo infinito
						traduzione = scanner.nextLine(); //Input da stdin
						if(FineSfida.getFineSfida()) { //Se la sfida è finita
							FineSfida.resetFineSfida(); //resetto la fineSfida a false
							return; //return
						}
						if("".equals(traduzione)) { //se la traduzione è nulla
							stampa_errore(ParametriCondivisi.msg_null_response); //stampo l'errore
						} else { //altrimenti esco dal ciclo infinito
							break;
						}
					}
					byteBuffer = ByteBuffer.wrap(traduzione.getBytes()); //nel bytebuffer inserisco la traduzione della parola
					try {
						socketClient.write(byteBuffer); //la scrivo al server
					} catch (IOException e) {
						//Se sollevato IOException
						stampa_errore("IOException in ScriviSfida: run [socketClient.write()]");
						try {
							socketClient.close(); //chiudo il socketClient
						} catch (IOException e1) {
							//Se sollevato IOException
							stampa_errore("IOException in LeggiSfida: run [socketClient.close()]");
						}
						//Il client termina la sua esecuzione
						stampa_errore(ParametriCondivisi.msg_client_end);
						System.exit(0);
					}
				}
				stampa_messaggio("\n" + ParametriCondivisi.msg_sleep); //Attendo...
				while(!FineSfida.getFineSfida()) { //Ciclo fino a quando non finisce la sfida
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						stampa_errore("InterruptedException in ScriviSfida: run [Thread.sleep()]");
						System.exit(0);
					}
				}
				FineSfida.resetFineSfida(); //resetto fineSfida a false
				traduzione = scanner.nextLine(); //Richiedo invio per tornare al prompt dei comandi
			} catch(Exception e) { }
		}
	}

	//Questo metodo è utilizzato per la stampa dei messaggi
	static void stampa_messaggio(String messaggio) {
		System.out.flush();
		System.out.println(messaggio);
	}

	//Questo metodo è utilizzato per la stampa degli errori
	static void stampa_errore(String messaggio) {
		System.err.flush();
		System.err.println(messaggio);
	}

	//Questo metodo è utilizzato per la registrazione di utenti
	static void registrazione(String nickUtente, String password) throws RemoteException, NotBoundException {
		try {
			RegistrazioneWQ serverRegistrazioneWQ;
			Remote serverRemoto;
			Registry reg = LocateRegistry.getRegistry(ParametriCondivisi.portaServizioRegistrazione);
			serverRemoto = reg.lookup(ParametriCondivisi.nomeServizioRegistrazione);
			serverRegistrazioneWQ = (RegistrazioneWQ) serverRemoto;
			stampa_messaggio(serverRegistrazioneWQ.registra_utente(nickUtente, password));
		} catch(ConnectException ce) {
			stampa_errore("ConnectException in MainClassWQClient: registrazione()");
		}
	}

	//Questo metodo è utilizzato per la sfida
	public static void sfida() {
		stampa_messaggio("\nVia alla sfida di traduzione!\n"
				+ "Hai " + (ParametriCondivisi.T2/1000) + " secondi per tradurre correttamente " + K + " parole");
		Thread tLeggi = new Thread(new LeggiSfida()); //Avvio un thread per la lettura dei messaggi dal server
		Thread tScrivi = new Thread(new ScriviSfida()); //Avvio un thread per la scrittura delle traduzioni
		tLeggi.start();
		tScrivi.start();
		try {
			tLeggi.join(); //Attendo la fine del thread di lettura
			tScrivi.join(); //Attendo la fine del thread di scrittura
		} catch (InterruptedException e) {
			//Se InterruptedException il client termina la sua esecuzione
			stampa_errore("Interrupted Exception in MainClassWQClient: sfida()");
			stampa_errore(ParametriCondivisi.msg_client_end);
			System.exit(0);
		}
		sfidaRicevuta.resetSfida(); //Resetto la sfida ricevuta
	}

	//Questo metodo è utilizzato per l'invio di comandi
	static public String inviaComando(String comando) {
		byte[] messaggio = new String(comando).getBytes(); //Inizializzo un array di byte con il comando da inviare
		ByteBuffer byteBuffer = ByteBuffer.wrap(messaggio); //Inizializzo il byteBuffer con il comando da inviare
		try {
			socketClient.write(byteBuffer); //Invio il comando al server
		} catch (IOException e) {
			//Se sollevata IOException
			System.err.println("IOException in MainClassWQClient: inviaComando() [socketClient.write()]");
			try {
				socketClient.close(); //Chiudo il socketClient
			} catch (IOException e1) {
				//Se sollevata IOException
				stampa_errore("IOException in MainClassWQClient: inviaComando() [socketClient.close()]");
			}
			//Il client termina la sua esecuzione
			stampa_errore(ParametriCondivisi.msg_client_end);
			System.exit(0);
		}
		byteBuffer = ByteBuffer.allocate(DIM_BYTEBUFFER); //Alloco di nuovo il byteBuffer;
		int numRead = -1;
		try {
			numRead = socketClient.read(byteBuffer); //Leggo la risposta dal server
		} catch (IOException e) {
			//Se IOException
			stampa_errore("IOException in MainClassWQClient: inviaComando() [socketClient.read()]");
			try {
				socketClient.close(); //Chiudo il socketClient
			} catch (IOException e1) {
				//Se IOException
				stampa_errore("IOException in MainClassWQClient: inviaComando() [socketClient.close()]");
			}
			//il client termina la sua esecuzione
			stampa_errore(ParametriCondivisi.msg_client_end);
			System.exit(0);
		}
		if(numRead == -1) { //Se non viene sollevata IOException ma vengono letti -1 caratteri
			System.out.println("Letti -1 caratteri in MainClassWQClient: inviaComando() [socketClient.read()");
			try {
				socketClient.close(); //chiudo il socketClient
			} catch (IOException e) {
				//Se IOException
				System.err.println("IOException in MainClassWQClient: inviaComando() [socketClient.close()]");
			}
			stampa_errore(ParametriCondivisi.msg_client_end);
			System.exit(0);
			//il client termina la sua esecuzione
		}
		return new String(byteBuffer.array()).trim(); //ritorno il messaggio restituito dal server
	}

	//Questo metodo è utilizzato per elaborare il comando inviato
	static public void elaboraComando(String comando) {
		st_command = new StringTokenizer(comando); //StringTokenizer per il comando
		if(st_command.hasMoreTokens()) { //Se ci sono token
			String finalCommand = st_command.nextToken(); //Il primo token è il nome del comando
			if(finalCommand.equals(ParametriCondivisi.cmd_registra_utente)) { //Se registra_utente
				//REGISTRAZIONE
				if(loggato == true) { //Se si è loggati
					stampa_errore(ParametriCondivisi.msg_req_reg); //Non è possibile registrare utenti
				} else { //altrimenti
					try {
						registrazione(st_command.nextToken(), st_command.nextToken()); //chiamo il metodo per la registrazione
					} catch(NoSuchElementException nsee) {
						//Se NoSuchElementException
						stampa_errore(ParametriCondivisi.usg_reg);
					} catch(RemoteException re) {
						//o se RemoteException
						stampa_errore("RemoteException in MainClassWQClient: elaboraComando() [registra_utente]");
					} catch(NotBoundException nbe) {
						//o se NotBoundException
						stampa_errore("NotBoundException in MainClassWQClient: elaboraComando() [registra_utente]");
					}
					//viene stampato il relativo errore
				}
			} else if(finalCommand.equals(ParametriCondivisi.cmd_login)) { //Se login
				//LOGIN
				if(loggato == true) { //Se si è loggati
					stampa_errore(ParametriCondivisi.msg_log_log); //Non è possibile effettuare di nuovo il login
				} else { //Altrimenti
					String nickUtente; 
					try {
						nickUtente = st_command.nextToken(); //Ottengo il nickutente dal secondo token
						String risposta = inviaComando(ParametriCondivisi.cmd_login + " " + nickUtente + " " + st_command.nextToken()); //invio il comando e ottengo la risposta
						stampa_messaggio(risposta); //stampa la risposta
						if(risposta.equals(ParametriCondivisi.msg_ok_login)) { //Se OK
							sfidaRicevuta = new SfidaRicevuta(); //Inizializzo sfidaRicevuta
							loggato = true; //loggato diventa true
							myNick = nickUtente; //il nickUtente corrente viene settato
							sudpc = new SfidaUDPClient(sfidaRicevuta, nickUtente, datagramSocket);
							Thread t = new Thread(sudpc);
							t.start(); //viene avviato un thread in ascolto per le richieste
						}
					} catch(NoSuchElementException nsee) {
						//Se NoSuchElemenetException viene stampato il relativo errore
						stampa_errore(ParametriCondivisi.usg_login);
					}
				}
			} else if(finalCommand.equals(ParametriCondivisi.cmd_logout)) { //Se logout
				//LOGOUT
				if(loggato == false) { //Se non si è loggati
					//Viene stampato il relativo errore
					stampa_errore(ParametriCondivisi.msg_req_logout);
				} else { //Altrimenti
					String risposta = inviaComando(ParametriCondivisi.cmd_logout); //Viene inviato il comando
					stampa_messaggio(risposta); //Viene stampata la risposta
					sfidaRicevuta.resetSfida(); //Viene resettata la sfida
					loggato = false; //loggato diventa false
					myNick = null; //il riferimento al nick è nullo
					sudpc.uccidi(); //viene terminato il thread in ascolto per le richieste
				}
			} else if(finalCommand.equals(ParametriCondivisi.cmd_mostra_punteggio)) { //Se mostra_punteggio
				//MOSTRA PUNTEGGIO
				if(loggato == false) { //Se non si è loggati
					stampa_errore(ParametriCondivisi.msg_req_login); //Viene stampato il relativo errore
				} else { //Altrimenti
					stampa_messaggio(inviaComando(ParametriCondivisi.cmd_mostra_punteggio)); //Si invia il comando e si stampa il punteggio
				}
			} else if(finalCommand.equals(ParametriCondivisi.cmd_lista_amici)) { //Se lista_amici
				//LISTA AMICI
				if(loggato == false) { //Se non si è loggati
					stampa_errore(ParametriCondivisi.msg_req_login); //Si stampa il relativo errore
				} else { //altrimenti
					String jsonString = inviaComando(ParametriCondivisi.cmd_lista_amici); //si invia il comando e si ottiene la risposta in formato JSON
					Gson gson = new Gson(); //Si crea un oggetto Gson
					List<String> amici = gson.fromJson(jsonString, new TypeToken<List<String>>(){}.getType()); //Si deserializza
					if(amici.isEmpty()) { //Se la lista è vuota
						stampa_messaggio(ParametriCondivisi.msg_no_friends); //Viene stampato il relativo avviso
					} else { //Altrimenti vengono stampati tutti gli amici
						for(int i=0; i<amici.size(); i++) {
							if(i == amici.size()-1) {
								System.out.println(amici.get(i));
							} else {
								System.out.print(amici.get(i) + ", ");
							}
						}
					}
				}
			} else if(finalCommand.equals(ParametriCondivisi.cmd_mostra_classifica)) { //Se mostra_classifica
				//CLASSIFICA
				if(loggato == false) { //Se non si è loggati
					stampa_errore(ParametriCondivisi.msg_req_login); //Si stampa il relativo errore
				} else { //altrimenti
					String jsonString = inviaComando(ParametriCondivisi.cmd_mostra_classifica); //Si invia il comando e si ottiene la risposta in formato JSON
					Gson gson = new Gson(); //Si crea l'oggetto Gson
					Type type = new TypeToken<LinkedHashMap<String, Integer>>(){}.getType(); 
					LinkedHashMap<String, Integer> classificaOrdinata = gson.fromJson(jsonString, type); //Si deserializza
					List<String> elementi = new ArrayList<String>(classificaOrdinata.keySet()); //Si crea una lista di stringhe contenente la classifica ordinata
					int lunghezzaClassifica = elementi.size() - 1;
					for(int i=lunghezzaClassifica; i>=0; i--) {  //Si stampa la classifica
						String elemento = elementi.get(i);
						if(i == 0) {
							System.out.println(elemento + " " + classificaOrdinata.get(elemento));
						} else {
							System.out.print(elemento + " " + classificaOrdinata.get(elemento) + ", ");
						}
					}
				}
			} else if(finalCommand.equals(ParametriCondivisi.cmd_aggiungi_amico)) { //Se aggiungi_amico
				//AGGIUNGI AMICO
				if(loggato == false) { //se non si è loggati
					stampa_errore(ParametriCondivisi.msg_req_login); //si stampa il relativo errore
				} else { //altrimenti
					try {
						stampa_messaggio(inviaComando(ParametriCondivisi.cmd_aggiungi_amico + " " + st_command.nextToken())); //si invia il comando e si stampa la risposta
					} catch(NoSuchElementException nsee) {
						//Se NoSuchElementException
						stampa_errore(ParametriCondivisi.usg_aggiungi_amico); //Si stampa il relativo errore
					}
				}
			} else if(finalCommand.equals(ParametriCondivisi.cmd_sfida)) { //Se sfida
				//SFIDA
				if(loggato == false) { //se non si è loggati
					stampa_errore(ParametriCondivisi.msg_req_login); //si stampa il relativo errore
				} else { //altrimenti
					try {
						String avversario = st_command.nextToken(); //si ottiene il nome dello sfidato dal secondo token
						if(avversario.equals(myNick)) { //Se il nome dello sfidato è lo stesso dello sfidante
							stampa_errore(ParametriCondivisi.msg_no_solo_challenge); //si stampa il relativo errore
						} else { //altrimenti
							String risposta = inviaComando(ParametriCondivisi.cmd_sfida + " " + avversario); //viene inviato il comando
							stampa_messaggio(risposta); //Si stampa la risposta
							if(risposta.equals(avversario + " ha accettato la tua sfida")) { //Se lo sfidato ha accettato la sfida
								sfida(); //Si chiama il metodo sfida()
							}
						}
					} catch(NoSuchElementException nsee) {
						//Se NoSuchElementException
						stampa_errore(ParametriCondivisi.usg_sfida); //Si stampa il relativo errore
					}
				}
			} else { //Errore relativo a un comando non esistente
				stampa_errore(ParametriCondivisi.cmd_nex);
			}
		} else { //Errore relativo alla digitazione del comando
			stampa_errore(ParametriCondivisi.cmd_err);
		}
	}

	//Main
	public static void main(String[] args) throws RemoteException, NotBoundException, UnknownHostException {
		try {
			if(args.length == 1) { //Se il numero di argomenti da linea di comando è uno
				if(args[0].equals("--help")) { //e se è uguale a --help, viene stampato l'USAGE
					stampa_messaggio("USAGE: COMMAND [ARGS...]\n"	+
							"Commands:\n" +
							"    - Registra l'utente\n" +
							"          registra_utente <nickUtente> <password>\n" +
							"    - Effettua il login\n" +
							"          login <nickUtente> <password>\n" +
							"    - Effettua il logout\n" +
							"          logout\n" +
							"    - Crea relazione di amicizia con nickUtente\n" +
							"          aggiungi_amico <nickUtente>\n" +
							"    - Mostra la lista dei propri amici\n" +
							"          lista_amici\n" +
							"    - Richiesta di una sfida a nickAmico\n" +
							"          sfida <nickUtente>\n" +
							"    - Mostra il punteggio dell'utente\n" +
							"          mostra_punteggio\n" +
							"    - Mostra una classifica degli amici dell'utente (incluso l'utente stesso)\n" +
							"          mostra_classifica");
				}
				return;
			} else { //altrimenti
				//Si settano i parametri per la connessione col server
				try {
					indirizzoServer = args[0];
					try {
						portaServer = Integer.parseInt(args[1]);
					} catch(NumberFormatException nfe) {
						portaServer = ParametriCondivisi.serverPort;
					}
				} catch(ArrayIndexOutOfBoundsException aioobe) {
					//Se ArrayIndexOutOfBoundsException, i parametri restano quelli di default
					indirizzoServer = ParametriCondivisi.serverAddress;
					portaServer = ParametriCondivisi.serverPort;
				}

				stampa_messaggio(ParametriCondivisi.main_nome); //Stampo il nome del programma

				//Mi connetto al server
				InetSocketAddress serverAddress = new InetSocketAddress(indirizzoServer, portaServer);
				try {
					socketClient = SocketChannel.open(serverAddress);
				} catch (IOException e) {
					//Se IOException il client termina la sua esecuzione
					System.err.println("IOException in MainClassWQClient: Main [SocketChannel.open()]");
					System.err.println(ParametriCondivisi.msg_client_end);
					return;
				}
				try {
					//Inizializzo la datagram socket per la ricezione delle richieste di sfida
					datagramSocket = new DatagramSocket(socketClient.socket().getLocalPort());
				} catch (SocketException e) {
					//Se socketException il client termina la sua esecuzione
					System.err.println("SocketException in MainClassWQClient: Main [... new DatagramSocket()]");
					System.err.println(ParametriCondivisi.msg_client_end);
					return;
				}

				scanner = new Scanner(System.in); //Inizializzo l'oggetto scanner

				while(true) { //Ciclo infinitamente
					System.out.print("\n> ");
					String command = scanner.nextLine(); //Digito il comando da stdin
					if(loggato == true) { //Se sono loggato
						if(sfidaRicevuta.esisteSfida()) { //Se esiste una richiesta di sfida
							try {
								if(command.equals("si") ||  command.equals("Si") || command.equals("sI") || command.equals("SI")) { //Se la risposta è SI
									String risp2Server = new String("SI");
									socketClient.write(ByteBuffer.wrap(risp2Server.getBytes())); //Invio la risposta al server
									//AVVIA SFIDA
									sfida(); //chiamo il metodo per la gestione di una sfida
								} else if(command.equals("no") || command.equals("No") || command.equals("nO") || command.equals("NO")) { //altrimenti
									sfidaRicevuta.resetSfida(); //resetto la richiesta di sfida
									String risp2Server = new String("NO"); 
									socketClient.write(ByteBuffer.wrap(risp2Server.getBytes())); //Invio la risposta al server
								} else { //altrimenti
									stampa_errore(ParametriCondivisi.msg_resp2challenge); //La risposta deve essere si o no
								}
							} catch(IOException ioe) {
								//Se IOException il client termina la sua esecuzione
								stampa_errore("IOException in MainClassWQClient: Main [socketClient.write()]");
								System.err.println(ParametriCondivisi.msg_client_end);
								return;
							}
							continue;
						}
					}
					elaboraComando(command); //altrimenti elaboro il comando
				}
			}
		} catch(Exception e) { }
	}
}

