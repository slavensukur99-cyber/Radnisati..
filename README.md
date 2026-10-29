# RadniSati — Android projekat

## Ručno postavljanje na GitHub
1. Raspakuj ovaj ZIP na telefonu ili računaru.
2. Na GitHubu napravi novi repozitorijum.
3. Postavi sadržaj ovog foldera u korijen repozitorijuma (uključujući `.github` folder).
4. Otvori karticu **Actions**.
5. Izaberi **Build RadniSati APK** i klikni **Run workflow**.
6. Kada se završi, otvori završeni workflow, nađi **Artifacts** i preuzmi `RadniSati-APK`.
7. Raspakuj preuzeti artifact i instaliraj `app-debug.apk` na Samsung Galaxy A56.

## Funkcije
- Tamni interfejs sa plavo-ljubičastim detaljima i jednostavnim animacijama.
- Mjesečni kalendar, izbor dana i navigacija između mjeseci.
- Unos početka/kraja smjene i napomene.
- Noćni rad 22:00–06:00; ostalo se računa kao dnevni rad.
- Lokalno čuvanje podataka preko Android SharedPreferences.

Napomena: ovo je početna verzija. Smjena kojoj je kraj isti kao početak tretira se kao smjena od 24 sata; unesi različita vremena za kraće smjene.
