# Dizayn — Status bug fix + Android UX (2026-07-06)

Workstream 1/3. Backend jonli: `https://sms.omadli.uz`. Bu ish faqat `ownsms-android`ni
o'zgartiradi (server o'zgarmaydi).

## A. Status bug fix (ildiz)

**Simptom:** SMS qabul qiluvchiga boradi, lekin "Bu qurilma"da `Yuborilmoqda`da qotadi;
server dashboard esa `xato` ko'rsatadi.

**Ildiz:** `sms/SmsSender.kt` `resultPi()` PendingIntent'ni *implicit* yasaydi
(`Intent(action).setPackage(pkg)`), `SmsResultReceiver` esa manifestda `<intent-filter>`siz.
Filtersiz manifest receiver implicit broadcast qabul qilmaydi → `onReceive` ishlamaydi →
lokal job `SENDING`da qoladi → "sent" serverga report qilinmaydi → server `expire_and_reclaim`
(lease 60s) xabarni `failed`/`lease_timeout` qiladi.

**Fix:** `resultPi`da explicit intent —
`Intent(context, SmsResultReceiver::class.java).apply { action = ...; putExtra(...) }`.
`intent.action` baribir o'qiladi; sent/delivered farqlanadi. Manifest o'zgarmasa ham bo'ladi.

**delivered:** kod o'zgarmaydi. `sent` = "Yuborildi ✓" asosiy muvaffaqiyat holati (allaqachon
shunday). `✓✓ Yetkazildi` — operator delivery-report qaytarsa (O'zbekistonda ko'pincha kelmaydi).
Message detailга bir qatorli izoh qo'shiladi.

**Check:** `SmsSender`/receiver uchun kichik unit yoki qo'lda telefonда tasdiq (SMS faqat
telefonда sinaladi).

## B. Navigation drawer

`AppRoot.kt`da `ModalNavigationDrawer` Scaffold'ni o'raydi; TopAppBar'ga hamburger
(`navigationIcon`). Drawer tarkibi:
- **Header:** yangi logo + "ownsms" + `versionName` + qurilma id.
- **Ilova haqida** → yangi `AboutScreen`.
- **Foydalanish yo'riqnomasi** → yangi `GuideScreen` (step-by-step).
- **API hujjatlari** → `Intent(ACTION_VIEW, https://sms.omadli.uz/api/v1/docs)` (brauzer).

Bottom-nav (Home/Faollik/SIM/Sozlamalar) primary bo'lib qoladi; drawer — ma'lumot/yordam.

## C. Tap-to-copy API kalit (niqoblangan + reveal)

`SettingsScreen.kt` Account kartasi: API kalit qatori `osk_•••••••1234` ko'rinishда,
o'ngда 👁 (ochish/yashirish) + 📋 (copy). Copy → `ClipboardManager` + "Nusxa olindi" snackbar.
API URL ham copy bo'ladi. Onboarding'да ro'yxatdan o'tgach kalit copy tugmasi bilan ko'rsatiladi.
Yordamchi: `components/CopyableField.kt` (label + niqob + reveal + copy).

## D. Logo/icon

Yangi adaptiv icon: **Ultramarine (#2952E3) suhbat pufagi + yashil (#16C784) ✓✓** (delivered
motiv). `res/drawable/ic_launcher_foreground.xml` (vector) + `ic_launcher_background`.
Drawer header'да ham shu logo. `ic_launcher_round` yangilanadi.

## E. Responsiv

`WindowSizeClass` (material3-window-size-class):
- **Compact** (telefon): bottom-nav + modal drawer.
- **Expanded** (planshet/foldable): doimiy (permanent) drawer / NavigationRail; kontent
  `widthIn(max = 640.dp)` bilan markazда.
Kartalar max-width bilan cheklangan, hamma ekran `verticalScroll`, adaptiv padding.

## F. Step-by-step yo'riqnomalar

`GuideScreen`: 1) SIM+ruxsat → 2) API URL → 3) Ro'yxatdan o'tish → 4) API kalit (copy) →
5) SMS yuborish (curl misoli, copy). Onboarding matnlari soddalashtiriladi.
i18n: `values/strings.xml` + `values-ru/strings.xml` yangi kalitlar bilan yangilanadi.

## Chegaradan tashqari (bu ishда emas)

API soddalashtirish + docs (workstream 2), ownsms sayti (workstream 3), backend o'zgarishlari.

## Qabul mezoni

- Telefonда SMS yuborilgach status `Yuborilmoqda → Yuborildi`ga o'tadi; server `sent` ko'rsatadi
  (endi `lease_timeout` yo'q).
- Drawer'дан Ilova haqida / Yo'riqnoma / API docs ochiladi.
- API kalit bir bosishда nusxa olinadi.
- Yangi logo ko'rinadi.
- Kichik va katta ekranда layout to'g'ri.
- `assembleDebug` + `ktlint` yashil.
