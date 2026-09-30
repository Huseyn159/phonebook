# Phonebook API

CRUD + Excel import/export + JWT auth + dinamik axtarish ucun API. Spring Boot, PostgreSQL, Liquibase, Apache POI, Spring Security.

## Ise salmaq

- Postgres-de bos `phonebook` bazasi yarat
- `application.properties`-de DB user/password oz muhitine uygunlasdir
- `JWT_SECRET` environment variable teyin et
- `./mvnw spring-boot:run`
- Swagger: http://localhost:8080/swagger-ui.html (Authorize ile Bearer token elave et)

## Endpoint-ler

| Method | Path | Ne edir |
|--------|------|---------|
| POST   | /api/v1/auth/register | Qeydiyyat |
| POST   | /api/v1/auth/login | Login, JWT token qaytarir |
| POST   | /api/v1/phonebook | Yeni yazi |
| GET    | /api/v1/phonebook/{id} | Bir yazini getir |
| PUT    | /api/v1/phonebook/{id} | Yazini deyis (stopDate qoymaq/silmek daxil) |
| DELETE | /api/v1/phonebook/{id} | Yazini sil |
| POST   | /api/v1/phonebook/import | Excel (.xlsx) ile toplu create/update |
| POST   | /api/v1/phonebook/search | Kriteriyali, sehifelenmis axtaris |
| POST   | /api/v1/phonebook/export | Kriteriyali axtarishin neticesini .xlsx kimi endir |
| POST   | /api/v1/phonebook/fields | Bir sutunun distinct deyerleri (filter UI ucun) |

## Esas qaydalar (Task 1)

- `active` saheni sistem ozu hesablayir: `stopDate` bosdursa aktiv, doludursa deyil
- Eyni nomreye eyni anda yalniz bir aktiv yazi ola biler, yoxsa 400
- Dayandirilmis nomrenin yeni yazisi, kohnenin bitme tarixinden evvel basalaya bilmez
- Uzunluq limiti asilsa -> 400, olmayan id -> 404

## Excel import (Task 2)

Endpoint: `POST /api/v1/phonebook/import` (multipart/form-data, sahe adi: `file`)

- Capitalize qaydasi: her sozun ilk herfi boyuk, qalani kicik (Title Case) - `name`, `surname`, `city`-e tetbiq olunur
- Create/Update ayrimi: `number` + `activatedDate` cutluyune gore DB-de axtarilir - tapilirsa update, tapilmirsa yeni yazi
- Her setir, Task 1-deki eyni qaydalarla yoxlanilir; bir setir ugursuz olsa, digerleri davam edir, xetalar `errors` massivinde
- Bos/oxuna bilmeyen fayl -> 400

## JWT Authentication

- Register: `username`+`password` (password: min 8 simvol, boyuk/kicik herf, reqem, xüsusi simvol)
- Login: dogru olarsa, JWT token qaytarir (24 saat etibarli)
- Qorunan endpoint-lere, `Authorization: Bearer <token>` basligi ile sorgu gonderilir
- `/api/v1/auth/**` ve Swagger, acigdir; qalan hersey, token teleb edir
- Sifreler, BCrypt ile hash olunur, hec vaxt aciq metnde saxlanmir

## Kriteriyali axtaris (Task 3)

Endpoint: `POST /api/v1/phonebook/search?page=0&size=20&sort=activatedDate,desc`

Body: `{ "criteria": [ { "key": "city", "value": "Baku", "operation": "EQUAL" } ] }`

- Bos `criteria` -> butun yazilar, sehifelenmis sekilde qaytarilir
- Butun kriteriyalar arasinda **AND** tetbiq olunur (OR desteklenmir)
- `value`, tek deyer (string/reqem) ve ya array ola biler (operation-a gore); `IN`/`NOT_IN`-de tek deyer, tek elementli array kimi qebul olunur
- Deyerlerin acig sahe siyahisi ile yoxlanilir, uygun olmayan `key` -> 400 (boyuk/kicik herfe hessas deyil)
- `IS_MEMBER`, `ANY_OF` enum-da var, amma phonebook-da kolleksiya tipli sahe olmadigi ucun istifade olunmur
- `LAST_HOUR`/`LAST_MINUTE`, yalniz `createdAt`/`updatedAt` (datetime) sahelerine tetbiq oluna biler; `LAST_DAY`/`LAST_WEEK`/`LAST_MONTH`, hem date, hem datetime sahelerinde islenir
- `BETWEEN`, tam olaraq 2 elementli array teleb edir, eksik/artiq olarsa -> 400

## Excel export (Task 4)

Endpoint: `POST /api/v1/phonebook/export` (Task 3-deki eyni `SearchRequest` body-si)

- Ayni `SpecificationBuilder`-den istifade olunur, axtaris ve export eyni netice setini qaytarir
- Sync export, cap ile mehdudlasdirilib (50 000 setir)
- Butun entity saheleri (id, active, createdAt, updatedAt daxil) sutun kimi yazilir; `null` deyerler bos xana kimi yazilir
- Tarix/vaxt saheleri, metn (String) formatinda yazilir, Excel-in oz tarix formatinda deyil

## Distinct field values (Task 5)

Endpoint: `POST /api/v1/phonebook/fields?field=city&searchValue=&page=0&size=20` (Task 3-deki eyni `SearchRequest` body-si, kohtenli)

- Whitelist: `name, surname, nationalId, fin, address, city, number, active`
- `searchValue`, "contains" axtarishi kimi ishleyir (`LIKE %value%`)
- `null` deyerler, netice siyahisindan cixarilir
- Repository/Specification `SELECT DISTINCT`-i desteklemedigi ucun, `EntityManager` (raw JPA Criteria API) ile qurulub; data ve cemi say ucun iki ayrica sorgu ishledilir

## Delete

Hard delete secildi - silinme isaresi ucun sutun yoxdur, tarixce onsuz da kohne (dayandirilmis) yazilarla qorunur.
