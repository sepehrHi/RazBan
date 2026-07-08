/**
 * 🛰️ رازبان مسنجر (Razban Messenger) - لایه ریلی گوگل (Google Apps Script Relay)
 * وظیفه: دور زدن فیلترینگ و اختلالات شبکه در حالت‌های Timeout و Emergency
 * مجهز به روتینگ هوشمند متون، فایل‌های مولتی‌پارت (ویس واقعی)، ساخت چت، ادیت، حذف، ریپلای و ضربان قلب
 */

// 🛑 آدرس ورکر کلودفلر (بدون اسلش آخر)
const CLOUDFLARE_WORKER_URL = "YOUR_CLOUDFLARE_URL";

/**
 * مدیریت تمام درخواست‌های POST کلاینت (ارسال پیام متنی، ویس، عکس، ساخت چت، ادیت، حذف و ضربان قلب)
 */
function doPost(e) {
  try {
    let clientRequestData;
    let isMultipart = false;

    // تشخیص ساختار درخواست
    if (e.postData && e.postData.type.includes("application/json")) {
      clientRequestData = JSON.parse(e.postData.contents);
    } else {
      isMultipart = true;
    }

    // -------------------------------------------------------------
    // 🆕 لایه منفی‌یک: ورود اضطراری (Emergency Login/Register)
    // -------------------------------------------------------------
    if (!isMultipart && clientRequestData && clientRequestData.action === "login") {
      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/login`, {
        method: "post",
        contentType: "application/json",
        payload: JSON.stringify({
          username: clientRequestData.username,
          password: clientRequestData.password
        }),
        muteHttpExceptions: true
      });
      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    if (!isMultipart && clientRequestData && clientRequestData.action === "register") {
      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/register`, {
        method: "post",
        contentType: "application/json",
        payload: JSON.stringify({
          username: clientRequestData.username,
          password: clientRequestData.password,
          display_name: clientRequestData.display_name
        }),
        muteHttpExceptions: true
      });
      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    // -------------------------------------------------------------
    // 🆕 لایه صفر: ساخت چت جدید (Chat Creation) در حالت Timeout/Emergency
    // -------------------------------------------------------------
    if (!isMultipart && clientRequestData && clientRequestData.action === "chats_create") {
      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/chats/create`, {
        method: "post",
        contentType: "application/json",
        headers: { "Authorization": "Bearer " + clientRequestData.token },
        payload: JSON.stringify({
          chat_type: clientRequestData.chat_type || "private",
          target_user_id: clientRequestData.target_user_id
        }),
        muteHttpExceptions: true
      });
      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    // -------------------------------------------------------------
    // 🆕 لایه جدید: ویرایش پیام (Edit Message Action)
    // -------------------------------------------------------------
    if (!isMultipart && clientRequestData && clientRequestData.action === "messages_edit") {
      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/messages/edit`, {
        method: "post",
        contentType: "application/json",
        headers: { "Authorization": "Bearer " + clientRequestData.token },
        payload: JSON.stringify({
          message_id: clientRequestData.message_id,
          content: clientRequestData.content
        }),
        muteHttpExceptions: true
      });
      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    // -------------------------------------------------------------
    // 🆕 لایه جدید: حذف پیام (Delete Message Action - دو حالته)
    // -------------------------------------------------------------
    if (!isMultipart && clientRequestData && clientRequestData.action === "messages_delete") {
      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/messages/delete`, {
        method: "post",
        contentType: "application/json",
        headers: { "Authorization": "Bearer " + clientRequestData.token },
        payload: JSON.stringify({
          message_id: clientRequestData.message_id,
          delete_type: clientRequestData.delete_type // for_me یا for_everyone
        }),
        muteHttpExceptions: true
      });
      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    // -------------------------------------------------------------
    // 🆕 لایه جدید: ضربان قلب آنلاین/آفلاین (Heartbeat Action)
    // -------------------------------------------------------------
    if (!isMultipart && clientRequestData && clientRequestData.action === "heartbeat") {
      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/users/heartbeat`, {
        method: "post",
        contentType: "application/json",
        headers: { "Authorization": "Bearer " + clientRequestData.token },
        payload: JSON.stringify({
          status: clientRequestData.status || "online"
        }),
        muteHttpExceptions: true
      });
      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    // -------------------------------------------------------------
    // 🔴 لایه اول: مدیریت حالت اضطراری (Emergency Mode - فقط متن + پشتیبانی از ریپلای)
    // -------------------------------------------------------------
    if (!isMultipart && clientRequestData && clientRequestData.network_mode === "emergency") {

      // در حالت اضطراری، اگر نوع پیام چیزی جز متن (text) باشد، ریلی گوگل بلافاصله آن را بلاک می‌کند
      if (clientRequestData.message_type && clientRequestData.message_type !== "text") {
        return createJsonResponse({ error: "در حالت اضطراری ارسال هرگونه فایل، ویس یا ویدیو مسدود است." }, 400);
      }

      // ساخت پورت بدنه و پاس دادن فیلد اختیاری reply_to_id
      const payloadBody = {
        chat_id: clientRequestData.chat_id,
        message_type: "text",
        content: clientRequestData.content,
        network_mode: "emergency"
      };
      if (clientRequestData.reply_to_id) {
        payloadBody.reply_to_id = clientRequestData.reply_to_id;
      }

      // ارسال مستقیم داده متنی به ورکر کلودفلر
      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/messages/send`, {
        method: "post",
        contentType: "application/json",
        headers: { "Authorization": "Bearer " + clientRequestData.token },
        payload: JSON.stringify(payloadBody),
        muteHttpExceptions: true
      });

      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    // -------------------------------------------------------------
    // 🟡 لایه دوم: مدیریت حالت تایم‌اوت مولتی‌پارت (Timeout Mode - متن و فایل مانند ویس واقعی زیر ۲۰ مگابایت)
    // -------------------------------------------------------------
    if (isMultipart) {
      // دریافت فایل و پارامترها از فرم کلاینت
      const chatId = e.parameter.chat_id;
      const msgType = e.parameter.message_type; // voice, photo, file
      const token = e.parameter.token;
      const fileBlob = e.parameter.file; // این شامل دیتای باینری فایل است
      const replyToId = e.parameter.reply_to_id; // دریافت فیلد اختیاری ریپلای از کلاینت اندروید

      if (!chatId || !fileBlob || !token) {
        return createJsonResponse({ error: "اطلاعات ارسالی فرم ناقص است." }, 400);
      }

      // 📏 کنترل سخت‌گیرانه حجم فایل (سقف ۲۰ مگابایت برای پایداری در اختلال)
      const fileSizeInBytes = fileBlob.getBytes().length;
      const maxSizeBytes = 20 * 1024 * 1024; // 20 Megabytes

      if (fileSizeInBytes > maxSizeBytes) {
        return createJsonResponse({ error: "حجم فایل در حالت تایم‌اوت نباید بیشتر از ۲۰ مگابایت باشد." }, 400);
      }

      // بازسازی فرم دیتای مولتی‌پارت برای ارسال به ورکر کلودفلر
      const boundary = "----RazbanMessengerBoundary" + Utilities.getUuid();

      // ساخت فیلدهای متنی در فرم دیتا به همراه فیلد ریپلای در صورت وجود
      const fields = {
        "chat_id": chatId,
        "message_type": msgType,
        "network_mode": "timeout"
      };
      if (replyToId) {
        fields["reply_to_id"] = replyToId;
      }

      let multipartBody = "";
      for (let key in fields) {
        multipartBody += `--${boundary}\r\n`;
        multipartBody += `Content-Disposition: form-data; name="${key}"\r\n\r\n`;
        multipartBody += `${fields[key]}\r\n`;
      }

      // اضافه کردن فایل باینری فیزیکی (مانند ویس ضبط شده) به بدنه درخواست
      multipartBody += `--${boundary}\r\n`;
      multipartBody += `Content-Disposition: form-data; name="file"; filename="${fileBlob.getName()}"\r\n`;
      multipartBody += `Content-Type: ${fileBlob.getContentType()}\r\n\r\n`;

      const multipartBodyBlob = Utilities.newBlob(multipartBody);
      const endBoundaryBlob = Utilities.newBlob(`\r\n--${boundary}--\r\n`);

      // ترکیب نهایی باینری‌ها به صورت بایت-ارایه یکپارچه
      const finalPayload = []
        .concat(multipartBodyBlob.getBytes())
        .concat(fileBlob.getBytes())
        .concat(endBoundaryBlob.getBytes());

      // شلیک درخواست از سرور گوگل به ورکر کلودفلر جهت پردازش و آپلود به Backblaze B2
      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/messages/send`, {
        method: "post",
        contentType: "multipart/form-data; boundary=" + boundary,
        headers: { "Authorization": "Bearer " + token },
        payload: finalPayload,
        muteHttpExceptions: true
      });

      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    // اگر متد ارسالی برای پیام متنی ساده در حالت چت معمولی/تایم‌اوت با لایه JSON باشد:
    if (clientRequestData && clientRequestData.action === "messages_send" &&
        (clientRequestData.network_mode === "timeout" || clientRequestData.network_mode === "normal")) {
      
      const payloadBody = {
        chat_id: clientRequestData.chat_id,
        message_type: clientRequestData.message_type || "text",
        content: clientRequestData.content,
        network_mode: clientRequestData.network_mode
      };
      if (clientRequestData.reply_to_id) {
        payloadBody.reply_to_id = clientRequestData.reply_to_id;
      }

      const response = UrlFetchApp.fetch(`${CLOUDFLARE_WORKER_URL}/api/messages/send`, {
        method: "post",
        contentType: "application/json",
        headers: { "Authorization": "Bearer " + clientRequestData.token },
        payload: JSON.stringify(payloadBody),
        muteHttpExceptions: true
      });
      return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());
    }

    return createJsonResponse({ error: "درخواست نامعتبر است یا فرمت شبکه مشخص نیست." }, 400);

  } catch (error) {
    return createJsonResponse({ error: "خطای داخلی ریلی گوگل", details: error.toString() }, 500);
  }
}

/**
 * مدیریت درخواست‌های GET (برای گرفتن پیام‌ها، لیست چت‌ها و جستجوی کاربران از طریق گوگل)
 */
function doGet(e) {
  try {
    const action = e.parameter.action;
    const token = e.parameter.token;

    if (!token) {
      return createJsonResponse({ error: "توکن احراز هویت الزامی است." }, 401);
    }

    let targetUrl = `${CLOUDFLARE_WORKER_URL}/api/`;

    // روتینگ هوشمند درخواست بر اساس نوع عملیات (Action)
    if (action === "messages_get") {
      const chatId = e.parameter.chat_id;
      const limit = e.parameter.limit || "50";
      targetUrl += `messages/get?chat_id=${chatId}&limit=${limit}`;
    } else if (action === "chats_list") {
      targetUrl += `chats`;
    } else if (action === "users_search") {
      // 🔍 روت جستجوی آنلاین کاربران از طریق ریلی گوگل
      const query = e.parameter.query;
      if (!query) {
        return createJsonResponse({ error: "عبارت جستجو الزامی است." }, 400);
      }
      targetUrl += `users/search?query=${encodeURIComponent(query)}`;
    } else {
      return createJsonResponse({ error: "عملیات مورد نظر یافت نشد." }, 404);
    }

    // بازخوانی اطلاعات واقعی از کلودفلر از طریق تونل خروجی گوگل
    const response = UrlFetchApp.fetch(targetUrl, {
      method: "get",
      headers: { "Authorization": "Bearer " + token },
      muteHttpExceptions: true
    });

    return createJsonResponse(JSON.parse(response.getContentText()), response.getResponseCode());

  } catch (error) {
    return createJsonResponse({ error: "خطای ریلی گوگل در درخواست GET", details: error.toString() }, 500);
  }
}

/**
 * تابع کمکی برای ساخت پاسخ خروجی استاندارد JSON
 */
function createJsonResponse(data, statusCode) {
  const output = ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
  return output;
}