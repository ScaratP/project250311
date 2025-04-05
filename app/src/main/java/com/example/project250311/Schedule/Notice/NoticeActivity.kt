package com.example.project250311.Schedule.Notice

import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.PendingIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.project250311.Data.Schedule
import com.example.project250311.Schedule.GetSchedule.setNoticationAlarm
import com.example.project250311.Schedule.NoSchool.GetLeaveDataActivity
import com.example.project250311.ui.theme.Project250311Theme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import androidx.compose.runtime.*
import java.util.Calendar

class NoticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel() // 建立通知頻道
        requestNotificationPermission()

        setContent {
            Project250311Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotificationButton()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26 以上
            val channelId = "notify_id" // 通道唯一辨識符
            val channelName = "通知通道" // 顯示給使用者的名稱
            val channelDescription = "這是APP的通知通道" // 通道描述
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}

@Composable
fun NotificationButton() {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var reminders by remember { mutableStateOf(listOf<String>()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 立即發送通知的按鈕
            Button(
                onClick = {
                    val testSchedule = Schedule(
                        id = "test1",
                        courseName = "立即測試課程",
                        teacherName = "立即測試老師",
                        location = "立即測試教室",
                        weekDay = "今天",
                        startTime = LocalTime.now().plusMinutes(1),
                        endTime = LocalTime.now().plusMinutes(2),
                        courseDates = listOf(LocalDate.now())
                    )
                    // 立即發送通知
                    sendNotification(context, testSchedule)
                }
            ) {
                Text("立即發送通知")
            }

            // 測試未來鬧鐘通知的按鈕
            Button(
                onClick = {
                    val today = LocalDate.now()
                    val weekDay = when (today.dayOfWeek) {
                        java.time.DayOfWeek.MONDAY -> "星期一"
                        java.time.DayOfWeek.TUESDAY -> "星期二"
                        java.time.DayOfWeek.WEDNESDAY -> "星期三"
                        java.time.DayOfWeek.THURSDAY -> "星期四"
                        java.time.DayOfWeek.FRIDAY -> "星期五"
                        java.time.DayOfWeek.SATURDAY -> "星期六"
                        java.time.DayOfWeek.SUNDAY -> "星期日"
                    }

                    val testSchedule = Schedule(
                        id = "test1",
                        courseName = "測試課程",
                        teacherName = "測試老師",
                        location = "測試教室",
                        weekDay = weekDay,
                        startTime = LocalTime.now().plusMinutes(2),
                        endTime = LocalTime.now().plusMinutes(3),
                        courseDates = listOf(today)
                    )

                    // 設定鬧鐘為當前時間後 1 分鐘觸發通知
                    val testAlarmTime = LocalDateTime.of(today, LocalTime.now().plusMinutes(1))
                    setNoticationAlarm(context, testSchedule, today)
                }
            ) {
                Text("測試未來鬧鐘通知")
            }

            Button(onClick = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }) {
                Text("選擇日期")
            }
            Text(text = "已選日期: $selectedDate", modifier = Modifier.padding(start = 8.dp))

            Button(onClick = {
                val calendar = Calendar.getInstance()
                TimePickerDialog(context, { _, hour, minute ->
                    selectedTime = LocalTime.of(hour, minute)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }) {
                Text("選擇時間")
            }
            Text(text = "已選時間: $selectedTime", modifier = Modifier.padding(start = 8.dp))

            Button(onClick = {
                val testSchedule = Schedule(
                    id = "custom",
                    courseName = "這是自訂的!",
                    teacherName = "我選的日期是$selectedDate",
                    location = "我選的時間是$selectedTime",
                    weekDay = "自訂",
                    startTime = selectedTime,
                    endTime = selectedTime.plusMinutes(1),
                    courseDates = listOf(selectedDate)
                )
                setNoticationAlarm(context, testSchedule, selectedDate)
                reminders = reminders + "$selectedDate $selectedTime"
            }) {
                Text("設定提醒")
            }
            Text("目前提醒: ${reminders.joinToString(", ")}", modifier = Modifier.padding(top = 16.dp))

        }
    }
}

// 用於立即發送通知的函式
fun sendNotification(context: Context, schedule: Schedule) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }

    val channelId = "notify_id"
    val notificationId = System.currentTimeMillis().toInt()

    // 查看課表的 Intent
    val url = "https://www.notion.so/115-14b63e698496818bb669f9073a87823f"
    val scheduleIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    val schedulePendingIntent = PendingIntent.getActivity(
        context, 0, scheduleIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 請假系統的 Intent
    val leaveIntent = Intent(context, GetLeaveDataActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val leavePendingIntent = PendingIntent.getActivity(
        context, 0, leaveIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 建立通知
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setContentTitle("通知提醒!!")
        .setContentText("該去上課了!!")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .addAction(android.R.drawable.ic_menu_agenda, "查看課表", schedulePendingIntent)
        .addAction(android.R.drawable.ic_menu_view, "請假系統", leavePendingIntent)

    with(NotificationManagerCompat.from(context)) {
        notify(notificationId, builder.build())
    }
}
