<div align="center">

# 🔐 رازبان مسنجر
### Razban Messenger

**یک پیام‌رسان ایمن، سبک و مقاوم در برابر فیلترینگ**  
*Built on Cloudflare Workers · Backblaze B2 · Google Apps Script*

[![Cloudflare Workers](https://img.shields.io/badge/Cloudflare-Workers-F38020?logo=cloudflare&logoColor=white)](https://workers.cloudflare.com/)
[![Backblaze B2](https://img.shields.io/badge/Backblaze-B2-E12E2E?logo=backblaze&logoColor=white)](https://www.backblaze.com/b2/)
[![Google Apps Script](https://img.shields.io/badge/Google-Apps%20Script-4285F4?logo=google&logoColor=white)](https://script.google.com/)
[![D1 SQLite](https://img.shields.io/badge/Cloudflare-D1%20SQLite-F38020?logo=cloudflare&logoColor=white)](https://developers.cloudflare.com/d1/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📖 درباره پروژه

رازبان یک پیام‌رسان سبک است که روی لبه (Edge) شبکه اجرا می‌شود و هیچ سرور اختصاصی برای اجرا نیاز ندارد. تمام پردازش‌ها روی **Cloudflare Workers** اتفاق می‌افتد، فایل‌ها روی **Backblaze B2** ذخیره می‌شوند، و یک لایه ریلی مبتنی بر **Google Apps Script** برای شرایطی که اتصال مستقیم کاربر ممکن نیست (فیلترینگ، قطعی، تایم‌اوت) وجود دارد.

### ویژگی‌های اصلی

- 💬 چت خصوصی و گروهی
- 🗣 ارسال پیام متنی، صوتی، تصویری و فایل
- ↩️ ریپلای (پاسخ) به پیام
- ✏️ ویرایش پیام متنی
- 🗑 حذف پیام برای خودم / برای همه
- 🟢 وضعیت آنلاین/آفلاین لحظه‌ای (Heartbeat)
- 🔍 جستجوی کاربران
- 🌐 **سه حالت شبکه:** Normal · Timeout · Emergency
- 🛡 پنل ادمین مخفی با احراز هویت سرور-ساید
- 🔒 محافظت کامل در برابر IDOR

---

## 🏗 معماری

```
┌──────────────────────────────────────────────────────────┐
│                      Client (Android APK)                 │
│                                                          │
│  ┌──────────────┐   ┌────────────────┐                   │
│  │  Normal Mode │   │ Emergency Mode │                   │
│  │   (Direct)   │   │  (via Relay)   │                   │
│  └──────┬───────┘   └───────┬────────┘                   │
└─────────┼───────────────────┼───────────────────────────-┘
          │                   │
          │          ┌────────▼─────────────┐
          │          │  Google Apps Script  │
          │          │  (Relay Layer)       │
          │          │  code.gs             │
          │          └────────┬─────────────┘
          │                   │
          └───────────────────▼
                ┌─────────────────────┐
                │  Cloudflare Worker  │
                │  w.js (Edge)        │
                └──────┬──────┬───────┘
                       │      │
          ┌────────────▼──┐ ┌─▼──────────────────┐
          │ Cloudflare D1 │ │   Backblaze B2      │
          │ (SQLite DB)   │ │ (File Storage S3)   │
          └───────────────┘ └─────────────────────┘
```

### لایه‌های پروژه

| فایل | نقش | محیط اجرا |
|------|------|-----------|
| `w.js` | هسته اصلی — API، احراز هویت، منطق کسب‌وکار، پنل ادمین | Cloudflare Workers |
| `code.gs` | ریلی اضطراری — پروکسی درخواست‌ها در شرایط قطعی | Google Apps Script |
| Android APK | کلاینت موبایل — UI چت و مدیریت حالت شبکه | Android |

---

## 🚀 راه‌اندازی

### پیش‌نیازها

- حساب **Cloudflare** (رایگان)
- حساب **Backblaze B2** (رایگان تا ۱۰ گیگ)
- حساب **Google** (برای Google Apps Script)
- [Wrangler CLI](https://developers.cloudflare.com/workers/wrangler/) نسخه ۳ به بالا

```bash
npm install -g wrangler
wrangler login
```

---

### ۱. Cloudflare D1 — راه‌اندازی پایگاه داده

یک دیتابیس D1 بسازید:

```bash
wrangler d1 create razban-db
```

اسکیمای جداول را اجرا کنید:

```sql
-- جدول کاربران
CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  display_name TEXT,
  avatar_url TEXT,
  status TEXT DEFAULT 'offline',
  last_seen DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- جدول سشن‌ها
CREATE TABLE user_sessions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  token TEXT UNIQUE NOT NULL,
  expires_at DATETIME NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- جدول چت‌ها
CREATE TABLE chats (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_type TEXT NOT NULL DEFAULT 'private',
  title TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- جدول اعضای چت
CREATE TABLE chat_participants (
  chat_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  PRIMARY KEY (chat_id, user_id),
  FOREIGN KEY (chat_id) REFERENCES chats(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- جدول پیام‌ها
CREATE TABLE messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  chat_id INTEGER NOT NULL,
  sender_id INTEGER NOT NULL,
  network_mode TEXT DEFAULT 'normal',
  message_type TEXT NOT NULL DEFAULT 'text',
  content TEXT,
  file_name TEXT,
  file_size INTEGER,
  reply_to_id INTEGER,
  is_edited INTEGER DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (chat_id) REFERENCES chats(id),
  FOREIGN KEY (sender_id) REFERENCES users(id)
);

-- جدول تاریخچه ویرایش/حذف
CREATE TABLE message_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  message_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  action TEXT NOT NULL,
  old_content TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- جدول پیام‌های پنهان‌شده (حذف برای من)
CREATE TABLE message_deletions (
  user_id INTEGER NOT NULL,
  message_id INTEGER NOT NULL,
  PRIMARY KEY (user_id, message_id)
);
```

```bash
wrangler d1 execute razban-db --file=./schema.sql
```

---

### ۲. Backblaze B2 — راه‌اندازی ذخیره‌سازی فایل

1. وارد [Backblaze B2](https://www.backblaze.com/b2/cloud-storage.html) شوید و یک Bucket عمومی بسازید.
2. یک **Application Key** با دسترسی `readFiles` و `writeFiles` برای آن باکت بسازید.
3. مقادیر زیر را یادداشت کنید:
   - `keyID` → `B2_ACCESS_KEY_ID`
   - `applicationKey` → `B2_SECRET_ACCESS_KEY`
   - `Endpoint` (مثلاً `s3.us-west-004.backblazeb2.com`) → `B2_ENDPOINT`
   - نام باکت → `B2_BUCKET_NAME`

---

### ۳. wrangler.toml — پیکربندی ورکر

```toml
name = "razban"
main = "w.js"
compatibility_date = "2024-01-01"

[[d1_databases]]
binding = "DB"
database_name = "razban-db"
database_id = "<your-d1-database-id>"

[vars]
B2_ENDPOINT = "s3.us-west-004.backblazeb2.com"
B2_BUCKET_NAME = "razban-files"
```

متغیرهای حساس را با Wrangler Secret ذخیره کنید (هرگز در کد ننویسید):

```bash
wrangler secret put B2_ACCESS_KEY_ID
wrangler secret put B2_SECRET_ACCESS_KEY
```

---

### ۴. اطلاعات ادمین — تنظیم در کد

در فایل `w.js` در بالای فایل، اطلاعات ادمین را مستقیم تنظیم کنید:

```js
const ADMIN_USERNAME = "your_admin_username";
const ADMIN_PASSWORD = "your_strong_password";
```

> ⚠️ این اطلاعات فقط سرور-ساید هستند و هرگز به مرورگر کاربر ارسال نمی‌شوند.

---

### ۵. Deploy ورکر

```bash
wrangler deploy
```

---

### ۶. راه‌اندازی ریلی گوگل (code.gs)

1. به [Google Apps Script](https://script.google.com) بروید و یک پروژه جدید بسازید.
2. محتوای `code.gs` را کپی کنید.
3. آدرس ورکر خودتان را در بالای فایل تنظیم کنید:
   ```js
   const CLOUDFLARE_WORKER_URL = "https://your-worker.workers.dev";
   ```
4. از منو: **Deploy → New Deployment → Web App**
   - Execute as: `Me`
   - Who has access: `Anyone`
5. آدرس Web App تولیدشده را کپی کنید — این همان **Relay URL** است.

---

## 📡 مستندات API

همه endpoint ها نیاز به هدر زیر دارند (به‌جز لاگین و ثبت‌نام):

```
Authorization: Bearer <token>
```

---

### احراز هویت

#### ثبت‌نام
```http
POST /api/register
Content-Type: application/json

{
  "username": "ali",
  "password": "strongPass123",
  "display_name": "علی"
}
```

#### ورود
```http
POST /api/login
Content-Type: application/json

{
  "username": "ali",
  "password": "strongPass123"
}
```

**پاسخ:**
```json
{
  "success": true,
  "token": "abc123...",
  "username": "ali",
  "display_name": "علی",
  "user": { "id": 1, "username": "ali", "display_name": "علی" }
}
```

---

### پیام‌ها

#### ارسال پیام متنی
```http
POST /api/messages/send
Content-Type: application/json

{
  "chat_id": "42",
  "message_type": "text",
  "content": "سلام!",
  "network_mode": "normal",
  "reply_to_id": "15"    // اختیاری
}
```

#### ارسال فایل / ویس / تصویر
```http
POST /api/messages/send
Content-Type: multipart/form-data

chat_id=42
message_type=voice        // voice | photo | file
network_mode=timeout
file=<binary>
reply_to_id=15            // اختیاری
```

#### دریافت تاریخچه پیام‌ها
```http
GET /api/messages/get?chat_id=42&limit=50
```

#### ویرایش پیام
```http
POST /api/messages/edit
Content-Type: application/json

{
  "message_id": "77",
  "content": "متن ویرایش‌شده"
}
```

#### حذف پیام
```http
POST /api/messages/delete
Content-Type: application/json

{
  "message_id": "77",
  "delete_type": "for_me"         // for_me | for_everyone
}
```

---

### چت‌ها

#### ساخت چت جدید
```http
POST /api/chats/create
Content-Type: application/json

{
  "chat_type": "private",
  "target_user_id": 5
}
```

#### لیست چت‌ها
```http
GET /api/chats
```

---

### کاربران

#### جستجوی کاربر
```http
GET /api/users/search?query=ali
```

#### ضربان قلب (Heartbeat)
```http
POST /api/users/heartbeat
Content-Type: application/json

{ "status": "online" }
```

> کلاینت باید این endpoint را هر ۳۰–۶۰ ثانیه صدا بزند تا وضعیت آنلاین حفظ شود. کاربر بعد از ۹۰ ثانیه غیرفعال، آفلاین نمایش داده می‌شود.

---

## 🌐 سیستم حالت‌های شبکه

رازبان سه حالت شبکه دارد که به کلاینت اجازه می‌دهد در هر شرایطی به سرور متصل شود:

| حالت | مسیر | محدودیت | کاربرد |
|------|------|---------|--------|
| `normal` | مستقیم به ورکر | بدون محدودیت | اتصال معمولی |
| `timeout` | از طریق ریلی گوگل | فایل حداکثر ۲۰ مگابایت | ورکر کند یا ناپایدار |
| `emergency` | از طریق ریلی گوگل | فقط پیام متنی | ورکر کاملاً مسدود |

فیلد `network_mode` در body پیام ذخیره می‌شود و در پنل ادمین قابل مشاهده است.

---

## 🔐 پنل ادمین

پنل ادمین در آدرس زیر در دسترس است:

```
https://your-worker.workers.dev/.hidden/core
```

### ویژگی‌ها
- 📊 **کنسول لاگ** — مشاهده تمام پیام‌ها با جزئیات (حالت شبکه، نوع پیام، فرستنده)
- 💬 **مسنجر ادمین** — ورود به هر چت و مشاهده تاریخچه
- 👥 **مدیریت اکانت‌ها** — ویرایش نام کاربری، نمایشی، تغییر دستی وضعیت آنلاین/آفلاین

### امنیت پنل
- احراز هویت کاملاً **سرور-ساید** با HMAC-SHA256
- اطلاعات ادمین **هرگز در HTML یا view source** ظاهر نمی‌شوند
- توکن پنل در هر بار لاگین مجدداً از سرور صادر می‌شود

---

## 🛡 امنیت

| ویژگی | جزئیات |
|-------|--------|
| هش رمز عبور | SHA-256 |
| توکن سشن | UUID دوبل (۶۴ کاراکتر تصادفی) با انقضای ۳۰ روزه |
| محافظت IDOR | بررسی `chat_participants` قبل از هر عملیات پیام |
| XSS Sanitization | حذف تمام تگ‌های HTML از محتوای پیام |
| اطلاعات ادمین | فقط سرور-ساید، احراز هویت HMAC |
| آپلود فایل | نام فایل UUID تصادفی در B2، پسوند sanitized |

---

## 📁 ساختار مخزن

```
razban/
├── w.js                # ورکر اصلی Cloudflare Workers
├── code.gs             # ریلی گوگل Apps Script
├── schema.sql          # اسکیمای پایگاه داده D1
└── README.md
```

---

## 🤝 مشارکت

Pull Request ها خوش‌آمدند. برای تغییرات بزرگ، لطفاً ابتدا یک Issue باز کنید تا درباره تغییر مورد نظر بحث شود.

---

## 📄 لایسنس

این پروژه تحت لایسنس [MIT](LICENSE) منتشر شده است.

---
# ⚠️ وضعیت پروژه

رازبان هنوز در مرحله توسعه است و نسخه فعلی کامل نیست.

## راه‌اندازی به زبان ساده

1. فایل `w.js` را داخل Cloudflare Workers قرار دهید و Deploy کنید.
2. فایل `code.gs` را داخل Google Apps Script کپی کنید و به صورت Web App منتشر کنید.
3. در برنامه، آدرس **Worker URL** و **Relay URL** را وارد کنید.

> هر کاربر باید Worker و Relay مخصوص خودش را داشته باشد و فقط یک بار آن‌ها را وارد کند. برنامه این اطلاعات را روی دستگاه ذخیره می‌کند و فقط در صورت انتخاب گزینه **Reset Server** نیاز به وارد کردن دوباره خواهد بود.

> اگر داخل کلودفلر `w.js` deploy نشد اسم فایل رو به `worker.js` تغییر بدید

## امکاناتی که فعلاً در دسترس نیستند

این نسخه هنوز کامل نیست و برخی قابلیت‌ها در حال توسعه هستند.

- ❌ ارسال فایل
- ❌ ارسال پیام صوتی (Voice)

این قابلیت‌ها در آپدیت‌های بعدی اضافه خواهند شد.

## برنامه‌های آینده

- ✅ ارسال فایل
- ✅ ارسال پیام صوتی
- ✅ بهبود رابط کاربری
- ✅ قابلیت‌های بیشتر و رفع باگ‌ها

از آنجایی که پروژه هنوز در حال توسعه است، در نسخه‌های آینده تغییرات و امکانات جدید زیادی به آن اضافه خواهد شد.
---

<div align="center">

ساخته‌شده با ☕ و Cloudflare Edge

</div>
