
const ADMIN_USERNAME = "admin";
const ADMIN_PASSWORD = "admin";

class S3Signer {
  constructor({ accessKeyId, secretAccessKey, endpoint, region }) {
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.endpoint = endpoint.replace(/\/$/, "");
    this.region = region || "us-east-1";
  }

  async signPutRequest(bucketName, fileName, fileBuffer, contentType) {
    const url = `https://${this.endpoint}/${bucketName}/${fileName}`;
    const datetime = new Date().toISOString().replace(/[:\-]\.\d{3}/g, "");
    const date = datetime.substr(0, 8);
    const hashedPayload = await this.sha256Hex(fileBuffer);
    const headers = {
      "host": this.endpoint,
      "x-amz-content-sha256": hashedPayload,
      "x-amz-date": datetime,
      "content-type": contentType
    };
    const canonicalHeaders = Object.keys(headers).sort().map(k => `${k}:${headers[k].trim()}`).join("\n") + "\n";
    const signedHeaders = Object.keys(headers).sort().join(";");
    const canonicalRequest = ["PUT", `/${bucketName}/${fileName}`, "", canonicalHeaders, signedHeaders, hashedPayload].join("\n");
    const credentialScope = `${date}/${this.region}/s3/aws4_request`;
    const stringToSign = ["AWS4-HMAC-SHA256", datetime, credentialScope, await this.sha256Hex(new TextEncoder().encode(canonicalRequest))].join("\n");
    const kDate = await this.hmac(new TextEncoder().encode("AWS4" + this.secretAccessKey), date);
    const kRegion = await this.hmac(kDate, this.region);
    const kService = await this.hmac(kRegion, "s3");
    const kSigning = await this.hmac(kService, "aws4_request");
    const signature = this.bufferToHex(await this.hmac(kSigning, stringToSign));
    headers["Authorization"] = `AWS4-HMAC-SHA256 Credential=${this.accessKeyId}/${credentialScope}, SignedHeaders=${signedHeaders}, Signature=${signature}`;
    return { url, headers };
  }

  async hmac(key, data) {
    const cryptoKey = await crypto.subtle.importKey("raw", key, { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
    return await crypto.subtle.sign("HMAC", cryptoKey, typeof data === "string" ? new TextEncoder().encode(data) : data);
  }

  async sha256Hex(data) {
    const hash = await crypto.subtle.digest("SHA-256", data);
    return this.bufferToHex(hash);
  }

  bufferToHex(buffer) {
    return Array.from(new Uint8Array(buffer)).map(b => b.toString(16).padStart(2, "0")).join("");
  }
}

function getAdminPanelHTML() {
  return `<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>رازبان | پنل کنترل</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  :root {
    --bg: #0a0a0f;
    --surface: #111118;
    --surface2: #1a1a24;
    --border: #2a2a3a;
    --accent: #7c5cfc;
    --accent2: #5c8afc;
    --danger: #fc5c5c;
    --warn: #fcb45c;
    --success: #5cfca0;
    --text: #e8e8f0;
    --muted: #7a7a9a;
    --online: #5cfca0;
    --offline: #7a7a9a;
  }
  body { background: var(--bg); color: var(--text); font-family: 'Segoe UI', Tahoma, sans-serif; min-height: 100vh; }

  /* LOGIN */
  #loginScreen {
    display: flex; align-items: center; justify-content: center;
    min-height: 100vh;
    background: radial-gradient(ellipse at 50% 0%, rgba(124,92,252,0.15) 0%, transparent 70%);
  }
  .loginBox {
    background: var(--surface); border: 1px solid var(--border);
    border-radius: 20px; padding: 48px 40px; width: 360px;
    box-shadow: 0 30px 80px rgba(0,0,0,0.5);
  }
  .loginBox .logo { text-align: center; margin-bottom: 32px; }
  .loginBox .logo h1 { font-size: 24px; font-weight: 700; background: linear-gradient(135deg, var(--accent), var(--accent2)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
  .loginBox .logo p { color: var(--muted); font-size: 13px; margin-top: 6px; }
  .loginBox input {
    width: 100%; padding: 14px 16px; background: var(--surface2);
    border: 1px solid var(--border); border-radius: 12px; color: var(--text);
    font-size: 14px; margin-bottom: 14px; outline: none; transition: border-color 0.2s;
  }
  .loginBox input:focus { border-color: var(--accent); }
  .loginBox button {
    width: 100%; padding: 14px; background: linear-gradient(135deg, var(--accent), var(--accent2));
    border: none; border-radius: 12px; color: #fff; font-size: 15px; font-weight: 600;
    cursor: pointer; transition: opacity 0.2s;
  }
  .loginBox button:hover { opacity: 0.9; }
  .loginError { color: var(--danger); font-size: 13px; margin-top: 10px; text-align: center; display: none; }

  /* LAYOUT */
  #adminPanel { display: none; flex-direction: row; min-height: 100vh; }
  .sidebar {
    width: 220px; min-width: 220px; background: var(--surface);
    border-left: 1px solid var(--border); display: flex; flex-direction: column; padding: 24px 0;
  }
  .sidebar .brand { padding: 0 20px 24px; border-bottom: 1px solid var(--border); }
  .sidebar .brand h2 { font-size: 16px; font-weight: 700; background: linear-gradient(135deg, var(--accent), var(--accent2)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
  .sidebar .brand p { color: var(--muted); font-size: 11px; margin-top: 2px; }
  .sidebar nav { flex: 1; padding: 16px 0; }
  .navItem {
    display: flex; align-items: center; gap: 12px; padding: 12px 20px;
    color: var(--muted); font-size: 14px; cursor: pointer; transition: all 0.2s;
    border-right: 3px solid transparent;
  }
  .navItem:hover { background: var(--surface2); color: var(--text); }
  .navItem.active { background: rgba(124,92,252,0.1); color: var(--accent); border-right-color: var(--accent); }
  .navItem .icon { font-size: 18px; }
  .sidebar .logoutBtn {
    margin: 16px; padding: 10px; background: rgba(252,92,92,0.1);
    border: 1px solid rgba(252,92,92,0.2); border-radius: 10px;
    color: var(--danger); font-size: 13px; cursor: pointer; text-align: center; transition: all 0.2s;
  }
  .sidebar .logoutBtn:hover { background: rgba(252,92,92,0.2); }

  /* CONTENT */
  .content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
  .topbar {
    padding: 20px 28px; border-bottom: 1px solid var(--border);
    display: flex; align-items: center; justify-content: space-between;
    background: var(--surface);
  }
  .topbar h1 { font-size: 18px; font-weight: 700; }
  .topbar .stats { display: flex; gap: 20px; }
  .statBadge {
    padding: 6px 14px; background: var(--surface2); border: 1px solid var(--border);
    border-radius: 8px; font-size: 12px; color: var(--muted);
  }
  .statBadge span { color: var(--text); font-weight: 600; margin-right: 4px; }

  .tabContent { flex: 1; overflow: hidden; display: none; }
  .tabContent.active { display: flex; flex-direction: column; }

  /* CONSOLE */
  .consoleBar {
    padding: 16px 24px; border-bottom: 1px solid var(--border);
    display: flex; gap: 10px; flex-wrap: wrap; align-items: center; background: var(--surface);
  }
  .filterBtn {
    padding: 7px 16px; border-radius: 8px; border: 1px solid var(--border);
    background: var(--surface2); color: var(--muted); font-size: 13px; cursor: pointer; transition: all 0.2s;
  }
  .filterBtn.active { background: var(--accent); border-color: var(--accent); color: #fff; }
  .consoleSearch {
    flex: 1; padding: 8px 14px; background: var(--surface2); border: 1px solid var(--border);
    border-radius: 8px; color: var(--text); font-size: 13px; outline: none; min-width: 200px;
  }
  .consoleSearch:focus { border-color: var(--accent); }
  .consoleList { flex: 1; overflow-y: auto; padding: 16px 24px; display: flex; flex-direction: column; gap: 8px; }
  .logEntry {
    background: var(--surface); border: 1px solid var(--border); border-radius: 12px;
    padding: 14px 18px; display: flex; align-items: flex-start; gap: 14px;
    transition: border-color 0.2s;
  }
  .logEntry:hover { border-color: var(--accent); }
  .logEntry .logIcon { font-size: 20px; flex-shrink: 0; }
  .logEntry .logBody { flex: 1; }
  .logEntry .logTitle { font-size: 14px; font-weight: 600; margin-bottom: 4px; }
  .logEntry .logMeta { font-size: 12px; color: var(--muted); }
  .logEntry .logTime { font-size: 11px; color: var(--muted); white-space: nowrap; }
  .tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; margin-right: 6px; }
  .tag.login { background: rgba(92,252,160,0.15); color: var(--success); }
  .tag.register { background: rgba(92,138,252,0.15); color: var(--accent2); }
  .tag.edit { background: rgba(252,180,92,0.15); color: var(--warn); }
  .tag.delete { background: rgba(252,92,92,0.15); color: var(--danger); }
  .tag.newchat { background: rgba(124,92,252,0.15); color: var(--accent); }

  /* MESSENGER */
  .messengerLayout { flex: 1; display: flex; overflow: hidden; }
  .chatList { width: 280px; min-width: 280px; border-left: 1px solid var(--border); overflow-y: auto; background: var(--surface); }
  .chatList .header { padding: 18px 20px; border-bottom: 1px solid var(--border); font-size: 14px; font-weight: 600; color: var(--muted); }
  .chatListItem {
    padding: 16px 20px; border-bottom: 1px solid rgba(42,42,58,0.5);
    cursor: pointer; transition: background 0.15s; display: flex; align-items: center; gap: 12px;
  }
  .chatListItem:hover { background: var(--surface2); }
  .chatListItem.active { background: rgba(124,92,252,0.1); }
  .chatAvatar {
    width: 42px; height: 42px; border-radius: 50%; background: linear-gradient(135deg, var(--accent), var(--accent2));
    display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; flex-shrink: 0;
    position: relative;
  }
  .chatAvatar .dot {
    position: absolute; bottom: 1px; left: 1px; width: 11px; height: 11px;
    border-radius: 50%; border: 2px solid var(--surface); background: var(--offline);
  }
  .chatAvatar .dot.online { background: var(--online); }
  .chatInfo { flex: 1; min-width: 0; }
  .chatInfo .name { font-size: 14px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .chatInfo .preview { font-size: 12px; color: var(--muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 3px; }

  .chatWindow { flex: 1; display: flex; flex-direction: column; background: var(--bg); }
  .chatWindowEmpty { flex: 1; display: flex; align-items: center; justify-content: center; color: var(--muted); flex-direction: column; gap: 12px; }
  .chatWindowEmpty .emptyIcon { font-size: 48px; opacity: 0.3; }

  .chatHeader {
    padding: 16px 24px; border-bottom: 1px solid var(--border);
    background: var(--surface); display: flex; align-items: center; justify-content: space-between;
  }
  .chatHeaderInfo { display: flex; align-items: center; gap: 12px; }
  .chatHeaderActions { display: flex; gap: 8px; }
  .chatHeaderActions button {
    padding: 8px 16px; border-radius: 8px; border: none; font-size: 13px; cursor: pointer; font-weight: 600; transition: all 0.2s;
  }
  .btnDanger { background: rgba(252,92,92,0.15); color: var(--danger); border: 1px solid rgba(252,92,92,0.3) !important; }
  .btnDanger:hover { background: rgba(252,92,92,0.25) !important; }
  .btnWarn { background: rgba(252,180,92,0.15); color: var(--warn); border: 1px solid rgba(252,180,92,0.3) !important; }

  .messagesArea { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; gap: 10px; }
  .msgBubble {
    max-width: 70%; padding: 12px 16px; border-radius: 16px; font-size: 14px; line-height: 1.6;
    position: relative; cursor: pointer; transition: all 0.15s;
  }
  .msgBubble:hover { opacity: 0.9; }
  .msgBubble.mine { background: linear-gradient(135deg, var(--accent), var(--accent2)); align-self: flex-end; border-bottom-left-radius: 4px; }
  .msgBubble.theirs { background: var(--surface2); border: 1px solid var(--border); align-self: flex-start; border-bottom-right-radius: 4px; }
  .msgBubble .msgMeta { font-size: 11px; opacity: 0.7; margin-top: 6px; display: flex; gap: 8px; }
  .msgBubble .editedTag { font-size: 11px; opacity: 0.6; }
  .msgWrapper { display: flex; flex-direction: column; }
  .msgWrapper .senderLabel { font-size: 12px; color: var(--muted); margin-bottom: 4px; padding: 0 4px; }
  .msgWrapper.mine { align-items: flex-end; }
  .msgWrapper.theirs { align-items: flex-start; }

  .msgActions {
    display: none; position: absolute; top: -40px; left: 50%; transform: translateX(-50%);
    background: var(--surface); border: 1px solid var(--border); border-radius: 10px;
    padding: 6px; gap: 4px; flex-direction: row; box-shadow: 0 4px 20px rgba(0,0,0,0.4); white-space: nowrap;
    z-index: 10;
  }
  .msgBubble:hover .msgActions { display: flex; }
  .msgActionBtn {
    padding: 5px 10px; border-radius: 6px; border: none; font-size: 12px; cursor: pointer; background: var(--surface2); color: var(--text); transition: all 0.15s;
  }
  .msgActionBtn:hover { background: var(--accent); color: #fff; }
  .msgActionBtn.del:hover { background: var(--danger); }

  .adminSendBar {
    padding: 16px 24px; border-top: 1px solid var(--border); background: var(--surface);
    display: flex; gap: 10px; align-items: flex-end;
  }
  .adminSendBar .senderPicker {
    padding: 10px 12px; background: var(--surface2); border: 1px solid var(--border);
    border-radius: 10px; color: var(--text); font-size: 13px; outline: none; min-width: 130px;
  }
  .adminSendBar .senderPicker:focus { border-color: var(--accent); }
  .adminSendBar textarea {
    flex: 1; padding: 12px 16px; background: var(--surface2); border: 1px solid var(--border);
    border-radius: 12px; color: var(--text); font-size: 14px; resize: none; outline: none;
    font-family: inherit; min-height: 46px; max-height: 120px;
  }
  .adminSendBar textarea:focus { border-color: var(--accent); }
  .adminSendBar .sendBtn {
    padding: 12px 20px; background: linear-gradient(135deg, var(--accent), var(--accent2));
    border: none; border-radius: 12px; color: #fff; font-size: 20px; cursor: pointer;
    transition: opacity 0.2s; flex-shrink: 0;
  }
  .adminSendBar .sendBtn:hover { opacity: 0.85; }

  /* ACCOUNTS */
  .accountsBar {
    padding: 16px 24px; border-bottom: 1px solid var(--border); background: var(--surface);
    display: flex; gap: 10px; align-items: center;
  }
  .accountsBar input {
    flex: 1; padding: 10px 16px; background: var(--surface2); border: 1px solid var(--border);
    border-radius: 10px; color: var(--text); font-size: 14px; outline: none;
  }
  .accountsBar input:focus { border-color: var(--accent); }
  .accountsGrid { flex: 1; overflow-y: auto; padding: 24px; display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; align-content: start; }
  .accountCard {
    background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 20px;
    transition: border-color 0.2s;
  }
  .accountCard:hover { border-color: var(--accent); }
  .accountCard .cardTop { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
  .accountCard .avatar {
    width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, var(--accent), var(--accent2));
    display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700; flex-shrink: 0;
    position: relative;
  }
  .accountCard .avatar .statusDot {
    position: absolute; bottom: 1px; right: 1px; width: 13px; height: 13px;
    border-radius: 50%; border: 2px solid var(--surface);
  }
  .accountCard .userInfo .displayName { font-size: 16px; font-weight: 700; }
  .accountCard .userInfo .username { font-size: 13px; color: var(--muted); margin-top: 2px; }
  .accountCard .userInfo .userId { font-size: 11px; color: var(--muted); margin-top: 1px; }
  .accountCard .editFields { display: flex; flex-direction: column; gap: 8px; }
  .accountCard .editFields input {
    padding: 9px 12px; background: var(--surface2); border: 1px solid var(--border);
    border-radius: 8px; color: var(--text); font-size: 13px; outline: none;
  }
  .accountCard .editFields input:focus { border-color: var(--accent); }
  .accountCard .cardActions { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
  .accountCard .cardActions button {
    flex: 1; padding: 8px 12px; border-radius: 8px; border: none; font-size: 13px; cursor: pointer; font-weight: 600; transition: all 0.2s; min-width: 80px;
  }
  .btnPrimary { background: linear-gradient(135deg, var(--accent), var(--accent2)) !important; color: #fff !important; }
  .btnPrimary:hover { opacity: 0.9; }
  .statusBadge { display: inline-block; padding: 3px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
  .statusBadge.online { background: rgba(92,252,160,0.15); color: var(--success); }
  .statusBadge.offline { background: rgba(122,122,154,0.15); color: var(--muted); }

  /* MODAL */
  .modal { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 100; align-items: center; justify-content: center; }
  .modal.open { display: flex; }
  .modalBox {
    background: var(--surface); border: 1px solid var(--border); border-radius: 20px;
    padding: 28px; width: 420px; max-width: 90vw; box-shadow: 0 30px 80px rgba(0,0,0,0.5);
  }
  .modalBox h3 { font-size: 18px; margin-bottom: 16px; }
  .modalBox p { color: var(--muted); font-size: 14px; line-height: 1.7; margin-bottom: 20px; }
  .modalActions { display: flex; gap: 10px; justify-content: flex-end; }
  .modalActions button {
    padding: 10px 22px; border-radius: 10px; border: none; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s;
  }
  .btnCancel { background: var(--surface2); color: var(--text); border: 1px solid var(--border) !important; }
  .btnConfirmDanger { background: var(--danger); color: #fff; }
  .btnConfirmDanger:hover { opacity: 0.85; }

  /* TOAST */
  #toast {
    position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%);
    background: var(--surface); border: 1px solid var(--border); border-radius: 12px;
    padding: 12px 24px; font-size: 14px; box-shadow: 0 10px 40px rgba(0,0,0,0.4);
    z-index: 200; opacity: 0; transition: opacity 0.3s; pointer-events: none;
  }
  #toast.show { opacity: 1; }
  #toast.success { border-color: var(--success); color: var(--success); }
  #toast.error { border-color: var(--danger); color: var(--danger); }

  /* SCROLLBAR */
  ::-webkit-scrollbar { width: 6px; }
  ::-webkit-scrollbar-track { background: transparent; }
  ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 3px; }
  ::-webkit-scrollbar-thumb:hover { background: var(--muted); }

  /* LOADING */
  .loading { text-align: center; padding: 40px; color: var(--muted); }
  .spinner { display: inline-block; width: 24px; height: 24px; border: 3px solid var(--border); border-top-color: var(--accent); border-radius: 50%; animation: spin 0.8s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }

  .emptyState { text-align: center; padding: 60px 20px; color: var(--muted); }
  .emptyState .icon { font-size: 48px; margin-bottom: 12px; opacity: 0.4; }
</style>
</head>
<body>

<!-- LOGIN -->
<div id="loginScreen">
  <div class="loginBox">
    <div class="logo">
      <h1>🛡️ رازبان ادمین</h1>
      <p>پنل کنترل مرکزی</p>
    </div>
    <input type="text" id="adminUser" placeholder="نام کاربری ادمین" autocomplete="off">
    <input type="password" id="adminPass" placeholder="رمز عبور ادمین">
    <button onclick="adminLogin()">ورود به پنل</button>
    <div class="loginError" id="loginError">نام کاربری یا رمز عبور اشتباه است</div>
  </div>
</div>

<!-- PANEL -->
<div id="adminPanel">
  <div class="sidebar">
    <div class="brand">
      <h2>🛡️ رازبان ادمین</h2>
      <p>پنل کنترل مرکزی</p>
    </div>
    <nav>
      <div class="navItem active" onclick="switchTab('console')" id="nav-console">
        <span class="icon">📋</span> کنسول لاگ
      </div>
      <div class="navItem" onclick="switchTab('messenger')" id="nav-messenger">
        <span class="icon">💬</span> مسنجر ادمین
      </div>
      <div class="navItem" onclick="switchTab('accounts')" id="nav-accounts">
        <span class="icon">👥</span> مدیریت اکانت‌ها
      </div>
    </nav>
    <div class="logoutBtn" onclick="adminLogout()">خروج از پنل</div>
  </div>

  <div class="content">
    <div class="topbar">
      <h1 id="pageTitle">کنسول لاگ</h1>
      <div class="stats" id="topStats"></div>
    </div>

    <!-- CONSOLE TAB -->
    <div class="tabContent active" id="tab-console">
      <div class="consoleBar">
        <button class="filterBtn active" onclick="setFilter('all', this)">همه</button>
        <button class="filterBtn" onclick="setFilter('login', this)">لاگین ✅</button>
        <button class="filterBtn" onclick="setFilter('register', this)">ثبت‌نام 🆕</button>
        <button class="filterBtn" onclick="setFilter('edit', this)">ویرایش ✏️</button>
        <button class="filterBtn" onclick="setFilter('delete', this)">حذف 🗑️</button>
        <button class="filterBtn" onclick="setFilter('newchat', this)">چت جدید 💬</button>
        <input type="text" class="consoleSearch" id="consoleSearch" placeholder="جستجو در لاگ‌ها..." oninput="filterLogs()">
      </div>
      <div class="consoleList" id="consoleList"><div class="loading"><div class="spinner"></div></div></div>
    </div>

    <!-- MESSENGER TAB -->
    <div class="tabContent" id="tab-messenger">
      <div class="messengerLayout">
        <div class="chatList">
          <div class="header">💬 تمام چت‌ها</div>
          <div id="adminChatList"><div class="loading"><div class="spinner"></div></div></div>
        </div>
        <div class="chatWindow">
          <div class="chatWindowEmpty" id="chatWindowEmpty">
            <div class="emptyIcon">💬</div>
            <div>یک چت انتخاب کنید</div>
          </div>
          <div id="chatWindowActive" style="display:none; flex-direction:column; flex:1; overflow:hidden; display:none;">
            <div class="chatHeader">
              <div class="chatHeaderInfo">
                <div class="chatAvatar" id="activeChatAvatar"></div>
                <div>
                  <div style="font-size:15px;font-weight:700" id="activeChatName"></div>
                  <div style="font-size:12px;color:var(--muted)" id="activeChatMeta"></div>
                </div>
              </div>
              <div class="chatHeaderActions">
                <button class="btnWarn" onclick="closeCurrentChat()">🔒 بستن چت</button>
              </div>
            </div>
            <div class="messagesArea" id="messagesArea"></div>
            <div class="adminSendBar">
              <select class="senderPicker" id="senderPicker"><option value="">ارسال از...</option></select>
              <textarea id="adminMsgInput" placeholder="پیام ادمین... (Shift+Enter برای خط جدید)" rows="1" onkeydown="handleSendKey(event)"></textarea>
              <button class="sendBtn" onclick="adminSendMessage()">➤</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ACCOUNTS TAB -->
    <div class="tabContent" id="tab-accounts">
      <div class="accountsBar">
        <input type="text" id="accountSearch" placeholder="جستجوی کاربر..." oninput="filterAccounts()">
      </div>
      <div class="accountsGrid" id="accountsGrid"><div class="loading"><div class="spinner"></div></div></div>
    </div>
  </div>
</div>

<!-- MODAL -->
<div class="modal" id="confirmModal">
  <div class="modalBox">
    <h3 id="modalTitle">تأیید عملیات</h3>
    <p id="modalBody"></p>
    <div class="modalActions">
      <button class="btnCancel" onclick="closeModal()">انصراف</button>
      <button class="btnConfirmDanger" id="modalConfirm">تأیید</button>
    </div>
  </div>
</div>

<div id="toast"></div>

<script>
let adminToken = null;
let allLogs = [];
let allUsers = [];
let allChats = [];
let currentFilter = 'all';
let currentChatId = null;

// ===== AUTH (سرور-ساید - هیچ اطلاعاتی در کد کلاینت نیست) =====
async function adminLogin() {
  const u = document.getElementById('adminUser').value.trim();
  const p = document.getElementById('adminPass').value;
  const errEl = document.getElementById('loginError');
  errEl.style.display = 'none';
  if (!u || !p) { errEl.style.display = 'block'; return; }
  try {
    const r = await fetch('/.hidden/core/api/auth', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: u, password: p })
    });
    const data = await r.json();
    if (data.token) {
      adminToken = data.token;
      document.getElementById('loginScreen').style.display = 'none';
      document.getElementById('adminPanel').style.display = 'flex';
      loadAll();
    } else {
      errEl.style.display = 'block';
    }
  } catch(e) {
    errEl.style.display = 'block';
  }
}

function adminLogout() {
  adminToken = null;
  document.getElementById('loginScreen').style.display = 'flex';
  document.getElementById('adminPanel').style.display = 'none';
}

document.getElementById('adminPass').addEventListener('keydown', e => { if (e.key === 'Enter') adminLogin(); });

// ===== API =====
async function api(path, opts = {}) {
  const r = await fetch(path, {
    ...opts,
    headers: { 'Content-Type': 'application/json', 'X-Admin-Token': adminToken, ...(opts.headers||{}) }
  });
  return r.json();
}

// ===== LOAD ALL =====
async function loadAll() {
  await Promise.all([loadLogs(), loadUsers(), loadChats()]);
}

// ===== CONSOLE =====
async function loadLogs() {
  const data = await api('/.hidden/core/api/logs');
  allLogs = data.logs || [];
  renderLogs();
  renderStats();
}

function renderStats() {
  const logins = allLogs.filter(l => l.type === 'login').length;
  const regs = allLogs.filter(l => l.type === 'register').length;
  document.getElementById('topStats').innerHTML = \`
    <div class="statBadge">لاگین‌ها<span>\${logins}</span></div>
    <div class="statBadge">ثبت‌نام‌ها<span>\${regs}</span></div>
    <div class="statBadge">کاربران<span>\${allUsers.length}</span></div>
    <div class="statBadge">چت‌ها<span>\${allChats.length}</span></div>
  \`;
}

function setFilter(f, el) {
  currentFilter = f;
  document.querySelectorAll('.filterBtn').forEach(b => b.classList.remove('active'));
  el.classList.add('active');
  filterLogs();
}

function filterLogs() {
  const q = document.getElementById('consoleSearch').value.toLowerCase();
  const filtered = allLogs.filter(l => {
    const matchType = currentFilter === 'all' || l.type === currentFilter;
    const matchQ = !q || JSON.stringify(l).toLowerCase().includes(q);
    return matchType && matchQ;
  });
  renderLogs(filtered);
}

function renderLogs(logs = allLogs) {
  const container = document.getElementById('consoleList');
  if (!logs.length) { container.innerHTML = '<div class="emptyState"><div class="icon">📭</div>لاگی یافت نشد</div>'; return; }
  const icons = { login:'🔑', register:'🆕', edit:'✏️', delete:'🗑️', newchat:'💬' };
  const labels = { login:'لاگین', register:'ثبت‌نام', edit:'ویرایش پیام', delete:'حذف پیام', newchat:'چت جدید' };
  container.innerHTML = logs.slice().reverse().map(l => \`
    <div class="logEntry">
      <div class="logIcon">\${icons[l.type]||'📌'}</div>
      <div class="logBody">
        <div class="logTitle">
          <span class="tag \${l.type}">\${labels[l.type]||l.type}</span>
          \${l.username || ''} \${l.details || ''}
        </div>
        <div class="logMeta">\${l.extra || ''}</div>
      </div>
      <div class="logTime">\${formatTime(l.time)}</div>
    </div>
  \`).join('');
}

function formatTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return d.toLocaleDateString('fa-IR') + ' ' + d.toLocaleTimeString('fa-IR', {hour:'2-digit', minute:'2-digit'});
}

// ===== MESSENGER =====
async function loadChats() {
  const data = await api('/.hidden/core/api/chats');
  allChats = data.chats || [];
  renderChatList();
}

function renderChatList() {
  const container = document.getElementById('adminChatList');
  if (!allChats.length) { container.innerHTML = '<div class="emptyState"><div class="icon">💬</div>چتی یافت نشد</div>'; return; }
  container.innerHTML = allChats.map(c => {
    const name = c.title || c.participants?.map(p => p.display_name || p.username).join('، ') || 'چت #' + c.id;
    const initial = name.charAt(0).toUpperCase();
    const isOnline = c.participants?.some(p => p.status === 'online');
    return \`
      <div class="chatListItem \${currentChatId == c.id ? 'active' : ''}" onclick="openChat(\${c.id})">
        <div class="chatAvatar">\${initial}<span class="dot \${isOnline ? 'online' : ''}"></span></div>
        <div class="chatInfo">
          <div class="name">\${name}</div>
          <div class="preview">\${c.last_message || 'بدون پیام'}</div>
        </div>
      </div>
    \`;
  }).join('');
}

async function openChat(chatId) {
  currentChatId = chatId;
  renderChatList();
  const chat = allChats.find(c => c.id == chatId);
  const name = chat?.title || chat?.participants?.map(p => p.display_name || p.username).join('، ') || 'چت #' + chatId;
  const initial = name.charAt(0).toUpperCase();

  document.getElementById('activeChatAvatar').textContent = initial;
  document.getElementById('activeChatName').textContent = name;
  document.getElementById('activeChatMeta').textContent = 'Chat ID: ' + chatId + ' | ' + (chat?.chat_type === 'group' ? 'گروه' : 'خصوصی');

  // Populate sender picker
  const picker = document.getElementById('senderPicker');
  picker.innerHTML = '<option value="">ارسال از...</option>' + allUsers.map(u =>
    \`<option value="\${u.id}">\${u.display_name || u.username} (@\${u.username})</option>\`
  ).join('') + '<option value="fake">👻 ID فیک دلخواه</option>';

  document.getElementById('chatWindowEmpty').style.display = 'none';
  const win = document.getElementById('chatWindowActive');
  win.style.display = 'flex';
  win.style.flexDirection = 'column';
  win.style.flex = '1';
  win.style.overflow = 'hidden';

  await loadMessages(chatId);
}

async function loadMessages(chatId) {
  const area = document.getElementById('messagesArea');
  area.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  const data = await api(\`/.hidden/core/api/messages?chat_id=\${chatId}\`);
  const msgs = data.messages || [];
  if (!msgs.length) { area.innerHTML = '<div class="emptyState"><div class="icon">💬</div>پیامی وجود ندارد</div>'; return; }
  area.innerHTML = msgs.map(m => {
    const sender = allUsers.find(u => u.id == m.sender_id);
    const senderName = sender ? (sender.display_name || sender.username) : 'کاربر #' + m.sender_id;
    const isEdited = m.is_edited ? '<span class="editedTag"> (ویرایش شده)</span>' : '';
    return \`
      <div class="msgWrapper theirs">
        <div class="senderLabel">\${senderName} · \${formatTime(m.created_at)}</div>
        <div class="msgBubble theirs" id="msg-\${m.id}">
          <div class="msgActions">
            <button class="msgActionBtn" onclick="adminEditMsg(\${m.id}, '\${escHtml(m.content)}')">✏️ ویرایش</button>
            <button class="msgActionBtn del" onclick="adminDeleteMsg(\${m.id})">🗑️ حذف</button>
          </div>
          \${escHtml(m.content)}\${isEdited}
          <div class="msgMeta">
            <span>ID:\${m.id}</span>
            <span>\${m.network_mode}</span>
          </div>
        </div>
      </div>
    \`;
  }).join('');
  area.scrollTop = area.scrollHeight;
}

function escHtml(s) {
  if (!s) return '';
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

async function adminSendMessage() {
  const chatId = currentChatId;
  const content = document.getElementById('adminMsgInput').value.trim();
  const senderVal = document.getElementById('senderPicker').value;
  if (!content) { showToast('متن پیام را وارد کنید', 'error'); return; }
  if (!senderVal) { showToast('فرستنده را انتخاب کنید', 'error'); return; }

  let senderId = senderVal;
  if (senderVal === 'fake') {
    senderId = prompt('ID دلخواه را وارد کنید (حتی اگر وجود نداشته باشد):');
    if (!senderId) return;
  }

  const r = await api('/.hidden/core/api/admin/send', {
    method: 'POST',
    body: JSON.stringify({ chat_id: chatId, sender_id: parseInt(senderId), content })
  });

  if (r.success) {
    document.getElementById('adminMsgInput').value = '';
    showToast('پیام ارسال شد ✓', 'success');
    await loadMessages(chatId);
  } else {
    showToast(r.error || 'خطا', 'error');
  }
}

function handleSendKey(e) {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); adminSendMessage(); }
}

function adminEditMsg(msgId, oldContent) {
  const newContent = prompt('ویرایش پیام:', oldContent);
  if (newContent === null) return;
  api('/.hidden/core/api/admin/edit', {
    method: 'POST',
    body: JSON.stringify({ message_id: msgId, content: newContent })
  }).then(r => {
    if (r.success) { showToast('پیام ویرایش شد ✓', 'success'); loadMessages(currentChatId); }
    else showToast(r.error || 'خطا', 'error');
  });
}

function adminDeleteMsg(msgId) {
  showConfirm('حذف پیام', 'آیا از حذف این پیام اطمینان دارید؟ این عملیات قابل برگشت نیست.', () => {
    api('/.hidden/core/api/admin/delete-message', {
      method: 'POST',
      body: JSON.stringify({ message_id: msgId })
    }).then(r => {
      if (r.success) { showToast('پیام حذف شد', 'success'); loadMessages(currentChatId); }
      else showToast(r.error || 'خطا', 'error');
    });
  });
}

function closeCurrentChat() {
  if (!currentChatId) return;
  showConfirm('بستن چت', 'آیا می‌خواهید تمام پیام‌های این چت پاک شوند؟', () => {
    api('/.hidden/core/api/admin/close-chat', {
      method: 'POST',
      body: JSON.stringify({ chat_id: currentChatId })
    }).then(r => {
      if (r.success) { showToast('چت بسته شد', 'success'); loadMessages(currentChatId); loadChats(); }
      else showToast(r.error || 'خطا', 'error');
    });
  });
}

// ===== ACCOUNTS =====
async function loadUsers() {
  const data = await api('/.hidden/core/api/users');
  allUsers = data.users || [];
  renderAccounts();
}

function filterAccounts() {
  const q = document.getElementById('accountSearch').value.toLowerCase();
  const filtered = allUsers.filter(u =>
    u.username.toLowerCase().includes(q) ||
    (u.display_name || '').toLowerCase().includes(q)
  );
  renderAccounts(filtered);
}

function renderAccounts(users = allUsers) {
  const grid = document.getElementById('accountsGrid');
  if (!users.length) { grid.innerHTML = '<div class="emptyState"><div class="icon">👥</div>کاربری یافت نشد</div>'; return; }
  grid.innerHTML = users.map(u => {
    const initial = (u.display_name || u.username).charAt(0).toUpperCase();
    const isOnline = u.status === 'online';
    return \`
      <div class="accountCard" id="card-\${u.id}">
        <div class="cardTop">
          <div class="avatar">
            \${initial}
            <span class="statusDot" style="background:\${isOnline ? 'var(--online)' : 'var(--offline)'}"></span>
          </div>
          <div class="userInfo">
            <div class="displayName">\${escHtml(u.display_name || u.username)}</div>
            <div class="username">@\${escHtml(u.username)}</div>
            <div class="userId">ID: \${u.id} · <span class="statusBadge \${isOnline ? 'online' : 'offline'}">\${isOnline ? 'آنلاین' : 'آفلاین'}</span></div>
          </div>
        </div>
        <div class="editFields">
          <input type="text" id="uname-\${u.id}" value="\${escHtml(u.username)}" placeholder="نام کاربری جدید">
          <input type="text" id="dname-\${u.id}" value="\${escHtml(u.display_name || '')}" placeholder="نام نمایشی جدید">
        </div>
        <div class="cardActions">
          <button class="btnPrimary" onclick="saveUser(\${u.id})">💾 ذخیره</button>
          <button class="btnWarn" style="background:rgba(252,180,92,0.15);color:var(--warn);border:1px solid rgba(252,180,92,0.3)" onclick="toggleStatus(\${u.id}, '\${u.status}')">
            \${isOnline ? '⬤ آفلاین کن' : '⬤ آنلاین کن'}
          </button>
        </div>
      </div>
    \`;
  }).join('');
}

async function saveUser(userId) {
  const username = document.getElementById('uname-' + userId).value.trim();
  const displayName = document.getElementById('dname-' + userId).value.trim();
  if (!username) { showToast('نام کاربری نمی‌تواند خالی باشد', 'error'); return; }
  const r = await api('/.hidden/core/api/admin/update-user', {
    method: 'POST',
    body: JSON.stringify({ user_id: userId, username, display_name: displayName })
  });
  if (r.success) { showToast('اطلاعات کاربر ذخیره شد ✓', 'success'); await loadUsers(); }
  else showToast(r.error || 'خطا', 'error');
}

async function toggleStatus(userId, currentStatus) {
  const newStatus = currentStatus === 'online' ? 'offline' : 'online';
  const r = await api('/.hidden/core/api/admin/set-status', {
    method: 'POST',
    body: JSON.stringify({ user_id: userId, status: newStatus })
  });
  if (r.success) { showToast('وضعیت تغییر کرد ✓', 'success'); await loadUsers(); }
  else showToast(r.error || 'خطا', 'error');
}

// ===== TABS =====
function switchTab(tab) {
  document.querySelectorAll('.tabContent').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.navItem').forEach(n => n.classList.remove('active'));
  document.getElementById('tab-' + tab).classList.add('active');
  document.getElementById('nav-' + tab).classList.add('active');
  const titles = { console: 'کنسول لاگ', messenger: 'مسنجر ادمین', accounts: 'مدیریت اکانت‌ها' };
  document.getElementById('pageTitle').textContent = titles[tab] || '';
}

// ===== MODAL =====
let modalCallback = null;
function showConfirm(title, body, cb) {
  document.getElementById('modalTitle').textContent = title;
  document.getElementById('modalBody').textContent = body;
  document.getElementById('confirmModal').classList.add('open');
  modalCallback = cb;
  document.getElementById('modalConfirm').onclick = () => { closeModal(); cb(); };
}
function closeModal() { document.getElementById('confirmModal').classList.remove('open'); }

// ===== TOAST =====
let toastTimer;
function showToast(msg, type = 'success') {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'show ' + type;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.className = '', 3000);
}
</script>
</body>
</html>`;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;

    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Admin-Token",
    };

    if (method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    // =================================================================
    // admin
    // =================================================================

    if (path === "/.hidden/core" || path === "/.hidden/core/") {
      const html = getAdminPanelHTML();
      return new Response(html, {
        headers: { "Content-Type": "text/html; charset=utf-8" }
      });
    }

    if (path === "/.hidden/core/api/auth" && method === "POST") {
      try {
        const body = await request.json();
        const { username, password } = body;
        if (username === ADMIN_USERNAME && password === ADMIN_PASSWORD) {
          const key = await crypto.subtle.importKey(
            "raw", new TextEncoder().encode(ADMIN_PASSWORD + ADMIN_USERNAME),
            { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
          );
          const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(ADMIN_USERNAME));
          const token = btoa(String.fromCharCode(...new Uint8Array(sig)));
          return new Response(JSON.stringify({ token }), { headers: corsHeaders });
        } else {
          return new Response(JSON.stringify({ error: "نام کاربری یا رمز عبور اشتباه است" }), { status: 401, headers: corsHeaders });
        }
      } catch (e) {
        return new Response(JSON.stringify({ error: "خطا در احراز هویت" }), { status: 400, headers: corsHeaders });
      }
    }

    if (path.startsWith("/.hidden/core/api/")) {
      const adminToken = request.headers.get("X-Admin-Token");
      const key = await crypto.subtle.importKey(
        "raw", new TextEncoder().encode(ADMIN_PASSWORD + ADMIN_USERNAME),
        { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
      );
      const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(ADMIN_USERNAME));
      const expectedToken = btoa(String.fromCharCode(...new Uint8Array(sig)));

      if (!adminToken || adminToken !== expectedToken) {
        return new Response(JSON.stringify({ error: "دسترسی غیرمجاز" }), { status: 403, headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/logs" && method === "GET") {
        const sessions = await env.DB.prepare(
          `SELECT us.created_at as time, u.username, 'login' as type, us.device_info as extra
           FROM user_sessions us JOIN users u ON us.user_id = u.id
           ORDER BY us.created_at DESC LIMIT 100`
        ).all();

        const registers = await env.DB.prepare(
          `SELECT created_at as time, username, 'register' as type, display_name as extra
           FROM users ORDER BY created_at DESC LIMIT 50`
        ).all();

        const edits = await env.DB.prepare(
          `SELECT mh.created_at as time, u.username, 'edit' as type,
           'پیام #' || mh.message_id || ' → ' || SUBSTR(mh.old_content, 1, 60) as details,
           'در چت ' || m.chat_id as extra
           FROM message_history mh
           JOIN users u ON mh.user_id = u.id
           LEFT JOIN messages m ON mh.message_id = m.id
           WHERE mh.action = 'edit'
           ORDER BY mh.created_at DESC LIMIT 50`
        ).all();

        const deletes = await env.DB.prepare(
          `SELECT mh.created_at as time, u.username, 'delete' as type,
           'پیام #' || mh.message_id as details,
           SUBSTR(mh.old_content, 1, 80) as extra
           FROM message_history mh
           JOIN users u ON mh.user_id = u.id
           WHERE mh.action = 'delete_everyone'
           ORDER BY mh.created_at DESC LIMIT 50`
        ).all();

        const newchats = await env.DB.prepare(
          `SELECT c.created_at as time, 'سیستم' as username, 'newchat' as type,
           'چت #' || c.id || ' (' || c.chat_type || ')' as details,
           GROUP_CONCAT(u.username, ' و ') as extra
           FROM chats c
           JOIN chat_participants cp ON c.id = cp.chat_id
           JOIN users u ON cp.user_id = u.id
           GROUP BY c.id
           ORDER BY c.created_at DESC LIMIT 50`
        ).all();

        const allLogs = [
          ...sessions.results.map(r => ({ ...r, type: 'login' })),
          ...registers.results.map(r => ({ ...r, type: 'register' })),
          ...edits.results.map(r => ({ ...r, type: 'edit' })),
          ...deletes.results.map(r => ({ ...r, type: 'delete' })),
          ...newchats.results.map(r => ({ ...r, type: 'newchat' })),
        ].sort((a, b) => new Date(b.time) - new Date(a.time));

        return new Response(JSON.stringify({ logs: allLogs }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/users" && method === "GET") {
        const users = await env.DB.prepare(
          "SELECT id, username, display_name, avatar_url, status, last_seen, created_at FROM users ORDER BY id"
        ).all();
        return new Response(JSON.stringify({ users: users.results }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/chats" && method === "GET") {
        const chats = await env.DB.prepare(
          `SELECT c.id, c.chat_type, c.title, c.created_at,
           (SELECT content FROM messages WHERE chat_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message
           FROM chats c ORDER BY c.created_at DESC`
        ).all();

        const result = [];
        for (const chat of chats.results) {
          const parts = await env.DB.prepare(
            `SELECT u.id, u.username, u.display_name, u.status
             FROM chat_participants cp JOIN users u ON cp.user_id = u.id
             WHERE cp.chat_id = ?`
          ).bind(chat.id).all();
          result.push({ ...chat, participants: parts.results });
        }

        return new Response(JSON.stringify({ chats: result }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/messages" && method === "GET") {
        const chatId = url.searchParams.get("chat_id");
        if (!chatId) return new Response(JSON.stringify({ error: "chat_id الزامی است" }), { status: 400, headers: corsHeaders });

        const messages = await env.DB.prepare(
          `SELECT m.*, u.username as sender_username
           FROM messages m JOIN users u ON m.sender_id = u.id
           WHERE m.chat_id = ? ORDER BY m.created_at ASC`
        ).bind(parseInt(chatId)).all();

        return new Response(JSON.stringify({ messages: messages.results }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/admin/send" && method === "POST") {
        const { chat_id, sender_id, content } = await request.json();
        if (!chat_id || !content) return new Response(JSON.stringify({ error: "اطلاعات ناقص" }), { status: 400, headers: corsHeaders });

        const safeSenderId = parseInt(sender_id) || 0;
        const safeContent = String(content).substring(0, 4000);

        const result = await env.DB.prepare(
          "INSERT INTO messages (chat_id, sender_id, network_mode, message_type, content) VALUES (?, ?, 'normal', 'text', ?)"
        ).bind(parseInt(chat_id), safeSenderId, safeContent).run();

        return new Response(JSON.stringify({ success: true, message_id: result.meta.last_row_id }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/admin/edit" && method === "POST") {
        const { message_id, content } = await request.json();
        if (!message_id || content === undefined) return new Response(JSON.stringify({ error: "اطلاعات ناقص" }), { status: 400, headers: corsHeaders });

        const msg = await env.DB.prepare("SELECT * FROM messages WHERE id = ?").bind(parseInt(message_id)).first();
        if (!msg) return new Response(JSON.stringify({ error: "پیام یافت نشد" }), { status: 404, headers: corsHeaders });

        await env.DB.prepare(
          "INSERT INTO message_history (message_id, user_id, action, old_content) VALUES (?, 0, 'edit', ?)"
        ).bind(msg.id, msg.content).run();

        await env.DB.prepare("UPDATE messages SET content = ?, is_edited = 1 WHERE id = ?")
          .bind(String(content).substring(0, 4000), msg.id).run();

        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/admin/delete-message" && method === "POST") {
        const { message_id } = await request.json();
        if (!message_id) return new Response(JSON.stringify({ error: "message_id الزامی است" }), { status: 400, headers: corsHeaders });

        const msg = await env.DB.prepare("SELECT * FROM messages WHERE id = ?").bind(parseInt(message_id)).first();
        if (!msg) return new Response(JSON.stringify({ error: "پیام یافت نشد" }), { status: 404, headers: corsHeaders });

        await env.DB.prepare(
          "INSERT INTO message_history (message_id, user_id, action, old_content) VALUES (?, 0, 'delete_everyone', ?)"
        ).bind(msg.id, msg.content).run();

        await env.DB.prepare("DELETE FROM messages WHERE id = ?").bind(msg.id).run();
        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/admin/close-chat" && method === "POST") {
        const { chat_id } = await request.json();
        if (!chat_id) return new Response(JSON.stringify({ error: "chat_id الزامی است" }), { status: 400, headers: corsHeaders });

        const msgs = await env.DB.prepare("SELECT id, content FROM messages WHERE chat_id = ?").bind(parseInt(chat_id)).all();
        for (const m of msgs.results) {
          await env.DB.prepare(
            "INSERT INTO message_history (message_id, user_id, action, old_content) VALUES (?, 0, 'delete_everyone', ?)"
          ).bind(m.id, m.content).run();
        }

        await env.DB.prepare("DELETE FROM messages WHERE chat_id = ?").bind(parseInt(chat_id)).run();
        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/admin/update-user" && method === "POST") {
        const { user_id, username, display_name } = await request.json();
        if (!user_id || !username) return new Response(JSON.stringify({ error: "اطلاعات ناقص" }), { status: 400, headers: corsHeaders });

        const cleanUsername = String(username).trim().toLowerCase().replace(/[^a-z0-9_]/g, "");
        if (cleanUsername.length < 3) return new Response(JSON.stringify({ error: "نام کاربری نامعتبر" }), { status: 400, headers: corsHeaders });

        const existing = await env.DB.prepare("SELECT id FROM users WHERE username = ? AND id != ?").bind(cleanUsername, parseInt(user_id)).first();
        if (existing) return new Response(JSON.stringify({ error: "این نام کاربری قبلاً استفاده شده" }), { status: 400, headers: corsHeaders });

        await env.DB.prepare("UPDATE users SET username = ?, display_name = ? WHERE id = ?")
          .bind(cleanUsername, String(display_name || cleanUsername).substring(0, 50), parseInt(user_id)).run();

        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
      }

      if (path === "/.hidden/core/api/admin/set-status" && method === "POST") {
        const { user_id, status } = await request.json();
        if (!user_id || !status) return new Response(JSON.stringify({ error: "اطلاعات ناقص" }), { status: 400, headers: corsHeaders });

        const validStatus = status === "online" ? "online" : "offline";
        await env.DB.prepare("UPDATE users SET status = ?, last_seen = CURRENT_TIMESTAMP WHERE id = ?")
          .bind(validStatus, parseInt(user_id)).run();

        return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
      }

      return new Response(JSON.stringify({ error: "اندپوینت ادمین یافت نشد" }), { status: 404, headers: corsHeaders });
    }

    // =================================================================
    // api
    // =================================================================
    try {
      if (path === "/api/register" && method === "POST") {
        const { username, password, display_name } = await request.json();
        if (!username || !password) return new Response(JSON.stringify({ error: "نام کاربری و رمز عبور الزامی است." }), { status: 400, headers: corsHeaders });

        const cleanUsername = username.trim().toLowerCase().replace(/[^a-z0-9_]/g, "");
        if (cleanUsername.length < 3) return new Response(JSON.stringify({ error: "نام کاربری نامعتبر یا بیش از حد کوتاه است." }), { status: 400, headers: corsHeaders });

        const existingUser = await env.DB.prepare("SELECT id FROM users WHERE username = ?").bind(cleanUsername).first();
        if (existingUser) return new Response(JSON.stringify({ error: "این نام کاربری قبلاً انتخاب شده است." }), { status: 400, headers: corsHeaders });

        const msgUint8 = new TextEncoder().encode(password);
        const hashBuffer = await crypto.subtle.digest("SHA-256", msgUint8);
        const passwordHash = Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, "0")).join("");

        const safeDisplayName = display_name ? display_name.replace(/<[^>]*>/g, "").substring(0, 50).trim() : cleanUsername;
        const insertResult = await env.DB.prepare(
          "INSERT INTO users (username, password_hash, display_name, status, last_seen) VALUES (?, ?, ?, 'online', CURRENT_TIMESTAMP)"
        ).bind(cleanUsername, passwordHash, safeDisplayName).run();

        const newUserId = insertResult.meta.last_row_id;
        const regToken = crypto.randomUUID().replace(/-/g, "") + crypto.randomUUID().replace(/-/g, "");
        const regExpiresAt = new Date();
        regExpiresAt.setDate(regExpiresAt.getDate() + 30);

        await env.DB.prepare("INSERT INTO user_sessions (user_id, token, expires_at) VALUES (?, ?, ?)")
          .bind(newUserId, regToken, regExpiresAt.toISOString()).run();

        return new Response(JSON.stringify({
          success: true, message: "ثبت‌نام با موفقیت انجام شد.", token: regToken,
          username: cleanUsername, display_name: safeDisplayName,
          user: { id: newUserId, username: cleanUsername, display_name: safeDisplayName }
        }), { status: 201, headers: corsHeaders });
      }

      if (path === "/api/login" && method === "POST") {
        const { username, password } = await request.json();
        if (!username || !password) return new Response(JSON.stringify({ error: "اطلاعات ورود ناقص است." }), { status: 400, headers: corsHeaders });

        const cleanUsername = username.trim().toLowerCase();
        const msgUint8 = new TextEncoder().encode(password);
        const hashBuffer = await crypto.subtle.digest("SHA-256", msgUint8);
        const passwordHash = Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, "0")).join("");

        const user = await env.DB.prepare("SELECT * FROM users WHERE username = ? AND password_hash = ?")
          .bind(cleanUsername, passwordHash).first();
        if (!user) return new Response(JSON.stringify({ error: "نام کاربری یا رمز عبور اشتباه است." }), { status: 401, headers: corsHeaders });

        const token = crypto.randomUUID().replace(/-/g, "") + crypto.randomUUID().replace(/-/g, "");
        const expiresAt = new Date();
        expiresAt.setDate(expiresAt.getDate() + 30);

        await env.DB.prepare("INSERT INTO user_sessions (user_id, token, expires_at) VALUES (?, ?, ?)")
          .bind(user.id, token, expiresAt.toISOString()).run();
        await env.DB.prepare("UPDATE users SET status = 'online', last_seen = CURRENT_TIMESTAMP WHERE id = ?").bind(user.id).run();

        return new Response(JSON.stringify({
          success: true, token, username: user.username, display_name: user.display_name,
          user: { id: user.id, username: user.username, display_name: user.display_name }
        }), { headers: corsHeaders });
      }

      const authHeader = request.headers.get("Authorization");
      if (!authHeader || !authHeader.startsWith("Bearer ")) {
        return new Response(JSON.stringify({ error: "توکن احراز هویت یافت نشد." }), { status: 401, headers: corsHeaders });
      }
      const token = authHeader.split(" ")[1];
      const session = await env.DB.prepare(
        "SELECT user_id FROM user_sessions WHERE token = ? AND expires_at > DATETIME('now')"
      ).bind(token).first();
      if (!session) return new Response(JSON.stringify({ error: "توکن نامعتبر یا منقضی شده است." }), { status: 401, headers: corsHeaders });

      const currentUserId = session.user_id;

      if (path === "/api/users/heartbeat" && method === "POST") {
        const { status } = await request.json().catch(() => ({ status: "online" }));
        const targetStatus = (status === "offline") ? "offline" : "online";
        await env.DB.prepare("UPDATE users SET status = ?, last_seen = CURRENT_TIMESTAMP WHERE id = ?")
          .bind(targetStatus, currentUserId).run();
        return new Response(JSON.stringify({ success: true, status: targetStatus }), { headers: corsHeaders });
      }

      if (path === "/api/messages/send" && method === "POST") {
        const contentType = request.headers.get("content-type") || "";

        if (contentType.includes("application/json")) {
          const { chat_id, message_type, content, network_mode, reply_to_id } = await request.json();
          if (!chat_id || content === undefined) return new Response(JSON.stringify({ error: "اطلاعات ارسالی ناقص است." }), { status: 400, headers: corsHeaders });

          const isParticipant = await env.DB.prepare("SELECT 1 FROM chat_participants WHERE chat_id = ? AND user_id = ?")
            .bind(parseInt(chat_id), currentUserId).first();
          if (!isParticipant) return new Response(JSON.stringify({ error: "دسترسی غیرمجاز! شما عضو این گفتگو نیستید." }), { status: 403, headers: corsHeaders });

          const safeContent = content.replace(/<[^>]*>/g, "").substring(0, 4000);
          const mode = network_mode || "normal";
          const finalMsgType = message_type || "text";
          const safeReplyToId = reply_to_id ? parseInt(reply_to_id) : null;

          const result = await env.DB.prepare(
            "INSERT INTO messages (chat_id, sender_id, network_mode, message_type, content, reply_to_id) VALUES (?, ?, ?, ?, ?, ?)"
          ).bind(parseInt(chat_id), currentUserId, mode, finalMsgType, safeContent, safeReplyToId).run();

          const senderRow = await env.DB.prepare("SELECT username FROM users WHERE id = ?").bind(currentUserId).first();
          const insertedRow = await env.DB.prepare("SELECT created_at FROM messages WHERE id = ?").bind(result.meta.last_row_id).first();

          return new Response(JSON.stringify({
            success: true, messageId: result.meta.last_row_id,
            message: {
              id: result.meta.last_row_id.toString(), chat_id: chat_id.toString(),
              sender_username: senderRow ? senderRow.username : "", content: safeContent,
              message_type: finalMsgType,
              timestamp: insertedRow ? new Date(insertedRow.created_at + "Z").getTime() : Date.now(),
              network_mode: mode, reply_to_id: safeReplyToId ? safeReplyToId.toString() : null, is_edited: false
            }
          }), { headers: corsHeaders });
        }

        else if (contentType.includes("multipart/form-data")) {
          const formData = await request.formData();
          const chatId = formData.get("chat_id");
          const msgType = formData.get("message_type");
          const networkMode = formData.get("network_mode") || "normal";
          const file = formData.get("file");
          const replyToId = formData.get("reply_to_id");

          if (!chatId || !file || !msgType) return new Response(JSON.stringify({ error: "اطلاعات فرم ناقص است." }), { status: 400, headers: corsHeaders });

          const isParticipant = await env.DB.prepare("SELECT 1 FROM chat_participants WHERE chat_id = ? AND user_id = ?")
            .bind(parseInt(chatId), currentUserId).first();
          if (!isParticipant) return new Response(JSON.stringify({ error: "دسترسی غیرمجاز!" }), { status: 403, headers: corsHeaders });

          const fileExt = file.name.split('.').pop().replace(/[^a-zA-Z0-9]/g, "");
          const b2FileName = `${crypto.randomUUID()}.${fileExt}`;
          const fileBuffer = await file.arrayBuffer();

          const signer = new S3Signer({
            accessKeyId: env.B2_ACCESS_KEY_ID, secretAccessKey: env.B2_SECRET_ACCESS_KEY,
            endpoint: env.B2_ENDPOINT, region: "us-east-1"
          });

          const { url: b2PutUrl, headers: b2Headers } = await signer.signPutRequest(
            env.B2_BUCKET_NAME, b2FileName, fileBuffer, file.type || "application/octet-stream"
          );

          const b2Response = await fetch(b2PutUrl, { method: "PUT", headers: b2Headers, body: fileBuffer });
          if (!b2Response.ok) return new Response(JSON.stringify({ error: "خطای سرور ذخیره‌سازی فایل" }), { status: 500, headers: corsHeaders });

          const fileDownloadUrl = `https://${env.B2_ENDPOINT}/${env.B2_BUCKET_NAME}/${b2FileName}`;
          const safeReplyToId = replyToId ? parseInt(replyToId) : null;
          const result = await env.DB.prepare(
            "INSERT INTO messages (chat_id, sender_id, network_mode, message_type, content, file_name, file_size, reply_to_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
          ).bind(parseInt(chatId), currentUserId, networkMode, msgType, fileDownloadUrl, file.name.replace(/<[^>]*>/g, ""), file.size, safeReplyToId).run();

          return new Response(JSON.stringify({ success: true, messageId: result.meta.last_row_id, fileUrl: fileDownloadUrl }), { headers: corsHeaders });
        }
      }

      if (path === "/api/messages/edit" && method === "POST") {
        const { message_id, content } = await request.json();
        if (!message_id || content === undefined) return new Response(JSON.stringify({ error: "اطلاعات ارسالی ناقص است." }), { status: 400, headers: corsHeaders });

        const targetMessage = await env.DB.prepare("SELECT * FROM messages WHERE id = ?").bind(parseInt(message_id)).first();
        if (!targetMessage) return new Response(JSON.stringify({ error: "پیام مورد نظر یافت نشد." }), { status: 404, headers: corsHeaders });
        if (targetMessage.sender_id !== currentUserId) return new Response(JSON.stringify({ error: "شما مجاز به ویرایش این پیام نیستید!" }), { status: 403, headers: corsHeaders });
        if (targetMessage.message_type !== "text") return new Response(JSON.stringify({ error: "فقط پیام‌های متنی قابل ویرایش هستند." }), { status: 400, headers: corsHeaders });

        await env.DB.prepare("INSERT INTO message_history (message_id, user_id, action, old_content) VALUES (?, ?, 'edit', ?)")
          .bind(targetMessage.id, currentUserId, targetMessage.content).run();

        const safeContent = content.replace(/<[^>]*>/g, "").substring(0, 4000);
        await env.DB.prepare("UPDATE messages SET content = ?, is_edited = 1 WHERE id = ?").bind(safeContent, targetMessage.id).run();

        return new Response(JSON.stringify({ success: true, message: "پیام با موفقیت ویرایش شد." }), { headers: corsHeaders });
      }

      if (path === "/api/messages/delete" && method === "POST") {
        const { message_id, delete_type } = await request.json();
        if (!message_id || !delete_type) return new Response(JSON.stringify({ error: "اطلاعات ارسالی ناقص است." }), { status: 400, headers: corsHeaders });

        const targetMessage = await env.DB.prepare("SELECT * FROM messages WHERE id = ?").bind(parseInt(message_id)).first();
        if (!targetMessage) return new Response(JSON.stringify({ error: "پیام مورد نظر یافت نشد یا قبلا حذف شده است." }), { status: 404, headers: corsHeaders });

        if (delete_type === "for_me") {
          await env.DB.prepare("INSERT OR IGNORE INTO message_deletions (user_id, message_id) VALUES (?, ?)")
            .bind(currentUserId, targetMessage.id).run();
          return new Response(JSON.stringify({ success: true, message: "پیام برای شما پنهان شد." }), { headers: corsHeaders });
        } else if (delete_type === "for_everyone") {
          if (targetMessage.sender_id !== currentUserId) return new Response(JSON.stringify({ error: "شما مجاز به حذف این پیام برای همگان نیستید!" }), { status: 403, headers: corsHeaders });

          await env.DB.prepare("INSERT INTO message_history (message_id, user_id, action, old_content) VALUES (?, ?, 'delete_everyone', ?)")
            .bind(targetMessage.id, currentUserId, targetMessage.content).run();
          await env.DB.prepare("DELETE FROM messages WHERE id = ?").bind(targetMessage.id).run();
          return new Response(JSON.stringify({ success: true, message: "پیام برای همگان حذف شد." }), { headers: corsHeaders });
        }
        return new Response(JSON.stringify({ error: "نوع حذف ارسالی معتبر نیست." }), { status: 400, headers: corsHeaders });
      }

      if (path === "/api/messages/get" && method === "GET") {
        const chatId = url.searchParams.get("chat_id");
        const limit = Math.min(parseInt(url.searchParams.get("limit") || "50"), 100);

        if (!chatId) return new Response(JSON.stringify({ error: "chat_id الزامی است." }), { status: 400, headers: corsHeaders });

        const isParticipant = await env.DB.prepare("SELECT 1 FROM chat_participants WHERE chat_id = ? AND user_id = ?")
          .bind(parseInt(chatId), currentUserId).first();
        if (!isParticipant) return new Response(JSON.stringify({ error: "دسترسی غیرمجاز! شما مجاز به مشاهده پیام‌های این چت نیستید." }), { status: 403, headers: corsHeaders });

        const messages = await env.DB.prepare(
          `SELECT m.*, u.username as sender_username_db 
           FROM messages m JOIN users u ON m.sender_id = u.id 
           WHERE m.chat_id = ? AND m.id NOT IN (SELECT message_id FROM message_deletions WHERE user_id = ?)
           ORDER BY m.created_at DESC LIMIT ?`
        ).bind(parseInt(chatId), currentUserId, limit).all();

        const chatHistoryRaw = messages.results.reverse();
        const partnerPresence = await env.DB.prepare(
          `SELECT status, last_seen, (strftime('%s', 'now') - strftime('%s', last_seen)) as seconds_ago
           FROM users WHERE id IN (SELECT user_id FROM chat_participants WHERE chat_id = ? AND user_id != ?) LIMIT 1`
        ).bind(parseInt(chatId), currentUserId).first();

        let targetPresenceStatus = "offline";
        let finalLastSeen = partnerPresence ? partnerPresence.last_seen : "";
        if (partnerPresence && partnerPresence.status === "online" && partnerPresence.seconds_ago <= 90) {
          targetPresenceStatus = "online";
        }

        const chatHistory = chatHistoryRaw.map(row => {
          const tsMs = row.created_at ? new Date(row.created_at.replace(" ", "T") + "Z").getTime() : Date.now();
          const isMedia = ["photo", "video", "voice", "file"].includes(row.message_type);
          return {
            id: row.id.toString(), chat_id: row.chat_id.toString(),
            sender_username: row.sender_username_db || "", content: row.content,
            message_type: row.message_type, timestamp: tsMs,
            media_url: isMedia ? row.content : null, network_mode: row.network_mode,
            reply_to_id: row.reply_to_id ? row.reply_to_id.toString() : null, is_edited: row.is_edited === 1
          };
        });

        return new Response(JSON.stringify({ presence: { status: targetPresenceStatus, last_seen: finalLastSeen }, messages: chatHistory }), { headers: corsHeaders });
      }

      if (path === "/api/chats/create" && method === "POST") {
        const body = await request.json();
        const chat_type = body.chat_type || "private";
        const title = body.title ? body.title.replace(/<[^>]*>/g, "").substring(0, 100) : null;
        const target_user_id = body.target_user_id || body.targetId;

        if (chat_type !== "private" && chat_type !== "group") return new Response(JSON.stringify({ error: "نوع گفتگو معتبر نیست." }), { status: 400, headers: corsHeaders });

        if (chat_type === "private") {
          const targetIdParsed = parseInt(target_user_id);
          if (!target_user_id || isNaN(targetIdParsed) || targetIdParsed <= 0 || targetIdParsed === currentUserId) {
            return new Response(JSON.stringify({ error: "شناسه کاربر مخاطب معتبر نیست." }), { status: 400, headers: corsHeaders });
          }

          const existingChat = await env.DB.prepare(
            `SELECT c.id FROM chats c
             JOIN chat_participants cp1 ON c.id = cp1.chat_id AND cp1.user_id = ?
             JOIN chat_participants cp2 ON c.id = cp2.chat_id AND cp2.user_id = ?
             WHERE c.chat_type = 'private' LIMIT 1`
          ).bind(currentUserId, targetIdParsed).first();

          if (existingChat) return new Response(JSON.stringify({ success: true, chat_id: existingChat.id, existing: true }), { headers: corsHeaders });
        }

        const chatResult = await env.DB.prepare("INSERT INTO chats (chat_type, title) VALUES (?, ?)").bind(chat_type, title).run();
        const chatId = chatResult.meta.last_row_id;

        await env.DB.prepare("INSERT INTO chat_participants (chat_id, user_id) VALUES (?, ?)").bind(chatId, currentUserId).run();

        if (chat_type === "private") {
          await env.DB.prepare("INSERT INTO chat_participants (chat_id, user_id) VALUES (?, ?)").bind(chatId, parseInt(target_user_id)).run();
        }

        return new Response(JSON.stringify({ success: true, chat_id: chatId, existing: false }), { headers: corsHeaders });
      }

      if ((path === "/api/chats/list" || path === "/api/chats") && method === "GET") {
        const userChats = await env.DB.prepare(
          `SELECT c.id, c.chat_type, c.title, c.created_at,
            (SELECT content FROM messages WHERE chat_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message,
            (SELECT created_at FROM messages WHERE chat_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message_at,
            u.id as partner_id, u.display_name as partner_display_name, u.username as partner_username,
            u.status as partner_status_db,
            (strftime('%s', 'now') - strftime('%s', u.last_seen)) as partner_seconds_ago
           FROM chats c
           JOIN chat_participants cp ON c.id = cp.chat_id
           LEFT JOIN chat_participants cp2 ON c.id = cp2.chat_id AND cp2.user_id != ?
           LEFT JOIN users u ON cp2.user_id = u.id
           WHERE cp.user_id = ? AND last_message IS NOT NULL
           ORDER BY (SELECT created_at FROM messages WHERE chat_id = c.id ORDER BY created_at DESC LIMIT 1) DESC`
        ).bind(currentUserId, currentUserId).all();

        const formattedChats = userChats.results.map(row => {
          let currentStatus = "offline";
          if (row.chat_type === "private" && row.partner_status_db === "online" && row.partner_seconds_ago <= 90) currentStatus = "online";
          return {
            id: row.id.toString(),
            title: row.chat_type === "group" ? (row.title || `Group #${row.id}`) : (row.partner_display_name || row.partner_username || `Chat #${row.id}`),
            last_message: row.last_message, last_message_time: row.last_message_at,
            unread_count: 0, chat_type: row.chat_type, partner_status: currentStatus
          };
        });

        return new Response(JSON.stringify(formattedChats), { headers: corsHeaders });
      }

      if (path === "/api/users/search" && method === "GET") {
        const query = url.searchParams.get("query");
        if (!query) return new Response(JSON.stringify({ error: "عبارت جستجو الزامی است." }), { status: 400, headers: corsHeaders });

        const cleanQuery = query.replace(/[^a-zA-Z0-9_\u0600-\u06FF]/g, "").trim();
        if (cleanQuery.length < 2) return new Response(JSON.stringify({ success: true, users: [] }), { headers: corsHeaders });

        const searchedUsers = await env.DB.prepare(
          "SELECT id, username, display_name, avatar_url FROM users WHERE username LIKE ? AND id != ? LIMIT 10"
        ).bind(`%${cleanQuery}%`, currentUserId).all();

        return new Response(JSON.stringify({ success: true, users: searchedUsers.results }), { headers: corsHeaders });
      }

      return new Response(JSON.stringify({ error: "اندپوینت مورد نظر یافت نشد." }), { status: 404, headers: corsHeaders });

    } catch (error) {
      return new Response(JSON.stringify({ error: "خطای داخلی ورکر رازبان", message: error.message }), { status: 500, headers: corsHeaders });
    }
  },
};