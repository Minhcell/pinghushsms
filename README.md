# SmsPing (Android 15)

Kiểm tra một số điện thoại có online không, bằng cách gửi SMS "ping" kèm yêu cầu
**delivery report**, rồi hiện thông báo khi có báo cáo giao hàng trả về.

- Target Android 15 (SDK 35), min SDK 24.
- Dùng `SmsManager.sendTextMessage(..., sentIntent, deliveryIntent)` chuẩn — **không cần root, không Xposed**.
- SMS gửi đi là tin **hiện bình thường** ở máy nhận (không phải Type-0/silent).
- Notification viết lại bằng `NotificationCompat` + `NotificationChannel` + `FLAG_IMMUTABLE`
  (thay cho `setLatestEventInfo` đã bị gỡ từ Android 6).

## Build bằng GitHub (làm hoàn toàn trên điện thoại)

1. Tạo repo mới trên GitHub, upload toàn bộ thư mục này (giữ nguyên cấu trúc).
2. Vào tab **Actions** → chạy workflow **Build APK** (hoặc push là tự chạy).
3. Khi xong, mở lần chạy → tải artifact **SmsPing-debug-apk**.
4. Cài file `app-debug.apk`. Mở app, cấp quyền **SMS** và **Thông báo**, nhập số, bấm PING.

Workflow không cần `gradle-wrapper.jar` (dùng `gradle/actions/setup-gradle` với gradle-version),
nên không phải commit file nhị phân từ điện thoại.

## Vì sao KHÔNG dùng PWABuilder
PWA/TWA chạy trong WebView; web không có API gửi SMS. Không thể ping SMS từ PWA.

## Gửi Type-0 / Class-0 / MWI (silent) như HushSMS gốc
Không nằm trong bản này. Nó cần hook telephony nội bộ qua **LSPosed + root** và phải
build module riêng khớp chữ ký AOSP 15 — không thể làm qua GitHub Actions hay PWABuilder,
và không cần thiết cho việc kiểm tra số online.
