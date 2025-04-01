package com.example.project250311.Schedule.GetSchedule

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.project250311.Data.AppDatabase
import com.example.project250311.Data.CourseRepository
import com.example.project250311.Data.CourseViewModel
import com.example.project250311.Data.Schedule
import com.example.project250311.MainActivity
import com.example.project250311.Schedule.NoSchool.GetLeaveDataActivity
import com.example.project250311.Schedule.Notice.NoticeActivity
import com.example.project250311.Schedule.Notice.NotificationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters



class GetScheduleActivity : ComponentActivity() {
    private val viewModel: CourseViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(applicationContext)
                val repository = CourseRepository(db.courseDao())
                @Suppress("UNCHECKED_CAST")
                return CourseViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this) //建立通知頻道
        requestNotificationPermission(this)
        setContent {
            ScheduleScreen(viewModel)
        }
    }
}

// 使用 withContext(Dispatchers.IO) 在背景線程抓取資料
suspend fun fetchWebData(url: String, cookies: String?): List<Schedule> {
    return withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).apply {
                cookies?.let { header("Cookie", it) }
                userAgent("Mozilla/5.0")
                timeout(10000)
            }.get()

            val table = doc.select("table.NTTU_GridView")
            val rows = table.select("tr")
            val tempDataList = mutableListOf<Schedule>()

            val weekDays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
            val startTimes = listOf(
                "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00",
                "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00"
            ).map { LocalTime.parse(it) }
            val endTimes = listOf(
                "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00",
                "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00"
            ).map { LocalTime.parse(it) }

            // 收集階段：使用原始時間，不提前調整
            val contentRows = rows.drop(1)
            for ((rowIndex, row) in contentRows.withIndex()) {
                val columns = row.select("td")
                for ((colIndex, column) in columns.withIndex()) {
                    val span = column.selectFirst("span[title]")
                    val title = span?.attr("title")?.trim() ?: ""

                    if (rowIndex >= startTimes.size) continue

                    val weekDay = if (colIndex in 1..7) weekDays[colIndex - 1] else "未知"
                    val startTime = startTimes[rowIndex]
                    val endTime = endTimes[rowIndex]

                    if (title.isNotEmpty()) {
                        val parsedData = parseTitle(title)
                        val courseSchedule = Schedule(
                            id = "$colIndex$rowIndex",
                            courseName = parsedData["科目名稱"] ?: "未知課程",
                            teacherName = parsedData["授課教師"] ?: "未知教師",
                            location = parsedData["場地"] ?: "其它",
                            weekDay = weekDay,
                            startTime = startTime,
                            endTime = endTime,
                            courseDates = getCourseDates(weekDay)
                        )
                        tempDataList.add(courseSchedule)
                    }
                }
            }

            // 合併連續時間並在最後調整開始時間
            val mergedDataList = mutableListOf<Schedule>()
            tempDataList.groupBy { it.courseName to it.weekDay }.forEach { (key, courses) ->
                val sortedCourses = courses.sortedBy { it.startTime }
                var currentStartTime: LocalTime? = null
                var currentEndTime: LocalTime? = null
                var currentId: String = sortedCourses.first().id

                for (course in sortedCourses) {
                    if (currentStartTime == null) {
                        currentStartTime = course.startTime
                        currentEndTime = course.endTime
                    } else if (currentEndTime == course.startTime) {
                        // 連續時間，更新結束時間
                        currentEndTime = course.endTime
                    } else {
                        mergedDataList.add(
                            Schedule(
                                id = currentId,
                                courseName = key.first,
                                teacherName = courses.first().teacherName,
                                location = courses.first().location,
                                weekDay = key.second,
                                startTime = currentStartTime.plusMinutes(10),
                                endTime = currentEndTime!!,
                                courseDates = getCourseDates(key.second) // 傳遞計算出的課程日期
                            )
                        )
                        currentStartTime = course.startTime
                        currentEndTime = course.endTime
                        currentId = course.id
                    }
                }
                if (currentStartTime != null && currentEndTime != null) {
                    mergedDataList.add(
                        Schedule(
                            id = currentId,
                            courseName = key.first,
                            teacherName = courses.first().teacherName,
                            location = courses.first().location,
                            weekDay = key.second,
                            startTime = currentStartTime.plusMinutes(10),
                            endTime = currentEndTime,
                            courseDates = getCourseDates(key.second) // 傳遞計算出的課程日期
                        )
                    )
                }
            }

            Log.d("fetchWebData", "Final merged data: $mergedDataList")
            mergedDataList
        } catch (e: Exception) {
            Log.e("fetchWebData", "Error fetching data", e)
            emptyList()
        }
    }
}

fun parseTitle(title: String): Map<String, String> {
    val regexMap = mapOf(
        "科目名稱" to """科目名稱：(.+?)\n""",
        "授課教師" to """授課教師：(.+?)\n""",
        "場地" to """場地：(.+?)\n"""
    )
    val result = mutableMapOf<String, String>()
    regexMap.forEach { (key, pattern) ->
        val regex = Regex(pattern)
        regex.find(title)?.let { match ->
            result[key] = match.groupValues.drop(1).joinToString(" ").trim()
        }
    }
    return result
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String, onLoginSuccess: (String) -> Unit) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (url.contains("InfoLoginNew.aspx")) {
                        view.scrollTo(0, 0)
                        return
                    }
                    val cookies = CookieManager.getInstance().getCookie(url)
                    onLoginSuccess(cookies ?: "")
                }
            }
            loadUrl(url)
        }
    })
}

@Composable
fun ScheduleScreen(viewModel: CourseViewModel) {
    var cookies by remember { mutableStateOf<String?>(null) }
    val scheduleList by viewModel.allCourses.observeAsState(emptyList())
    val selectedCourses by viewModel.selectedCourses.observeAsState(null)
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val onCourseSelected = remember(viewModel) {
        { courses: List<Schedule>? -> viewModel.selectCourses(courses) }
    }

    // 初始載入資料，決定是否需要登入
    LaunchedEffect(Unit) {
        // 直接調用 suspend loadAllCourses() 並獲得 Boolean 結果
        val hasData = viewModel.loadAllCourses()
        if (!hasData) {
            cookies = null
            Log.d("ScheduleScreen", "No data in database, triggering login")
        } else {
            cookies = "existing"
            Log.d("ScheduleScreen", "App restarted with existing data, skipping login")
        }
    }

    // 當 cookies 更新時，刷新資料
    LaunchedEffect(cookies) {
        if (cookies != null && cookies != "existing") {
            isLoading = true
            val success = fetchNewData(viewModel, cookies!!)
            if (success) {
                Log.d("ScheduleScreen", "Data fetch and import completed successfully")
            } else {
                Log.e("ScheduleScreen", "Failed to fetch or import data")
            }
            isLoading = false
        }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars // 確保內容不會被系統列遮擋
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 應用內邊距以適應系統列.fillMaxSize())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "課程表",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                cookies = null // 觸發重新登入並強制抓取
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        } else {
                            Text("刷新")
                        }
                    }

                    Button(
                        onClick = {
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("返回")
                    }
                }
            }

            if (cookies == null) {
                WebViewScreen("https://infosys.nttu.edu.tw/InfoLoginNew.aspx") { newCookies ->
                    cookies = newCookies
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        ScheduleTable(
                            scheduleList = scheduleList,
                            onCourseSelected = onCourseSelected
                        )
                    }
                    selectedCourses?.let { courses ->
                        Spacer(modifier = Modifier.height(16.dp))
                        CourseDetailCard(
                            courses = courses,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleTable(
    scheduleList: List<Schedule>,
    onCourseSelected: (List<Schedule>?) -> Unit
) {
    val timeSlots = listOf(
        "07:00" to "08:00", "08:00" to "09:00", "09:00" to "10:00", "10:00" to "11:00",
        "11:00" to "12:00", "12:00" to "13:00", "13:00" to "14:00", "14:00" to "15:00",
        "15:00" to "16:00", "16:00" to "17:00", "17:00" to "18:00", "18:00" to "19:00",
        "19:00" to "20:00", "20:00" to "21:00", "21:00" to "22:00"
    ).map { LocalTime.parse(it.first) to LocalTime.parse(it.second) }

    val weekDays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    val activeCols = scheduleList.map { weekDays.indexOf(it.weekDay) + 1 }
        .filter { it > 0 }
        .distinct()
        .sorted()
    val activeRows = (0 until timeSlots.size).filter { rowIndex ->
        scheduleList.any { course ->
            val slotStart = timeSlots[rowIndex].first
            val slotEnd = timeSlots[rowIndex].second
            course.startTime < slotEnd && course.endTime > slotStart
        }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Schedule",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                if (scheduleList.isEmpty()) {
                    Text(
                        text = "No schedule found",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    Log.d("ScheduleScreen", "Rendering schedule with ${scheduleList.size} courses")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 4.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                activeCols.forEach { colIndex ->
                                    Text(
                                        text = weekDays[colIndex - 1],
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            activeRows.forEach { rowIndex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (activeRows.indexOf(rowIndex) % 2 == 0)
                                                MaterialTheme.colorScheme.surface
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "第${rowIndex}節",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${timeSlots[rowIndex].first.toString().substring(0, 5)}-${timeSlots[rowIndex].second.toString().substring(0, 5)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    activeCols.forEach { colIndex ->
                                        val slotStart = timeSlots[rowIndex].first
                                        val slotEnd = timeSlots[rowIndex].second
                                        val weekDay = weekDays[colIndex - 1]
                                        val courseAtSlot = scheduleList.firstOrNull { course ->
                                            val matches = course.weekDay == weekDay &&
                                                    course.startTime < slotEnd &&
                                                    course.endTime > slotStart
                                            Log.d("ScheduleScreen", "col=$colIndex, row=$rowIndex, course=${course.courseName}, matches=$matches")
                                            matches
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(60.dp)
                                                .padding(2.dp)
                                                .background(
                                                    if (courseAtSlot != null)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else Color.Transparent,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable(enabled = courseAtSlot != null) {
                                                    courseAtSlot?.let {
                                                        val relatedCourses = scheduleList.filter { course ->
                                                            course.courseName == it.courseName
                                                        }
                                                        onCourseSelected(relatedCourses)
                                                    }
                                                }
                                                .border(
                                                    width = 0.5.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = courseAtSlot?.courseName ?: "",
                                                fontSize = 12.sp,
                                                color = if (courseAtSlot != null)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else Color.Transparent,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseDetailCard(
    courses: List<Schedule>,
    viewModel: CourseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        createNotificationChannel(context)
        activity?.let { requestNotificationPermission(it) }
    }

    if (courses.isEmpty()) return

    var showEditDialog by remember { mutableStateOf(false) }
    var newLocation by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { showEditDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "課程: ${courses.first().courseName}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "老師: ${courses.map { it.teacherName }.distinct().joinToString(", ")}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "地點: ${courses.map { it.location }.distinct().joinToString(", ")}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "時間安排:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            courses.forEach { course ->
                CourseItem(course = course)
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("編輯地點") },
            text = {
                TextField(
                    value = newLocation,
                    onValueChange = { newLocation = it },
                    label = { Text("輸入新地點") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLocation.isNotBlank()) {
                            val updatedLocation = newLocation
                            scope.launch {
                                courses.forEach { course ->
                                    viewModel.updateCourseLocation(course.id, updatedLocation)
                                }
                            }
                            showEditDialog = false
                            newLocation = ""
                        }
                    }
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CourseItem(course: Schedule) {
    val context = LocalContext.current
    val notificationStates = remember { mutableStateMapOf<String, Boolean>() }
    val isNotificationEnabled = notificationStates[course.id] ?: false

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${course.weekDay} ${
                    course.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                } - ${course.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isNotificationEnabled,
            onCheckedChange = { isEnabled ->
                notificationStates[course.id] = isEnabled
                if (isEnabled) {
                    // 對每個上課日期設定通知
                    course.courseDates.forEach { date ->
                        setNoticationAlarm(context, course, date)
                    }
                } else {
                    cancelNotificatuon(context, course)
                }
            }
        )
    }
}


fun setNoticationAlarm(context: Context, course: Schedule, courseDate: LocalDate) {
    val alarmTime = course.startTime.minusMinutes(10) // 提前 10 分鐘通知

    // 設定鬧鐘時間（確保是課程當天）
    val alarmCalendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, courseDate.year)
        set(java.util.Calendar.MONTH, courseDate.monthValue - 1) // Calendar 的 month 是 0-based
        set(java.util.Calendar.DAY_OF_MONTH, courseDate.dayOfMonth - 1)
        set(java.util.Calendar.HOUR_OF_DAY, alarmTime.hour)
        set(java.util.Calendar.MINUTE, alarmTime.minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

    val now = java.util.Calendar.getInstance()
    if (alarmCalendar.timeInMillis <= now.timeInMillis) {
        // **如果時間已經過去，不應該推遲一週，而是直接跳過**
        return
    }

    // 讓 PendingIntent 唯一，防止不同天的相同時間課程被覆蓋
    val requestCode = "${course.id}_${courseDate}".hashCode()

    val alarmIntent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("course_id", course.id)
        putExtra("course_name", course.courseName)
        putExtra("teacher_name", course.teacherName)
        putExtra("location", course.location)
        putExtra("start_time", course.startTime.toString())
        putExtra("end_time", course.endTime.toString())
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode, // 確保不同日期的課程不會覆蓋
        alarmIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    alarmManager.setExactAndAllowWhileIdle(
        android.app.AlarmManager.RTC_WAKEUP,
        alarmCalendar.timeInMillis,
        pendingIntent
    )
}



private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //判斷版本是否在API26以上
        val channelId = "notify_id" //通道的唯一辨識符號
        val channelName = "通知通道" //顯示給使用者的通知名稱
        val channelDescription = "這是APP的通知通道" //用來描述通道的用途
        val importance = NotificationManager.IMPORTANCE_HIGH //通知優先順序
        val channel = NotificationChannel(channelId, channelName, importance).apply { //創建新的通知通道
            description = channelDescription //設定通知描述
        }
        //取得NotificationManager並建立通知通道
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager //取得系統通知管理氣
        notificationManager.createNotificationChannel(channel) //註冊通知通道
    }
}

fun requestNotificationPermission(activity: Activity){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 如果沒有權限，請求權限
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
            return
        }
    }

}

fun cancelNotificatuon(context: Context,course: Schedule){
    val notificationId = course.id.hashCode()

    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.cancel(notificationId)
}


fun getCourseDates(weekDay: String): List<java.time.LocalDate> {
    val now = java.time.LocalDate.now() // 取得今天的日期
    // 以最近一個星期一作為起始日期
    val semesterStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    // 假設學期為 18 週
    val semesterEnd = semesterStart.plusWeeks(18)

    // 找到對應的 DayOfWeek
    val targetDayOfWeek = when (weekDay) {
        "星期一" -> DayOfWeek.MONDAY
        "星期二" -> DayOfWeek.TUESDAY
        "星期三" -> DayOfWeek.WEDNESDAY
        "星期四" -> DayOfWeek.THURSDAY
        "星期五" -> DayOfWeek.FRIDAY
        "星期六" -> DayOfWeek.SATURDAY
        "星期日" -> DayOfWeek.SUNDAY
        else -> return emptyList()
    }

    val dates = mutableListOf<java.time.LocalDate>()
    var date = semesterStart.with(TemporalAdjusters.nextOrSame(targetDayOfWeek)) // 找到最近的該星期幾

    while (!date.isAfter(semesterEnd)) {
        dates.add(date)
        date = date.plusWeeks(1) // 每週同一天
    }
    return dates
}




/*fun sendNotification(context: Context, course: Schedule) {

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
    val notificationId = System.currentTimeMillis().toInt() //確保通知ID唯一


    //查看課表
    val url1 = "https://www.notion.so/115-14b63e698496818bb669f9073a87823f"
    val scheduleIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url1))

    val schedulePendingIntent = PendingIntent.getActivity( //點擊開啟
        context,0,scheduleIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    //開起請假系統
    val leaveIntent = Intent(context, GetLeaveDataActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val leavePendingIntent = PendingIntent.getActivity(
        context, 0, leaveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    //建立通知
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setContentTitle("通知提醒!!")
        .setContentText("課程: ${course.courseName}\n老師:${course.teacherName}\n地點:${course.location}\n時間:${course.startTime}~${course.endTime}")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .addAction(android.R.drawable.ic_menu_agenda,"查看課表",schedulePendingIntent)
        .addAction(android.R.drawable.ic_menu_view,"請假系統",leavePendingIntent)

    with(NotificationManagerCompat.from(context)) {
        notify(notificationId, builder.build())
    }
}*/

// 修改後的 fetchNewData：使用 suspend 清除、插入與讀取課程
suspend fun fetchNewData(viewModel: CourseViewModel, cookies: String): Boolean {
    val fetchedData = fetchWebData(
        "https://infosys.nttu.edu.tw/n_CourseBase_Select/WeekCourseList.aspx?ItemParam=",
        cookies
    )
    return if (fetchedData.isNotEmpty()) {
        withContext(Dispatchers.IO) {
            viewModel.clearAllCourses()
            Log.d("fetchNewData", "All courses cleared")
            fetchedData.forEach { course ->
                viewModel.insertCourse(course)
                Log.d("fetchNewData", "Inserted course: $course")
            }
            viewModel.loadAllCourses()
            Log.d("fetchNewData", "Courses loaded")
        }
        true
    } else {
        false
    }
}
