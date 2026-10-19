# Phonebook API

CRUD + Excel import/export + JWT auth + dinamik axtarish + arxa plan import + real-vaxt bildirisler + Elasticsearch snapshot axtarisi ucun API. Spring Boot, PostgreSQL, Liquibase, Apache POI, Spring Security, WebSocket, SMTP, Elasticsearch.

## Ise salmaq

- Postgres-de bos `phonebook` bazasi yarat
- Elasticsearch 8.x ishe sal (lokal, Docker ile), meselen:
  `docker run -d -p 9200:9200 -e "discovery.type=single-node" -e "xpack.security.enabled=false" docker.elastic.co/elasticsearch/elasticsearch:8.15.0`
- `application.properties`-de DB user/password oz muhitine uygunlasdir
- Environment variable-lar teyin et: `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD` (Gmail App Password)
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
| POST   | /api/v1/phonebook/import | Excel (.xlsx) ile toplu create/update (sync) |
| POST   | /api/v1/phonebook/import/async | Eyni, amma arxa planda (boyuk fayllar ucun) |
| GET    | /api/v1/result-set/{id} | Arxa plan isinin veziyyeti (polling) |
| POST   | /api/v1/phonebook/search | Kriteriyali, sehifelenmis axtaris (PostgreSQL) |
| POST   | /api/v1/phonebook/export | Kriteriyali axtarishin neticesini .xlsx kimi endir |
| POST   | /api/v1/phonebook/fields | Bir sutunun distinct deyerleri (filter UI ucun) |
| POST   | /api/v1/phonebook/es/search | Elasticsearch snapshot axtarisi (yeni snapshot yaradir ve ya movcud olani daraldir) |

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

- Register: `username`+`password`+`email` (password: min 8 simvol, boyuk/kicik herf, reqem, xususi simvol)
- Login: dogru olarsa, JWT token qaytarir (24 saat etibarli)
- Qorunan endpoint-lere, `Authorization: Bearer <token>` basligi ile sorgu gonderilir
- `/api/v1/auth/**`, `/ws/**` ve Swagger, acigdir; qalan hersey, token teleb edir
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
- Icazeli sahelerin siyahisi `PhonebookFields`-dedir; Elasticsearch axtarisi (Task 7) da eyni siyahidan istifade edir

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

## Boyuk Excel import, arxa planda (Task 6)

Endpoint: `POST /api/v1/phonebook/import/async` (eyni form-data format, sync import ile)

- Sorgu, fayli diske saxlayir, `phonebook_result_set` setri (`IN_PROGRESS`) yaradir, `resultSetId`-ni derhal qaytarir (~1 san.)
- Faktiki import, arxa planda (`@Async`) gedir; 20 000 setir ucun ~20-25 saniye
- Bitende, `phonebook_result_set` setri `COMPLETED`/`FAILED` olur, sayilar (`totalRows`, `createdCount`, `updatedCount`, `failedCount`) yazilir
- Ireliye getme usullari:
  - **Polling**: `GET /api/v1/result-set/{resultSetId}`
  - **WebSocket**: STOMP, `/ws` endpoint, `/topic/result-set/{resultSetId}` kanalina abune olunur, proses bitende tam `PhonebookResultSet` push olunur
  - **Email**: istifadecinin `email`-i varsa (register-de qeyd olunubsa), bitende netice email-le gonderilir; email yoxdursa, bu addim sessizce atlanir
- `phonebook_result_set`, generic cedveldir (`type` sutunu ile): `IMPORT` (Task 6) ve `SEARCH` (Task 7) eyni cedveli istifade edir

## Elasticsearch snapshot axtarisi (Task 7)

Endpoint: `POST /api/v1/phonebook/es/search?page=0&size=20`

Body: `{ "resultSetId": null, "criteria": [ { "key": "city", "value": "Baku", "operation": "EQUAL" } ] }`

### Niye Elasticsearch

- PostgreSQL esas melumat menbeyi olaraq qalir (create/update/import orada gedir); Elasticsearch yalniz muveqqeti, suretli axtaris gorunusudur
- Agir filtri her sehife klikinde tekrar isletmek evezine, onu bir defe PostgreSQL-de isledirik ve neticeni Elasticsearch-e kocururuk ("snapshot"). Sonraki sehifeleme ve daraltma yalniz snapshot-dan oxuyur

### Nece isleyir

**`resultSetId` yoxdursa (yeni axtaris):**
1. `phonebook_result_set` setri yaradilir (`type=SEARCH`, `IN_PROGRESS`)
2. `criteria` PostgreSQL-de tetbiq olunur (Task 3-deki eyni mexanizm, en cox `max-snapshot-rows` setir)
3. Yeni indeks yaradilir (`phonebook-search-<resultSetId>`), neticeler bulk ile yazilir
4. Setir `COMPLETED` olur (`index_name`, `total_rows`, `expiration_time`)
5. `resultSetId` ve ilk sehife qaytarilir

**`resultSetId` varsa:** PostgreSQL-e muraciet olunmur, yalniz hemin snapshot-in indeksinde axtarilir. `criteria` snapshot daxilinde elave daraltmadir (bos olarsa hamisi). Yeni indeks yaranmir, `resultSetId` eyni qalir.

Netice `createdAt` azalan, beraber olanlar `id` artan sirada qaytarilir:

```json
{ "resultSetId": "...", "content": [ ... ], "page": 0, "size": 20,
  "totalElements": 2568, "totalPages": 514, "truncated": false }
```

### Xeta halari

| Hal | Status |
|-----|--------|
| `resultSetId` `IMPORT` tipli setrdir | 400 |
| `page * size + size` 10 000-i kecir (ES `max_result_window`) | 400 |
| Desteklenmeyen operator, yanlis sahe, yanlis tarix formati, bos `value` | 400 |
| Olmayan `resultSetId`, muddeti bitmis snapshot ve ya silinmis indeks | 404 |
| Snapshot hazir deyil (`IN_PROGRESS` ve ya `FAILED`) | 409 |

### Desteklenen operatorlar

| Operator | Saheler | Elasticsearch sorgusu |
|----------|---------|-----------------------|
| `EQUAL`, `IN` | butun saheler (`name`, `surname`, `address` ucun tam uygunluq, `.raw` keyword) | `term` / `terms` |
| `MATCH` | yalniz `name`, `surname`, `address` (soz esasli: `"ali"` "Ali Veli"-ni tapir, `"li"` tapmir) | `match` |
| `GREATER_THAN`, `GREATER_THAN_EQUAL`, `LESS_THAN`, `LESS_THAN_EQUAL`, `BETWEEN` | yalniz tarix saheleri (ISO date ve ya date-time) | `range` |

Diger operatorlar (`NOT_*`, `LAST_*` ve s.) snapshot daxilinde desteklenmir -> 400. `BETWEEN` sinirlari daxil sayilir, ters verilerse netice bosdur.

### Konfiqurasiya

| Xasse | Qiymet | Menasi |
|-------|--------|--------|
| `spring.elasticsearch.uris` | `http://localhost:9200` | Elasticsearch unvani (timeout-lar da `spring.elasticsearch.*` ile) |
| `phonebook.es.max-snapshot-rows` | `10000` | Snapshot-a kocurulen maksimum setir sayi |
| `phonebook.result-set.retention-days` | `30` | Snapshot-in omru (gun) |
| `phonebook.result-set.cleanup-interval-ms` | `3600000` (defolt, 1 saat) | Temizleme isinin araligi |

### 30 gunluk temizleme

- Her `SEARCH` setrinde `expiration_time = yaranma vaxti + retention-days`
- Scheduler muddeti bitmis `SEARCH` setirlerini tapir: **evvel Elasticsearch indeksini, sonra DB setrini** silir
- Indeks onsuz da yoxdursa, bu ugur sayilir ve setir silinir; indeks silinmesi alinmazsa setir qalir ve novbeti isledilmede yeniden cehd olunur
- Indeksin adi `resultSetId`-den qurulur, ona gore `FAILED` setirlerin (`index_name` bos) bos indeksleri de temizlenir

### Indeks mapping-i

`name`, `surname`, `address`: `text` + `raw` (keyword) alt sahesi; `id`, `nationalId`, `fin`, `city`, `number`: `keyword`; tarix saheleri: `date`; `active`: `boolean`.
Melum mehdudiyyetler
Her resultSetId-siz sorgu yeni snapshot ve yeni indeks yaradir (eyni filtr ucun movcud olan yeniden istifade olunmur). Bu bilerekdir: tapshiriq bunu teleb edir, snapshot "o anin sekli"dir ve yeni axtaris cari melumati gostermelidir; kohne snapshot-i paylasmaq kohne neticeni sessizce qaytara biler, resultSetId-nin ise sahibi yoxdur. Gelecekde optimallasdirma lazim olsa: criteriaHash + createdBy saxlayib, yalniz eyni istifadecinin, eyni filtrli ve yeni (meselen 5-10 deq.-den az) snapshot-ini yeniden istifade etmek olar
Snapshot en cox max-snapshot-rows setirdir; filtr bundan cox netice tapsa, hamisi snapshot-a dusmur. Cavabdaki truncated sahesi hele doldurulmur (hemise false)
from + size 10 000-den cox ola bilmez (search_after yoxdur)
MATCH soz esaslidir, soz daxilinde hisse axtarisi (LIKE '%li%' kimi) etmir; bunun ucun PostgreSQL axtarisi (/search) istifade olunur
resultSetId sahibsizdir: sisteme daxil olmus istifadeci, bildiyi istenilen resultSetId ile snapshot-i oxuya biler
Yanlis criteria ile yeni axtarishda yaradilmis result_set setri FAILED qalir (error_summary ile) ve muddeti bitende temizlenir
Tutulmayan xetalar (meselen Elasticsearch-in icra ede bilmediyi sorgu) 500 evezine 403 kimi gorsene biler; sebebi, ehtimal ki, Spring-in daxili /error yonlendirmesidir ve hele hell edilmeyib
## Delete

Hard delete secildi - silinme isaresi ucun sutun yoxdur, tarixce onsuz da kohne (dayandirilmis) yazilarla qorunur.