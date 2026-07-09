package com.vtv.smsping

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Nhận cả báo SENT (đã gửi) và DELIVERED (đã giao) và HIỆN THÔNG BÁO trên Android 8..15.
 * Thay hoàn toàn cho Notification cũ + setLatestEventInfo() đã bị gỡ từ API 23.
 */
class SmsDeliveryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SENT = "com.vtv.smsping.SMS_SENT"
        const val ACTION_DELIVERED = "com.vtv.smsping.SMS_DELIVERED"
        const val EXTRA_DEST = "dest"
        private const val CHANNEL_ID = "smsping_status"

        /** Cho MainActivity nghe để cập nhật log trên màn hình (nếu app đang mở). */
        @Volatile var listener: ((String) -> Unit)? = null

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java)
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_ID, "SMS status",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply { description = "Báo cáo gửi / giao SMS" }
                    )
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val dest = intent.getStringExtra(EXTRA_DEST) ?: "?"
        val text: String = when (intent.action) {
            ACTION_SENT -> when (resultCode) {
                Activity.RESULT_OK -> "[$dest] Đã gửi tới tổng đài ✓"
                SmsManager.RESULT_ERROR_NO_SERVICE -> "[$dest] Lỗi: không có sóng dịch vụ"
                SmsManager.RESULT_ERROR_RADIO_OFF -> "[$dest] Lỗi: radio đang tắt"
                SmsManager.RESULT_ERROR_NULL_PDU -> "[$dest] Lỗi: PDU rỗng"
                else -> "[$dest] Lỗi gửi (mã $resultCode)"
            }
            ACTION_DELIVERED -> when (resultCode) {
                Activity.RESULT_OK -> "[$dest] ĐÃ GIAO — số ONLINE ✓"
                else -> "[$dest] Chưa giao / đang chờ (số có thể offline)"
            }
            else -> return
        }

        listener?.invoke(text)                 // cập nhật màn hình nếu đang mở
        showNotification(context, dest, text)  // luôn hiện thông báo, kể cả app nền
    }

    private fun showNotification(context: Context, dest: String, text: String) {
        ensureChannel(context)

        val tap = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("SmsPing")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // id theo số + thời gian để mỗi báo cáo là 1 dòng riêng
            context.getSystemService(NotificationManager::class.java)
                .notify((dest + System.currentTimeMillis()).hashCode(), notif)
        }
    }
}
