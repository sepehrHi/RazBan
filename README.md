# 🔐 Razban Messenger

**A secure, lightweight messenger resistant to filtering**
*Built on Cloudflare Workers · Backblaze B2 · Google Apps Script · Android (Kotlin)*

[![Backend Lint](https://github.com/sepehrHi/RazBan/actions/workflows/backend-lint.yml/badge.svg)](https://github.com/sepehrHi/RazBan/actions/workflows/backend-lint.yml)
[![Android Build](https://github.com/sepehrHi/RazBan/actions/workflows/android-build.yml/badge.svg)](https://github.com/sepehrHi/RazBan/actions/workflows/android-build.yml)
[![Deploy Cloudflare Worker](https://github.com/sepehrHi/RazBan/actions/workflows/cloudflare-deploy.yml/badge.svg)](https://github.com/sepehrHi/RazBan/actions/workflows/cloudflare-deploy.yml)
[![Cloudflare Workers](https://img.shields.io/badge/Cloudflare-Workers-F38020?logo=cloudflare&logoColor=white)](https://workers.cloudflare.com/)
[![Backblaze B2](https://img.shields.io/badge/Backblaze-B2-E12E2E?logo=backblaze&logoColor=white)](https://www.backblaze.com/b2/)
[![Google Apps Script](https://img.shields.io/badge/Google-Apps%20Script-4285F4?logo=google&logoColor=white)](https://script.google.com/)
[![D1 SQLite](https://img.shields.io/badge/Cloudflare-D1%20SQLite-F38020?logo=cloudflare&logoColor=white)](https://developers.cloudflare.com/d1/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[مستندات فارسی 🇮🇷](docs/README.fa.md)

---

## 📖 About the Project

Razban is a lightweight messenger that runs at the network edge and requires no dedicated server.

All processing happens on **Cloudflare Workers**, files are stored on **Backblaze B2**, and a fallback relay layer using **Google Apps Script** is used when direct connectivity is unavailable (filtering, outages, timeouts).

### Key Features

- 💬 Private and group chats
- 🗣 Send text, voice, image, and file messages
- ↩️ Reply to messages
- ✏️ Edit messages
- 🗑 Delete messages (for me / for everyone)
- 🟢 Real-time online/offline status (heartbeat)
- 🔍 User search
- 🌐 **Three network modes:** Normal · Timeout · Emergency
- 🛡 Hidden admin panel with server-side authentication
- 🔒 Protection against IDOR attacks

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Client (Android APK)                    │
│                                                              │
│  ┌──────────────┐   ┌────────────────┐                     │
│  │  Normal Mode │   │ Emergency Mode │                     │
│  │   (Direct)   │   │  (via Relay)   │                     │
│  └──────┬───────┘   └───────┬────────┘                     │
└─────────┼───────────────────┼──────────────────────────────┘
          │                   │
          │          ┌────────▼─────────────┐
          │          │  Google Apps Script   │
          │          │  (Relay Layer)        │
          │          │  apps/gas-relay       │
          │          └────────┬──────────────┘
          │                   │
          └───────────────────▼
                ┌──────────────────────┐
                │  Cloudflare Worker    │
                │  apps/backend-cloudflare │
                └──────┬──────┬─────────┘
                       │      │
          ┌────────────▼──┐ ┌─▼──────────────────┐
          │ Cloudflare D1 │ │   Backblaze B2      │
          │ (SQLite DB)   │ │ (File Storage S3)   │
          └───────────────┘ └──────────────────────┘
```

---

## 📁 Project Structure

```
RazBan/
├── apps/
│   ├── backend-cloudflare/   # Core API (Cloudflare Worker) + D1 schema
│   │   ├── src/worker.js
│   │   ├── schema.sql
│   │   ├── wrangler.toml
│   │   └── package.json
│   ├── gas-relay/            # Emergency/timeout relay layer
│   │   └── code.gs
│   └── android/              # Android client (Kotlin, Jetpack Compose)
├── docs/
│   └── README.fa.md          # Persian documentation
├── .github/workflows/        # CI/CD (lint, build, deploy)
└── README.md
```

---

## 🚀 Setup

### Requirements

- **Cloudflare account** (free)
- **Backblaze B2 account** (free tier available)
- **Google account** (for Apps Script)
- [Wrangler CLI](https://developers.cloudflare.com/workers/wrangler/) v3+

```bash
npm install -g wrangler
wrangler login
```

### 1. Cloudflare D1 — Database

```bash
cd apps/backend-cloudflare
wrangler d1 create razban-db
wrangler d1 execute razban-db --file=./schema.sql
```

Update `database_id` in `apps/backend-cloudflare/wrangler.toml` with the ID returned above.

### 2. Backblaze B2 — File Storage

1. Create a bucket in Backblaze B2
2. Generate an Application Key with `readFiles` + `writeFiles`
3. Set secrets:

```bash
wrangler secret put B2_ACCESS_KEY_ID
wrangler secret put B2_SECRET_ACCESS_KEY
```

### 3. Deploy the Worker

```bash
cd apps/backend-cloudflare
npm install
wrangler deploy
```

Or push to `main` — the `cloudflare-deploy.yml` workflow deploys automatically once
`CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` are set as repo secrets
(**Settings → Secrets and variables → Actions**).

### 4. Google Apps Script Relay

1. Create a new project at [script.google.com](https://script.google.com/)
2. Paste the contents of `apps/gas-relay/code.gs`
3. Set your deployed worker URL:
   ```js
   const CLOUDFLARE_WORKER_URL = "https://your-worker.workers.dev";
   ```
4. Deploy as Web App → Execute as **Me**, Access **Anyone**
5. Copy the Web App URL — this is your **Relay URL**

### 5. Android App

```bash
cd apps/android
./gradlew assembleDebug
```

---

## 📡 API Overview

All endpoints require `Authorization: Bearer <token>` except login/register.

| Endpoint | Method | Description |
|---|---|---|
| `/api/register` | POST | Create account |
| `/api/login` | POST | Authenticate, returns token |
| `/api/messages/send` | POST | Send text / multipart file |
| `/api/messages/get` | GET | Fetch messages for a chat |
| `/api/messages/edit` | POST | Edit a message |
| `/api/messages/delete` | POST | Delete a message |
| `/api/chats/create` | POST | Create private/group chat |
| `/api/chats` | GET | List chats |
| `/api/users/search` | GET | Search users |
| `/api/users/heartbeat` | POST | Update online status |

---

## 🌐 Network Modes

| Mode | Route | Limit | Use Case |
|---|---|---|---|
| `normal` | Direct to Worker | none | Normal usage |
| `timeout` | Google relay | 20MB | Unstable worker connection |
| `emergency` | Google relay | text only | Full network blockage |

---

## 🔐 Admin Panel

Available at `https://your-worker.workers.dev/.hidden/core`, protected by
server-side, HMAC-signed authentication — credentials never reach the client.

> **Note:** Since this path lives in a public repository, treat it as a
> known path, not a secret. Rotate admin credentials regularly and consider
> adding IP allow-listing or Cloudflare Access in front of it for extra protection.

---

## 🛡 Security Notes

- SHA-256 password hashing
- Long-lived random session tokens
- IDOR protection via chat/message ownership checks
- XSS sanitization on rendered content
- Randomized file names in B2 storage

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to
discuss what you'd like to change.

## 📄 License

MIT License — see [LICENSE](LICENSE).

## ⚠️ Project Status

Razban is under active development and not fully complete.

**Known limitations:** file sending is not fully stable · voice messages are not fully implemented.

**Planned:** stable file uploads · voice messages · UI improvements · bug fixes.

---

<div align="center">

Built with ☕ and Cloudflare Edge

</div>
