Plaintext
================================================================================
                        PROJEKT: GRA POKER (5-CARD DRAW)
                        ZALICZENIE SEMESTRALNE - PZ1
================================================================================

A. OMÓWIENIE ZASAD GRY
================================================================================
Projekt implementuje grę w pokera w wariancie pięciokartowym dobieranym (5-Card Draw).
Gra przeznaczona jest dla 2 do 4 graczy.

Przebieg rozgrywki:
1. Wniesienie stawki wejściowej (Ante).
2. Rozdanie każdemu graczowi 5 kart prywatnych.
3. Pierwsza runda licytacji (Betting Phase 1).
4. Faza wymiany kart (Drawing Phase) - gracze mogą wymienić od 0 do 5 kart.
5. Druga runda licytacji (Betting Phase 2).
6. Wyłożenie kart (Showdown) i wyłonienie zwycięzcy na podstawie starszeństwa układów.

Zaimplementowane układy kart (od najmocniejszego):
Poker Królewski, Poker, Kareta, Ful, Kolor, Strit, Trójka, Dwie Pary, Para, Wysoka Karta.

Źródła zasad gry:
- https://pl.wikipedia.org/wiki/Poker_pi%C4%99ciokartowy_dobierany
- https://terazpoker.pl/poker-pieciokartowy-dobierany-5-card-draw/

B. SPOSÓB URUCHOMIENIA PROGRAMU
================================================================================
Wymagania: Java 17+, Maven.

1. Uruchomienie testów:
   Należy wykonać polecenie w głównym katalogu projektu:
   $ mvn test

1. Kompilacja projektu:
   Należy wykonać polecenie w głównym katalogu projektu:
   $ mvn clean install

2. Uruchomienie Serwera:
   Należy przejść do katalogu modułu serwera i uruchomić klasę główną:
   (Serwer domyślnie nasłuchuje na porcie 7777)

3. Uruchomienie Klienta:
   Należy otworzyć nowe okno terminala dla każdego gracza, przejść do modułu klienta i uruchomić:

C. OMÓWIENIE PROTOKOŁU KOMUNIKACYJNEGO
================================================================================
Komunikacja odbywa się tekstowo. Każda komenda kończy się znakiem nowej linii.
Format ogólny: KOMENDA [PARAMETR1] [PARAMETR2] '\n'

--------------------------------------------------------------------------------
1. KOMUNIKATY KLIENTA (Wysyłane do Serwera)
--------------------------------------------------------------------------------

HELLO <version>
- Opis: Nawiązanie połączenia.
- Parametry (opcjonalnie): Wersja klienta (np. 1.0).
- Odpowiedź serwera: OK Welcome

CREATE <ante> <minBet>
- Opis: Tworzy nowy stół do gry. Nadawca zostaje Hostem.
- Parametry:
    - ante: stawka wejściowa.
    - minBet: minimalne przebicie (opcjonalne, domyślnie równe ante).
- Odpowiedź serwera: WELCOME <gameId> (dla Hosta)

JOIN <gameId> <playerName>
- Opis: Dołączenie do istniejącej gry.
- Parametry:
    - gameId: unikalny identyfikator gry (otrzymany przez Hosta).
    - playerName: nazwa gracza.
- Odpowiedź serwera: LOBBY <lista_graczy> (Broadcast do wszystkich w grze)

START
- Opis: Rozpoczyna rozgrywkę (rozdanie kart). Dostępne tylko dla Hosta, wymaga min. 2 graczy.
- Parametry: Brak.
- Odpowiedź serwera: STARTED <ante>, następnie DEAL (prywatne) i TURN (Broadcast).

STATUS
- Opis: Żądanie wyświetlenia aktualnego stanu gracza (faza, żetony, pula, czyja kolej).
- Parametry: Brak.
- Odpowiedź serwera: STATUS <tekst_informacyjny> (Prywatna wiadomość)

SHOW
- Opis: Pokazuje aktualne karty w ręce gracza.
- Parametry: Brak.
- Odpowiedź serwera: SHOW <lista_kart> (Prywatna wiadomość)

BET <amount>
- Opis: Podbicie stawki lub postawienie zakładu.
- Parametry: amount (liczba żetonów, musi być > 0).
- Odpowiedź serwera: ACTION <gracz> BET <kwota> (Broadcast)

CALL
- Opis: Sprawdzenie (wyrównanie do aktualnej najwyższej stawki).
- Parametry: Brak.
- Odpowiedź serwera: ACTION <gracz> CALL (Broadcast)

CHECK
- Opis: Czekanie (jeśli nikt nie podbił stawki w danej rundzie).
- Parametry: Brak.
- Odpowiedź serwera: ACTION <gracz> CHECK (Broadcast)

FOLD
- Opis: Pas (rezygnacja z dalszej gry w tej rundzie).
- Parametry: Brak.
- Odpowiedź serwera: ACTION <gracz> FOLD (Broadcast) lub END (jeśli został jeden gracz).

DRAW <idx1> <idx2> ...
- Opis: Wymiana kart.
- Parametry: Indeksy kart do wymiany (0-4). Np. "DRAW 0 1" wymienia pierwszą i drugą kartę.
- Odpowiedź serwera: DRAWOK <gracz> <ilość> (Broadcast)

QUIT
- Opis: Opuszczenie gry.
- Parametry: Brak.
- Odpowiedź serwera: OK (dla wychodzącego), LOBBY (dla pozostałych).

HELP
- Opis: Wyświetla listę dostępnych komend.
- Parametry: Brak.
- Odpowiedź serwera: MSG <treść_pomocy>

EXIT
- Opis: Rozłączenie klienta z serweremn
- Parametry: Brak.
- Odpowiedź serwera: QUIT Gracz opuścił serwer

--------------------------------------------------------------------------------
2. KOMUNIKATY SERWERA (Wysyłane do Klienta)
--------------------------------------------------------------------------------

OK <treść>
- Opis: Potwierdzenie wykonania operacji (np. po HELLO).

ERR <kod> <opis>
- Opis: Błąd (np. zły ruch, zła faza, brak gry).
- Przykłady: ERR INVALID_ARG Game is full, ERR SERVER_ERROR.

WELCOME <gameId>
- Opis: Potwierdzenie utworzenia gry. Przekazuje ID gry niezbędne dla innych graczy.

LOBBY <lista_imion>
- Opis: Informacja o zmianie listy graczy (ktoś dołączył lub wyszedł).

STARTED <ante>
- Opis: Informacja o rozpoczęciu gry i pobraniu wpisowego (ante).

DEAL <imie> <karty>
- Opis: Prywatna wiadomość z rozdaniem kart na początku gry.
- Przykład: DEAL Player1 [Ace of Spades, 10 of Hearts, ...]

TURN <imie> <faza> <aktualny_bet>
- Opis: Informacja o tym, czyja jest teraz kolej ruchu.

ACTION <imie> <ruch> [kwota]
- Opis: Informacja o ruchu wykonanym przez innego gracza (np. Player2 BET 100).

DRAWOK <imie> <ilość>
- Opis: Potwierdzenie, że dany gracz wymienił określoną liczbę kart.

WINNER <imie>
- Opis: Ogłoszenie zwycięzcy rundy.

END <powód>
- Opis: Koniec rundy (następuje po WINNER).

MSG <treść>
- Opis: Ogólna wiadomość tekstowa (np. instrukcje fazy gry, pomoc).

D. INNE ISTOTNE INFORMACJE
================================================================================
Technologia:
- Język: Java 17
- System budowania: Maven
- Logowanie: SLF4J + Logback (zastąpiono standardowe System.out/err dla czystości raportu SonarQube).
