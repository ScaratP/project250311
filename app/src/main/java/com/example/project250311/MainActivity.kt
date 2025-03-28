package com.example.project250311

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.project250311.Schedule.GetSchedule.GetScheduleActivity
import com.example.project250311.Schedule.NoSchool.GetLeaveDataActivity
import com.example.project250311.Schedule.Note.NoteActivity
import com.example.project250311.Schedule.Note.NoteListActivity
import com.example.project250311.Schedule.Notice.NoticeActivity
import com.example.project250311.ui.theme.Project250311Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Project250311Theme {
                // 主畫面的 Compose UI
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen123() {
    val context = LocalContext.current

    Project250311Theme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 上半部兩個按鈕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MenuButton(
                    text = "Schedule",
                    icon = Icons.Default.CalendarToday,
                    onClick = {
                        context.startActivity(Intent(context, GetScheduleActivity::class.java))
                    },
                    modifier = Modifier.weight(1f)
                )
                MenuButton(
                    text = "Notice",
                    icon = Icons.Default.Notifications,
                    onClick = {
                        context.startActivity(Intent(context, NoticeActivity::class.java))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 下半部三個按鈕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MenuButton(
                    text = "Leave",
                    icon = Icons.Default.ExitToApp,
                    onClick = {
                        context.startActivity(Intent(context, GetLeaveDataActivity::class.java))
                    },
                    modifier = Modifier.weight(1f)
                )
                MenuButton(
                    text = "Notes",
                    icon = Icons.Default.Note,
                    onClick = {
                        context.startActivity(Intent(context, NoteListActivity::class.java))
                    },
                    modifier = Modifier.weight(1f)
                )
                MenuButton(
                    text = "More",
                    icon = Icons.Default.MoreHoriz,
                    onClick = {
                        context.startActivity(Intent(context, NoteActivity::class.java))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxHeight(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf("Select") } // 預設顯示選擇的選項
    val options = listOf("Schedule", "Leave", "Notice", "Notes")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顯示下拉選單
        DropDownMenuButton(
            text = "Go to", // 這裡是顯示在下拉選單旁邊的提示文字
            options = options,
            selectedOption = selectedOption,
            onOptionSelected = { option ->
                when (option) {
                    "Schedule" -> context.startActivity(Intent(context, GetScheduleActivity::class.java))
                    "Leave" -> context.startActivity(Intent(context, GetLeaveDataActivity::class.java))
                    "Notice" -> context.startActivity(Intent(context, NoticeActivity::class.java))
                    "Notes" -> context.startActivity(Intent(context, NoteActivity::class.java))
                }
            }
        )
    }
}

@Composable
fun DropDownMenuButton(
    text: String,  // 這個參數需要提供
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // DropdownMenu 使用的是 Material3 的穩定 API
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
    ) {
        // 顯示當前選擇的選項
        TextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(text) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "下拉選單"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = !expanded })
        )

        // 顯示下拉選單
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}