Phonebook API

CRUD + Excel import ucun API. Spring Boot, PostgreSQL, Liquibase, Apache POI.

Ise salmaq
Postgres-de bos phonebook bazasi yarat
application.properties-de DB user/password oz muhitine uygunlasdir
./gradlew bootRun
Swagger: http://localhost:8080/swagger-ui.html
Endpoint-ler
Method	Path	Ne edir
POST	/api/v1/phonebook	Yeni yazi
GET	/api/v1/phonebook/{id}	Bir yazini getir
PUT	/api/v1/phonebook/{id}	Yazini deyis (stopDate qoymaq/silmek daxil)
DELETE	/api/v1/phonebook/{id}	Yazini sil
POST	/api/v1/phonebook/import	Excel (.xlsx) ile toplu create/update
Esas qaydalar (Task 1)
active saheni sistem ozu hesablayir: stopDate bosdursa aktiv, doludursa deyil
Eyni nomreye eyni anda yalniz bir aktiv yazi ola biler, yoxsa 400
Dayandirilmis nomrenin yeni yazisi, kohnenin bitme tarixinden evvel basalaya bilmez
Uzunluq limiti asilsa -> 400, olmayan id -> 404
Excel import (Task 2)

Endpoint: POST /api/v1/phonebook/import (multipart/form-data, sahe adi: file)

Capitalize qaydasi: her sozun ilk herfi boyuk, qalani kicik (Title Case) - name, surname, city-e tetbiq olunur. Meselen "vusal" -> "Vusal"
Create/Update ayrimi: number + activatedDate cutluyune gore DB-de axtarilir - tapilirsa update, tapilmirsa yeni yazi
Her setir, Task 1-deki eyni qaydalarla (aktiv nomre, tarix ardicilligi, uzunluq) yoxlanilir
Fayl daxilinde bir setir ugursuz olsa, diger setirler emal olunmaga davam edir; xetalar errors massivinde, Excel-de gorunen setir nomresi ile qaytarilir
Bos/oxuna bilmeyen fayl -> 400
Delete

Hard delete secildi - silinme isaresi ucun sutun yoxdur, tarixce onsuz da kohne (dayandirilmis) yazilarla qorunur.