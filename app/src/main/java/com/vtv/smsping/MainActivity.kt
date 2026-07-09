package com.vtv.smsping

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.text.format.DateFormat
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var log: TextView
    private var pending: String? = null   // số đang chờ gửi sau khi xin quyền
    private var reqCode = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SmsDeliveryReceiver.ensureChannel(this)
        log = findViewById(R.id.tvLog)
        val num = findViewById<EditText>(R.id.etNumber)

        findViewById<Button>(R.id.btnPing).setOnClickListener {
            val n = num.text.toString().trim()
            if (n.isEmpty()) {
                Toast.makeText(this, "Nhập số điện thoại", Toast.LENGTH_SHORT).show()
            } else {
                ensurePermissionsThenSend(n)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // nghe báo cáo để in ra màn hình
        SmsDeliveryReceiver.listener = { line -> runOnUiThread { append(line) } }
    }

    override fun onPause() {
        super.onPause()
        SmsDeliveryReceiver.listener = null
    }

    private fun ensurePermissionsThenSend(number: String) {
        val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) need += Manifest.permission.SEND_SMS

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) need += Manifest.permission.POST_NOTIFICATIONS

        if (need.isEmpty()) {
            sendPing(number)
        } else {
            pending = number
            ActivityCompat.requestPermissions(this, need.toTypedArray(), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val smsIdx = permissions.indexOf(Manifest.permission.SEND_SMS)
        val smsOk = smsIdx < 0 ||
            (smsIdx < grantResults.size && grantResults[smsIdx] == PackageManager.PERMISSION_GRANTED)
        val n = pending
        pending = null
        if (smsOk && n != null) sendPing(n)
        else append("Chưa cấp quyền SEND_SMS — không gửi được.")
    }

    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            getSystemService(SmsManager::class.java)
        else
            @Suppress("DEPRECATION") SmsManager.getDefault()

    private fun sendPing(number: String) {
        val id = reqCode++
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val sent = PendingIntent.getBroadcast(
            this, id,
            Intent(this, SmsDeliveryReceiver::class.java)
                .setAction(SmsDeliveryReceiver.ACTION_SENT)
                .putExtra(SmsDeliveryReceiver.EXTRA_DEST, number),
            flags
        )
        val delivered = PendingIntent.getBroadcast(
            this, id + 500000,
            Intent(this, SmsDeliveryReceiver::class.java)
                .setAction(SmsDeliveryReceiver.ACTION_DELIVERED)
                .putExtra(SmsDeliveryReceiver.EXTRA_DEST, number),
            flags
        )

        try {
            // Body ngắn; tin nhắn này HIỆN bình thường ở máy nhận (không phải Type-0 ẩn).
            smsManager().sendTextMessage(number, null, "ping", sent, delivered)
            append("[$number] Đang gửi ping...")
        } catch (e: Exception) {
            append("[$number] Lỗi: ${e.message}")
        }
    }

    private fun append(line: String) {
        val t = DateFormat.format("HH:mm:ss", System.currentTimeMillis())
        log.text = "$t  $line\n${log.text}"
    }
}
