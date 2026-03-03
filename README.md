# Relazione CROSS — Documentazione tecnica

## 1. Definizione delle scelte implementative

L’obiettivo del progetto è l’implementazione di un servizio di **order book** per un exchange di criptovalute, denominato **CROSS**, che gestisce ordini di acquisto e vendita per la coppia **BTC/USD**.

L’architettura è stata progettata per essere **robusta**, **scalabile** e **performante**, seguendo le specifiche fornite e adottando pattern architetturali moderni per la gestione della concorrenza e l’elaborazione dei dati.

### 1.1 Approccio progettuale e principi SOLID

L’implementazione del sistema è stata guidata dai principi dell’ingegneria del software per garantire un’architettura **manutenibile**, **estensibile** e **robusta**. Pur essendo un progetto didattico, si è cercato di aderire ai principi **SOLID**:

- **Single Responsibility Principle (SRP)**  
  Ogni classe ha una singola e ben definita responsabilità.  
  Esempi:
  - `Worker`: gestione comunicazione con un singolo client
  - `UserDB`: persistenza utenti
  - `OrderBook`: logica di matching
  - `RequestHandler`: gestione di una specifica operazione

- **Open/Closed Principle (OCP)**  
  Il sistema è aperto all’estensione ma chiuso alle modifiche.  
  Grazie ai pattern **Strategy** e **Factory** (`RequestHandlerFactory` e `RequestHandlerInterface`), è possibile aggiungere nuove funzionalità (es. nuovo comando o nuovo tipo di ordine) creando un nuovo `RequestHandler` senza modificare il `RequestDispatcher`.

- **Dependency Inversion Principle (DIP)**  
  I moduli di alto livello dipendono da astrazioni, non da implementazioni concrete.  
  Esempio: `RequestDispatcher` non conosce le classi specifiche (login, inserimento ordini, ecc.), ma interagisce solo tramite `RequestHandlerInterface`.

Questa suddivisione delle responsabilità, insieme all’uso di pattern consolidati, permette una base di codice **modulare**, in cui i componenti possono essere compresi e modificati in modo isolato.

### 1.2 Architettura dettagliata del server

- **Gestione delle connessioni**  
  Per gestire un elevato numero di client concorrenti, il server utilizza un `ThreadPoolExecutor` di dimensione fissata. Ogni connessione entrante viene assegnata a un thread lavoratore (`Worker`) che gestisce l’intero ciclo di vita della sessione.

- **Pattern dispatcher + strategy**  
  Le richieste JSON sono gestite dal `RequestDispatcher` come entry point centrale. Tramite il pattern **Factory** (`RequestHandlerFactory`) viene creata e selezionata la strategia appropriata (`RequestHandlerInterface`) in base all’operazione richiesta.  
  Questo disaccoppia il server dalla logica specifica delle operazioni e rende il sistema facilmente estensibile.

- **Motore di matching e pattern Disruptor (LMAX)**  
  Per il cuore del sistema (order book) è stato scelto il pattern **LMAX Disruptor**, puntando a **bassa latenza** e **alto throughput**.  
  `OrderBookDisruptor` disaccoppia i thread `Worker` dal motore di matching, che è **single-threaded**.

- **Persistenza dei dati**  
  `UserDB` e `TradeDB` utilizzano un `ScheduledExecutorService` per il salvataggio periodico su disco.  
  Per evitare corruzione dei file, viene usata una scrittura **atomica**:
  1. scrittura su file temporaneo
  2. sostituzione del file originale solo a scrittura completata

### 1.3 Architettura dettagliata del client

- **Pattern State**  
  L’interfaccia utente è gestita con il pattern **State**.  
  `ClientController` mantiene uno stato corrente (`AuthenticatedState` o `NotAuthenticatedState`) che definisce le operazioni disponibili.

- **Separazione logica–vista (ispirazione MVC)**  
  Architettura ispirata a **Model-View-Controller** (`ClientView` e `ClientController`).  
  Anche senza un *Model* esplicito lato client (stato mantenuto sul server), la separazione migliora manutenibilità e testabilità.

## 2. Schema generale dei thread e strutture dati

### 2.1 Schema dei thread — lato server

- **Main thread**
  - inizializza i servizi
  - attende nuove connessioni client

- **ThreadPool per i client (`tcpRequestPool`)**
  - gestisce le sessioni dei client
  - assegna un `Worker` per connessione

- **Disruptor single-consumer thread**
  - unico thread con accesso in scrittura all’`OrderBook`
  - processa gli eventi sequenzialmente

- **Scheduler per la persistenza**
  - due thread schedulati salvano in background utenti e trade

- **Post-matching executor thread**
  - `ExecutorService` single-threaded per I/O asincrono post-matching (persistenza e notifiche)

- **JVM shutdown hook thread**
  - `TerminationHandler` per chiusura ordinata del server

### 2.2 Schema dei thread — lato client

- **Main thread**
  - gestisce la CLI
  - richieste sincrone al server

- **Notification listener thread**
  - thread demone dedicato all’ascolto notifiche UDP asincrone

### 2.3 Strutture dati principali

- **Order book (`TreeMap`)**
  - due `TreeMap`: una per *ask* e una per *bid*
  - essendo `SortedMap`, mantiene i prezzi ordinati → miglior prezzo in `O(log n)`
    - *ask*: miglior prezzo = più basso (ordine naturale)
    - *bid*: miglior prezzo = più alto (ordine inverso)

- **Price level (`ArrayDeque`)**
  - per ogni livello di prezzo della `TreeMap`, gli ordini sono in una `ArrayDeque`
  - implementazione su array → migliore località di cache rispetto a `LinkedList` → meno cache miss CPU

- **Gestione utenti e sessioni (`ConcurrentHashMap`)**
  - accesso thread-safe e performante senza lock esplicite

- **Gestione rollback (`Stack` + `HashSet`)**
  - `TransactionState` usa:
    - `Stack` per salvare lo stato originale degli ordini modificati
    - `HashSet` per evitare salvataggi duplicati
  - in caso di errore, rollback ripristina lo stato precedente garantendo consistenza

## 3. Descrizione delle primitive di sincronizzazione

### Pattern Disruptor: cuore del matching engine

La scelta architetturale più significativa per performance elevate è l’adozione del **Disruptor** per la gestione del matching degli ordini.  
Rispetto alle code tradizionali, riduce contesa e lock, puntando a efficienza a livello hardware.

- **Ring Buffer ed eventi pre-allocati**
  - struttura centrale: `RingBuffer` circolare pre-allocato all’avvio (dimensione configurata in `server.properties`)
  - contiene oggetti `OrderEvent` riciclati
  - la pre-allocazione riduce la pressione sul Garbage Collector → utile per bassa latenza

- **Produttori e Sequencer**
  - ogni `Worker` è un produttore
  - per pubblicare un evento:
    1. reclama uno slot sul `RingBuffer` tramite il `Sequencer` (lock-free)
    2. popola l’`OrderEvent`
    3. pubblica la sequenza rendendola visibile al consumatore
  - `OrderEventProducer` astrae questa logica

### Strutture dati concorrenti (`ConcurrentHashMap`)

Usata in `UserDB` e `SessionManager` per consentire letture/scritture thread-safe senza sincronizzazioni esterne.

### Variabili atomiche

- `AtomicBoolean` (`isDirty` in `UserDB`): traccia modifiche non salvate in modo thread-safe
- `AtomicInteger` (`IDLoader`): genera ID univoci per gli ordini in modo atomico, evitando conflitti senza lock

### Uso mirato di `synchronized`

- lato server: limitato a brevi sezioni critiche per massimizzare concorrenza  
  Esempio: in `TradeDB` protegge solo la copia della lista dei trade, lasciando fuori le operazioni di I/O più lente.
- lato client: uso più frequente (es. `NotificationCenter`, accesso alla vista)

## 4. Istruzioni per la compilazione ed esecuzione

### 4.1 Prerequisiti

- **Java Development Kit (JDK)** versione **8 o superiore**
- Variabili d’ambiente `JAVA_HOME` e `PATH` impostate correttamente

### 4.2 Librerie esterne

Il progetto utilizza le librerie nella directory `lib/`: **gson**, **disruptor**, **logback**, **slf4j**.

### 4.3 Compilazione

Aprire un terminale nella directory del progetto (contenente `src/` e `lib/`) ed eseguire il comando appropriato, includendo le librerie nel classpath.

#### Linux/macOS

```bash
mkdir -p bin
javac -d bin -cp ".:lib/*" $(find . -name "*.java")
cp src/it/cross/server/server.properties bin/it/cross/server/
cp src/it/cross/client/client.properties bin/it/cross/client/
cp src/logback.xml bin/
````

#### Windows (PowerShell)

```powershell
mkdir bin
javac -d bin -cp ".;lib/*" (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
Copy-Item -Path "src/it/cross/server/server.properties" -Destination "bin/it/cross/server/server.properties"
Copy-Item -Path "src/it/cross/client/client.properties" -Destination "bin/it/cross/client/client.properties"
Copy-Item -Path "src/logback.xml" -Destination "bin/logback.xml"
```

### 4.4 Esecuzione

L’esecuzione richiede **due terminali separati**. I parametri sono letti dai file `server.properties` e `client.properties`.

1. **Avviare il server**

   * posizionarsi nella directory del progetto
   * eseguire:

     * Linux/macOS:

       ```bash
       java -cp "bin:lib/*" it.cross.server.ServerMain
       ```
     * Windows (usa `;` come separatore):

       ```powershell
       java -cp "bin;lib/*" it.cross.server.ServerMain
       ```
   * il server si avvierà e rimarrà in ascolto

2. **Avviare il client**

   * posizionarsi nella directory del progetto
   * eseguire:

     * Linux/macOS:

       ```bash
       java -cp "bin:lib/*" it.cross.client.ClientMain
       ```
     * Windows (usa `;` come separatore):

       ```powershell
       java -cp "bin;lib/*" it.cross.client.ClientMain
       ```
   * il client si avvierà e mostrerà il menu iniziale
