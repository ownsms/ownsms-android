# Rassilka, navigatsiya tuzatish va to'liq registratsiya

Sana: 2026-07-16 · Repo: `ownsms-android` · Backend: **o'zgarmaydi**

## Qamrov

Uch qism, bittasi ham backend'ga tegmaydi (`django-ownsms` v0.3.3 jonli, campaigns API allaqachon bor):

1. **Navigatsiya tuzatish** (bug, ildizdan) — drawer ekranlaridan uyga qaytib bo'lmaydi.
2. **Rassilka** — bulk SMS ekrani + tarix, mavjud `/api/v1/campaigns` ustida.
3. **Registratsiya + tezlik sozlamalari** — email, operator, SIM raqami; rate/pause override.

Tartib majburiy: (1) avval, chunki (2) yangi tab qo'shadi — buzuq navigatsiya ustiga qurmaslik kerak.

---

## 1. Navigatsiya tuzatish

### Ildiz

`ui/AppRoot.kt:102-105`:

```kotlin
fun openFromDrawer(route: String) {
    scope.launch { drawerState.close() }
    nav.navigate(route) { launchSingleTop = true }   // popUpTo YO'Q
}
```

`popUpTo` yo'q. `launchSingleTop` faqat ekran backstack'ning **eng ustida** bo'lsagina takrorlashni to'xtatadi.
Natija: About → Guide → About → Guide = `[home, about, guide, about, guide]` — backstack cheksiz o'sadi.
"Orqaga" bosilsa yana About, yana Guide... uyga yetib bo'lmaydi. Bu aynan xabar qilingan simptom.

Qo'shimcha kamchiliklar (simptomni kuchaytiradi, lekin sababi emas):

- `AppRoot.kt:128-132` — TopAppBar `navigationIcon` doim gamburger; About/Guide'да orqaga strelkasi yo'q.
- `AppRoot.kt:80` — `currentTab` drawer route'ida `null` → bottom-nav'da hech biri tanlanmagan, o'lik ko'rinadi.
- `components/AppDrawer.kt` — "Bosh sahifa" elementi yo'q.

### Fix

**`selectTab` va `openFromDrawer` bitta `go(route)` funksiyasiga birlashadi.** Ikkalasi ham bir xil
navigatsiya opsiyalarini ishlatadi, dublikat o'chadi:

```kotlin
fun go(route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

Drawer'dan chaqirilganda ustiga `scope.launch { drawerState.close() }`. Shundan keyin backstack hech qachon
`[home, X]`dan uzun bo'lmaydi; istalgan drawer ekranidan "Orqaga" → to'g'ri uyga.

**Orqaga strelkasi:** `currentTab == null` (ya'ni about/guide) bo'lganda `navigationIcon` gamburger o'rniga
`Icons.AutoMirrored.Filled.ArrowBack` → `nav.popBackStack()`. Tab ekranlarida gamburger qoladi.

Skipped: drawer'ga alohida "Bosh sahifa" elementi — orqaga strelkasi + bottom-nav qoplaydi.

### Tekshiruv

Sof mantiq yo'q (Compose navigatsiyasi) → unit test yozilmaydi. Telefonда qo'lda:
drawer → About → drawer → Guide → drawer → About → **Orqaga bir marta** → Home.

---

## 2. Rassilka (bulk SMS)

### Kirish nuqtasi

`Tab` enum'ga 5-element: `BULK("bulk", ...)` — Home / Faollik / **Rassilka** / SIM / Sozlamalar.
M3 bottom-nav 5 tagacha ruxsat beradi. Ikonka: `Icons.Filled.Campaign` (`material-icons-extended` allaqachon dep).

### Ekran tuzilishi (`ui/screens/BulkScreen.kt`)

**A. Yangi rassilka formasi**

- **Raqamlar** — ko'p qatorli maydon, har qator bitta raqam. Ostida jonli hisob: "12 ta raqam".
- **Matn** — ko'p qatorli maydon + belgi/segment hisoblagichi (`segments` mantiqi Home'dagi test-SMS bilan bir xil).
- **Sozlamalar** (yig'iladigan karta):
  - `queued` — switch, default **yoqilgan** (backend campaign default'i ham `true`).
  - `send_at` — native date+time picker, bo'sh = darhol.
  - `from` — SIM tanlash (dropdown), bo'sh = default SIM (`_resolve_sim` server tomonda shunday tushadi).
- **Yuborish** tugmasi.

**B. Progress karta** (faol rassilka bo'lsa)

`queued / sending / sent / failed` hisoblari + **Pauza / Davom / Bekor** tugmalari.
Poll: har 5s `GET /api/v1/campaigns/{id}`, faqat ekran ko'rinib turganda.

**C. Tarix**

Oxirgi rassilkalar ro'yxati (id, matn boshi, total, status). Bosilsa → progress/boshqaruv.

> **Backend cheklovi:** campaign ro'yxati endpoint'i **yo'q** (`urls.py`da faqat create/detail/messages/act).
> Tarix **lokal** saqlanadi: Room'ga `CampaignEntity(id, text, total, createdAt)` — yuborilganda yoziladi,
> ochilganda har biri uchun `GET /campaigns/{id}` bilan status yangilanadi. Faqat shu telefondan
> yuborilgan rassilkalar ko'rinadi — self-use uchun yetarli.
> Skipped: server tomonda `GET /api/v1/campaigns` ro'yxati — backend o'zgarishi + redeploy talab qiladi;
> kerak bo'lsa keyin.

### Parsing

```kotlin
fun parseRecipients(raw: String): List<String> =
    raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
```

Bo'sh qatorlar tashlanadi. Dublikat raqamlar **o'chirilmaydi** (foydalanuvchi ataylab qo'ygan bo'lishi mumkin).

### Xato ishlash — MAJBURIY

Backend **fail-fast atomic**: 100 raqamdan bittasi xato bo'lsa **hech biri yuborilmaydi**, 422 qaytadi:

```json
{"error": {"code": "validation_error", "message": "...",
           "bad_rows": [{"index": 0, "error": "invalid_phone", "to": "901"}]}}
```

`bad_rows[].error` ∈ `missing_to` | `invalid_phone` (+`to`) | `missing_vars` (+`missing`).

Ekran `index`ni **qator raqamiga** aylantirib ko'rsatadi: *"3-qator: raqam noto'g'ri (901)"*.
Bo'sh qatorlar tashlangani uchun `index` ≠ matndagi qator raqami — moslashtirish kerak:
`parseRecipients` raqam bilan birga original qator indeksini ham qaytaradi.

Bu chala qilinmaydi — aks holda foydalanuvchi nima uchun ishlamayotganini bilmaydi.

### API (`data/remote/DevApi.kt` — Bearer `api_key`)

```kotlin
@POST("api/v1/campaigns")
suspend fun createCampaign(@Body body: CreateCampaignReq): CampaignCreated

@GET("api/v1/campaigns/{id}")
suspend fun campaign(@Path("id") id: String): CampaignDetail

@POST("api/v1/campaigns/{id}/{action}")
suspend fun campaignAction(@Path("id") id: String, @Path("action") action: String): CampaignDetail
```

DTO (`ApiModels.kt`, snake_case — Moshi reflective, annotation yo'q):

```kotlin
data class Recipient(val to: String)
data class CreateCampaignReq(
    val text: String,
    val recipients: List<Recipient>,
    val from: String? = null,
    val send_at: String? = null,   // ISO-8601
    val queued: Boolean = true,
)
data class CampaignCreated(val id: String, val status: String, val total: Int)
data class CampaignProgress(
    val queued: Int = 0, val sending: Int = 0, val sent: Int = 0, val delivered: Int = 0,
    val failed: Int = 0, val expired: Int = 0, val canceled: Int = 0,
)
data class CampaignDetail(val id: String, val status: String, val total: Int, val progress: CampaignProgress)
data class BadRow(val index: Int, val error: String, val to: String? = null, val missing: List<String>? = null)
```

422 javobi Retrofit'да `HttpException` → `errorBody()` qo'lda Moshi bilan parse qilinadi.

**`vars` / `{placeholder}` qo'shilmaydi** — matn hammaga bir xil. Backend qo'llab-quvvatlaydi, lekin so'ralmagan.
Skipped: `rotate_sims`, `callback_url` — backend ularni qabul qiladi-yu, **hech qachon o'qimaydi** (o'lik maydonlar).

---

## 3. Registratsiya + tezlik sozlamalari

### 3a. Registratsiya to'ldiriladi

Hozirgi holat (`ui/screens/OnboardingScreen.kt`, `ui/MainViewModel.kt`):

| Kamchilik | Hozir | Bo'ladi |
|---|---|---|
| Email | `SignupRequest.email` DTO'да bor, **hech qachon to'ldirilmaydi** — doim `""` | Onboarding'ga maydon; `register(url, email)` |
| Operator | `simRegs()` doim `""` yuboradi | `SubscriptionManager.carrierName` → `SimRepository`da o'qiladi |
| SIM raqami | Operator bermasa bo'sh ketadi, **qo'lda kiritib bo'lmaydi** | SIM tasdiqlash qadami — raqam tahrirlanadi |

**SIM tasdiqlash qadami** — ruxsatlardan keyin, registratsiyadan oldin: topilgan SIM'lar ro'yxati,
har biri uchun tahrirlanadigan raqam maydoni + default tanlash. O'zbek operatorlari raqamni ko'pincha
bermaydi → hozir bo'sh ketadi → `from` (rassilkada ham) ishlamaydi. Bu eng muhim tuzatish.

Registratsiya ham, pairing ham shu qadamdan o'tadi (`simRegs()` ikkalasiga umumiy).

**Xabarlar `strings.xml`ga:** `MainViewModel`da qattiq yozilgan o'zbekcha satrlar
(`"Ro'yxatdan o'tdingiz — API KEY saqlandi."`, `"Server bilan bog'lanib bo'lmadi"`, `"Raqam kiriting"` va h.k.)
`values-ru`да ham o'zbekcha chiqadi. Resurslarga ko'chiriladi.

### 3b. Yuborish tezligi sozlamalari

Yangi karta — Sozlamalar → **"Yuborish tezligi"**:

| Maydon | Server default | Chegara |
|---|---|---|
| Tanaffus (soniya) | 2–5 (jitter) | **min 2s** |
| Daqiqada | 15 | — |
| Soatda | 200 | — |
| Kunda | 500 | — |
| Kunlik kvota | yo'q | bo'sh = cheksiz |

**Bo'sh maydon = serverdagi qiymat.** Har maydon hint'ida serverdan kelgan joriy qiymat ko'rinadi
(→ "hozir nima amal qilyapti" doim ma'lum). Faqat to'ldirilgan maydon override qiladi.

Serverdan yuqori qiymat kiritilsa ogohlantirish yozuvi (operator bloklashi mumkin) — lekin ruxsat beriladi:
bu foydalanuvchining o'z SIM'i. **Tanaffusdagi 2s floor qat'iy** — anti-spam himoyasi kesilmaydi.

**Kod:** `RateGate` va `SenderService` tegilmaydi. Bitta sof funksiya:

```kotlin
fun effectiveConfig(server: SimConfig, o: Overrides): SimConfig
```

`SenderService` config olgandan keyin shundan o'tkazadi (`SenderService.kt:110` atrofida).
`Settings.kt`ga 5 kalit qo'shiladi (hozir 5 ta bor).

Override **global** — barcha SIM'larga bitta to'plam, per-SIM emas (asosan bitta default SIM'dan yuboriladi).
Skipped: ish soatlari (`work_hours_start/end`) — so'ralmagan; per-SIM override — kerak bo'lsa keyin.

### 3c. Akkaunt boshqaruvi (api_key bilan yoziladigan hamma narsa)

`api_key` (Bearer `osk_...`) bilan **haqiqatan o'zgaradigan** yagona sozlamalar shular. Qolgani
(akkaunt nomi/email/status, per-SIM rate/kvota) **faqat Django admin** — ilovaga qo'yilmaydi
(ishlamaydigan tugma bo'lardi).

**API kalitlari** (`GET`/`POST /api/v1/keys`, `POST /api/v1/keys/{id}/revoke`) — yangi karta:

- Ro'yxat: har kalit — `prefix`, `name`, `scopes`, `ip_allowlist`, `revoked`, `created_at`.
  To'liq token **ko'rsatilmaydi** (server saqlamaydi, faqat `prefix`).
- Yangi kalit: `name`, `scopes` (send/read checkbox), **`ip_allowlist`** (vergul bilan IP/CIDR).
  Javob `201` — to'liq `osk_...` **bir marta** ko'rsatiladi → `CopyableField` (mavjud komponent).
- Bekor qilish: `revoke` tugmasi. Un-revoke yo'q (backend'da ham yo'q).

> **IP allowlist yangilanmaydi.** Faqat kalit yaratishда beriladi (`views/keys.py:47`), update endpoint yo'q.
> Ilova "o'zgartirish" o'rniga: yangi kalit (yangi allowlist bilan) yasab, eskisini bekor qilishni
> taklif qiladi. Buni UI matnida aniq yozish kerak, aks holda foydalanuvchi update kutadi.

**Webhook** (`GET`/`PUT /api/v1/webhook`) — yangi karta:

- `url` (maydon), `events` (checkbox: `message.sent`, `message.delivered`, `message.failed`),
  `enabled` (switch). `PUT` shu uchtasini qabul qiladi.
- `secret` — **faqat o'qish**, `CopyableField(masked=true)`. HMAC imzo kaliti, `PUT` o'zgartira olmaydi
  (server avto-generatsiya qiladi).

**Qurilmalar** (`GET /api/v1/devices`, `POST /api/v1/devices/{id}/{act}`) — yangi karta:

- Ro'yxat: `name`, `status`, `online`, `last_seen`, `app_version`.
- Har biri: **Faollashtirish / O'chirish** (`activate`/`deactivate`). Boshqa amal yo'q.

**Audit jurnali** (`GET /api/v1/audit`, `read` scope) — yangi karta, **faqat o'qish**:

- Oxirgi 100 yozuv: `actor`, `action`, `target`, `ip`, `at`. Filtr yo'q. "Kim, qachon, qayerdan" ko'rinadi.

API (`data/remote/DevApi.kt`):

```kotlin
@GET("api/v1/keys")               suspend fun keys(): KeysList
@POST("api/v1/keys")              suspend fun createKey(@Body b: CreateKeyReq): CreatedKey
@POST("api/v1/keys/{id}/revoke")  suspend fun revokeKey(@Path("id") id: String): ApiKeyInfo
@GET("api/v1/webhook")            suspend fun webhook(): WebhookConfig
@PUT("api/v1/webhook")            suspend fun putWebhook(@Body b: WebhookPut): WebhookConfig
@GET("api/v1/devices")            suspend fun devices(): DevicesList
@POST("api/v1/devices/{id}/{act}") suspend fun deviceAction(@Path("id") id: String, @Path("act") act: String): DeviceInfo
@GET("api/v1/audit")              suspend fun audit(): AuditList
```

Barcha DTO snake_case (Moshi reflective). `scopes`/`events`/`ip_allowlist` — `List<String>`.

> **Xavfsizlik eslatmasi (kodga emas, foydalanuvchiga):** webhook `secret` har qanday amaldagi
> kalitga ko'rinadi; `read`-kalit ham yangi `send`-kalit yasay oladi (backend scope-subset
> tekshirmaydi). Bu backend xatti-harakati — ilova o'zgartirmaydi, faqat mavjud endpointlarni ochadi.

### Sozlamalar ekrani yakuniy tartibi

Kartalar (hozirgi 4 + yangi 5): Tayyorlik · Server · Akkaunt (API kalit + pairing) · **Yuborish tezligi** ·
**API kalitlari** · **Webhook** · **Qurilmalar** · **Audit** · Test SMS. Uzun bo'lgani uchun
har biri yig'iladigan (`ExpandableCard`) — faqat sarlavhalar ko'rinadi, bosilganda ochiladi.

---

## Testlar

Sof funksiyalar, telefon kerak emas (`app/src/test`, hozir 3 test bor):

1. `parseRecipients` — bo'sh qatorlar, `\r\n`, atrofdagi bo'shliqlar, bo'sh kirish.
2. `bad_rows[].index` → matndagi qator raqami (bo'sh qatorlar tashlangani uchun siljish bor).
3. `effectiveConfig` — bo'sh override = server qiymati; to'ldirilgan = override; tanaffus 2s floor.

Qolgani (SMS, navigatsiya, campaign E2E) — telefonда qo'lda.

## Build

```
JAVA_HOME=D:\Projects\ownsms\.toolchain\jdk-17.0.19+10
D:\Projects\ownsms\.toolchain\gradle-8.7\bin\gradle.bat -p "D:\Projects\ownsms\ownsms-android" ^
  assembleDebug ktlintCheck testDebugUnitTest
```

Branch: `feat/bulk-nav-registration` (`main`dan, hozir v1.0.1 `109e72c`).
</content>
</invoke>
