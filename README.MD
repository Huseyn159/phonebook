Phonebook API

CRUD + Excel import + JWT auth + dinamik axtarish ucun API. Spring Boot, PostgreSQL, Liquibase, Apache POI, Spring Security.

Ise salmaq
Postgres-de bos phonebook bazasi yarat
application.properties-de DB user/password oz muhitine uygunlasdir
JWT_SECRET environment variable teyin et
./mvnw spring-boot:run
Swagger: http://localhost:8080/swagger-ui.html (Authorize ile Bearer token elave et)
Endpoint-ler
Method	Path	Ne edir
POST	/api/v1/auth/register	Qeydiyyat
POST	/api/v1/auth/login	Login, JWT token qaytarir
POST	/api/v1/phonebook	Yeni yazi
GET	/api/v1/phonebook/{id}	Bir yazini getir
PUT	/api/v1/phonebook/{id}	Yazini deyis (stopDate qoymaq/silmek daxil)
DELETE	/api/v1/phonebook/{id}	Yazini sil
POST	/api/v1/phonebook/import	Excel (.xlsx) ile toplu create/update
POST	/api/v1/phonebook/search	Kriteriyali, sehifelenmis axtaris
Esas qaydalar (Task 1)
active saheni sistem ozu hesablayir: stopDate bosdursa aktiv, doludursa deyil
Eyni nomreye eyni anda yalniz bir aktiv yazi ola biler, yoxsa 400
Dayandirilmis nomrenin yeni yazisi, kohnenin bitme tarixinden evvel basalaya bilmez
Uzunluq limiti asilsa -> 400, olmayan id -> 404
Excel import (Task 2)

Endpoint: POST /api/v1/phonebook/import (multipart/form-data, sahe adi: file)

Capitalize qaydasi: her sozun ilk herfi boyuk, qalani kicik (Title Case) - name, surname, city-e tetbiq olunur
Create/Update ayrimi: number + activatedDate cutluyune gore DB-de axtarilir - tapilirsa update, tapilmirsa yeni yazi
Her setir, Task 1-deki eyni qaydalarla yoxlanilir; bir setir ugursuz olsa, digerleri davam edir, xetalar errors massivinde
Bos/oxuna bilmeyen fayl -> 400
JWT Authentication
Register: username+password (password: min 8 simvol, boyuk/kicik herf, reqem, xüsusi simvol)
Login: dogru olarsa, JWT token qaytarir (24 saat etibarli)
Qorunan endpoint-lere, Authorization: Bearer <token> basligi ile sorgu gonderilir
/api/v1/auth/** ve Swagger, acigdir; qalan hersey, token teleb edir
Sifreler, BCrypt ile hash olunur, hec vaxt aciq metnde saxlanmir
Kriteriyali axtaris (Task 3)

Endpoint: POST /api/v1/phonebook/search?page=0&size=20&sort=activatedDate,desc

Body: { "criteria": [ { "key": "city", "value": "Baku", "operation": "EQUAL" } ] }

Bos criteria -> butun yazilar, sehifelenmis sekilde qaytarilir
Butun kriteriyalar arasinda AND tetbiq olunur (OR desteklenmir)
value, tek deyer (string/reqem) ve ya array ola biler (operation-a gore)
Deyerlerin acig sahe siyahisi (name, surname, city, number, activatedDate ve s.) ile yoxlanilir, uygun olmayan key -> 400 (boyuk/kicik herfe hessas deyil)
IS_MEMBER, ANY_OF enum-da var, amma phonebook-da kolleksiya tipli sahe olmadigi ucun istifade olunmur
LAST_HOUR/LAST_MINUTE, yalniz createdAt/updatedAt (datetime) sahelerine tetbiq oluna biler; LAST_DAY/LAST_WEEK/LAST_MONTH, hem date, hem datetime sahelerinde islenir
BETWEEN, tam olaraq 2 elementli array teleb edir, eksik/artiq olarsa -> 400
Delete

Hard delete secildi - silinme isaresi ucun sutun yoxdur, tarixce onsuz da kohne (dayandirilmis) yazilarla qorunur.
