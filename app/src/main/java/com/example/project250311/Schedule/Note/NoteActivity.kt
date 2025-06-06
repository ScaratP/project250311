package com.example.project250311.Schedule.Note

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project250311.Data.Note
import com.example.project250311.Data.NoteDatabase
import com.example.project250311.MainActivity
import com.example.project250311.R
import kotlinx.coroutines.launch

class NoteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteScreen()
        }
    }
}

@Composable
fun NoteScreen() {
    var currentBold by remember { mutableStateOf(false) }
    var currentItalic by remember { mutableStateOf(false) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentSize by remember { mutableStateOf(20.sp) }
    var currentMark by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var annotatedString by remember { mutableStateOf(AnnotatedString("")) }
    var saveColor by remember { mutableStateOf(Color.Gray) }
    var isTextModified by remember { mutableStateOf(false) } // 追蹤文字是否被修改
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = NoteDatabase.getDatabase(context)
    val noteDao = db.noteDao()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) } // 控制對話框顯示
    var pendingNavigation by remember { mutableStateOf<String?>(null) } // 儲存待處理的導航目標

    // 儲存便利貼的函數
    fun saveNote(onSuccess: () -> Unit) {
        scope.launch {
            val note = Note(
                content = textFieldValue.text,
                formattedContent = Note(0, null, null).toFormattedContent(annotatedString)
            )
            noteDao.insert(note)
            saveColor = Color.Black
            isTextModified = false
            snackbarHostState.showSnackbar("正在貼上新的便利貼!")
            onSuccess()
        }
    }

    // 處理導航的函數
    fun handleNavigation(target: String) {
        when (target) {
            "Back" -> {
                context.startActivity(Intent(context, NoteListActivity::class.java))
                (context as? ComponentActivity)?.finish()
            }
            "Home" -> {
                context.startActivity(Intent(context, MainActivity::class.java))
                (context as? ComponentActivity)?.finish()
            }
        }
    }

    // 對話框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("便利貼還未儲存!!!") },
            text = { Text("請問是否儲存?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveNote {
                            showDialog = false
                            pendingNavigation?.let { handleNavigation(it) }
                            pendingNavigation = null
                        }
                    }
                ) {
                    Text("是")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        pendingNavigation?.let { handleNavigation(it) }
                        pendingNavigation = null
                    }
                ) {
                    Text("否")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (isTextModified) {
                            showDialog = true
                            pendingNavigation = "Back"
                        } else {
                            context.startActivity(Intent(context, NoteListActivity::class.java))
                            (context as? ComponentActivity)?.finish()
                        }
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Back", fontSize = 16.sp)
                }
                Text(
                    text = "SAVE",
                    color = saveColor,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .height(36.dp)
                        .clickable {
                            saveNote {
                                context.startActivity(Intent(context, NoteListActivity::class.java))
                                (context as? ComponentActivity)?.finish()
                            }
                        }
                        .wrapContentHeight(align = Alignment.CenterVertically)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomButton("B", currentBold, { currentBold = !currentBold })
                    CustomButton("I", currentItalic, { currentItalic = !currentItalic })
                    CustomButton("Big", currentSize == 24.sp, { currentSize = if (currentSize == 24.sp) 20.sp else 24.sp })
                    CustomButton("Small", currentSize == 12.sp, { currentSize = if (currentSize == 12.sp) 20.sp else 12.sp })
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomButton("Red", currentColor == Color.Red, { currentColor = if (currentColor == Color.Red) Color.Black else Color.Red })
                    CustomButton("Blue", currentColor == Color.Blue, { currentColor = if (currentColor == Color.Blue) Color.Black else Color.Blue })
                    CustomButton("Mark", currentMark, { currentMark = !currentMark })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val previousText = textFieldValue.text
                    val newText = newValue.text
                    val previousLength = previousText.length
                    val newLength = newText.length

                    val builder = AnnotatedString.Builder()
                    builder.append(newText)
                    annotatedString.spanStyles.forEach { style ->
                        if (style.start < previousLength && style.end <= previousLength) {
                            builder.addStyle(style.item, style.start, minOf(style.end, newLength))
                        }
                    }
                    if (newLength > previousLength) {
                        builder.addStyle(
                            SpanStyle(
                                fontSize = currentSize,
                                fontWeight = if (currentBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (currentItalic) FontStyle.Italic else FontStyle.Normal,
                                color = currentColor,
                                background = if (currentMark) Color(0xFFFFE082) else Color.Transparent
                            ),
                            previousLength,
                            newLength
                        )
                    }
                    annotatedString = builder.toAnnotatedString()
                    textFieldValue = newValue.copy(annotatedString = annotatedString)
                    isTextModified = true // 文字變更時設為 true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .border(2.dp, Color.Black),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFFF9C4),
                    unfocusedContainerColor = Color(0xFFFFF9C4)
                )
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                if (isTextModified) {
                    showDialog = true
                    pendingNavigation = "Home"
                } else {
                    context.startActivity(Intent(context, MainActivity::class.java))
                    (context as? ComponentActivity)?.finish()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.home),
                contentDescription = "Go to Home",
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun CustomButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFB5B1B8) else Color(0xFFE6E0E9)
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, color = Color.Black)
    }
}