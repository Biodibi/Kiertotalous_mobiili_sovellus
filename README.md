# HavikkiHaly -mobiilisovellus

HavikkiHaly -mobiilisovelluksen avulla kaupat tai muut toimijat, joiden toiminnassa hävikkiä syntyy, voivat ilmoittaa ruokahävikin hyödyntäjälle. Hyödyntäjä vastaanottaa viestit tällä samalla mobiilisovelluksella ja voi suunnitella hakureitin ilmoitusten perusteella. Sovelluksen tavoitteena on siis antaa reaaliajassa tietoa noudettavissa olevasta ruokahävikistä ja helpottaa ruokahävikin keräilyä ja mahdollistaa taloudellisen hakureitin suunnittelun ruokahävikille. Ilmoitukset tallentuvat järjestelmään ja niiden perusteella on mahdollista nähdä tietoa toimitetuista ruokahävikistä sekä niiden ilmastovaikutuksista HavikkiHaly -[web-sovelluksessa](https://github.com/Biodibi/kiertotalous_web).

## Sovelluksen toiminta

Sovellukseen kirjaudutaan käyttäjätunnuksilla. Käyttäjätunnuksen mukaan sovellus joko näyttää ruokahävikin ilmoitusnäkymän tai noutonäkymän. Käyttäjätunnukseen on siis sidottu rooli, edustaako käyttäjä ruokahävikin ilmoittajaa vai hyödyntäjää.

Ilmoitusnäkymässä kirjataan paino, joka luetaan automaattisesti vaa'an avulla, mikäli sovelluksen kanssa yhteensopiva vaaka on käytössä. Tämä demo käyttää [FLP-lattiavaakaa](https://www.teollisuusvaaka.fi/lattiavaaka/#FLP), jonka tuottaman painon sovellus lukee automaattisesti. Punnituksessa otetaan huomioon taaraus eli painosta vähennetään lavan tai rullakon paino automaattisesti. Tarvittaessa paino voidaan syöttää myös lomakkeelle käsin, mikäli vaaka ei ole käytössä. Painon lisäksi voidaan syöttää kuvaus sekä lisätä kuvia. Päiväys ja aika tallentuvat ilmoitukseen automaattisesti.

![Ilmoitusnäkymä](https://github.com/Biodibi/kiertotalous_app/blob/master/images/ilmoitus.png)

Kun ilmoitus tehdään, saapuu sovelluksen huomautus (notifikaatio) noudettavissa olevasta ruokahävikistä.

Noutonäkymässä nähdään ruokahävikistä tehdyt ilmoitukset listana. Listaa voidaan haluattaessa järjestellä ja lista osoittaa ruokahävikin noutojärjestyksen.

![Listanäkymä](https://github.com/Biodibi/kiertotalous_app/blob/master/images/lista.png)

Käyttäjä voi myös merkata ruokahävikin noudetuksi listalta laittamalla valintaruudun (checkbox) valituksi. Mikäli ilmoitettua ruokahävikkiä ei merkata noudetuksi, poistuu se listalta automaattisesti vuorokauden vaihtuessa. 
Ilmastovaikutusten laskentaa varten voidaan ilmoittaa noutoon/kuljetukseen liittyen matka sekä käytetty polttoaine.

Kompassi-ikonia painamalla näytetään kartta, joka piirtää reitin noutonäkymän listan mukaisessa järjestyksessä.

![Listanäkymä](https://github.com/Biodibi/kiertotalous_app/blob/master/images/kartta.png)

Sovelluksen asetuksissa voidaan asettaa käytettävä taara (punnituksessa vähennettävä lavan tai rullakon paino).

![Listanäkymä](https://github.com/Biodibi/kiertotalous_app/blob/master/images/asetukset.png)

## Sovelluksen toteutuksessa käytetyt tekniikat

Sovellus on toteutettu [Android-sovelluksena](https://developer.android.com). Tiedot tallentuvat [Firebase-tietokantaan](https://firebase.google.com).

## Lisenssi

Tämän projektin lähdekoodi on lisenssoitu MIT-lisenssillä. Katso lisenssin tiedot tarkemmin [LICENSE](https://github.com/Biodibi/kiertotalous_app/blob/master/LICENSE.md) tiedostosta. Mikäli sovelluksessa on käytetty kolmannen osapuolen työkaluja, komponentteja tai vastaavia, noudetaan niiden osalta ilmoitettuja lisenssiehtoja.

