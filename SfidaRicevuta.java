//Questa classe permette di gestire le richieste di sfide 
public class SfidaRicevuta {
	static class Sfida { //La classe Sfida contiene  
		static String nomeSfidante; //il nome dello sfidante
		static int codiceSfida; //e il codice della sfida
	}

	//Thread che si occupa di eliminare la richiesta di sfida dopo ParametriCondivisi.T1 millisecondi
	class ThreadEliminaSfida implements Runnable {
		String sfidantePassato; //Stringa contenente il riferimento allo sfidante 
		int codiceSfidaPassato; //Numero di sfida al momento della richiesta di sfida relativa allo sfidantePassato
		//Costruttore
		public ThreadEliminaSfida(String sfidantePassato, int codiceSfidaPassato) {
			this.sfidantePassato = sfidantePassato; //Inizializzato lo sfidantePassato
			this.codiceSfidaPassato = codiceSfidaPassato; //Inizializzato il codiceSfidaPassato
		}
		@SuppressWarnings("static-access")
		@Override
		public void run() {
			//Il thread resta in attesa per ParametriCondivisi.T1 millisecondi
			try {
				Thread.currentThread().sleep(ParametriCondivisi.T1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//e dopo controlla se i parametri passati alla creazione del thread sono uguali a quelli attuali
			if(checkSfidaUguale(sfidantePassato, codiceSfidaPassato)) {
				//e nel caso resetta la sfida
				resetSfida();
			}
		}
	}

	//Questo metodo permette di capire se i parametri passati siano ugauli a quelli attuali
	public synchronized boolean checkSfidaUguale(String sfidantePassato, int codiceSfidaPassato) {
		if(esisteSfida() && Sfida.nomeSfidante.equals(sfidantePassato) && Sfida.codiceSfida == codiceSfidaPassato) {
			return true;
		}
		return false;
	}

	//Costruttore
	public SfidaRicevuta() {
		Sfida.nomeSfidante = "";
		Sfida.codiceSfida = -1;
	}

	//Questo metodo permette di settare i nuovi valori della richiesta
	public synchronized void setSfida(String sfidante) {
		Sfida.nomeSfidante = new String(sfidante); //Viene inizializzato il nome dello sfidante
		Sfida.codiceSfida++; //e incrementato il codice della sfida
		(new Thread(new ThreadEliminaSfida(Sfida.nomeSfidante, Sfida.codiceSfida))).start(); //e viene avviato il thread che la cancellerà dopo T1 millisecondi 
		//(se non ancora eliminata da un'altra chiamata a resetSfida())
	}

	//Questo metodo permette di resettare la richiesta di sfida
	public synchronized void resetSfida() {
		Sfida.nomeSfidante = "";
	}

	//Questo metodo permette di controllare se esiste una sfida
	public synchronized boolean esisteSfida() {
		if(Sfida.nomeSfidante.isEmpty()) {
			return false;
		}
		return true;
	}
}
