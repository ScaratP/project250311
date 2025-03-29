package com.example.project250311.Schedule.Note

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project250311.Data.Note
import com.example.project250311.Data.NoteDatabase
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = NoteDatabase.getDatabase(context)
    val noteDao = db.noteDao()
    val snackbarHostState = remember { SnackbarHostState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9C4))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {
                context.startActivity(Intent(context, NoteListActivity::class.java))
                (context as? ComponentActivity)?.finish()
            }) {
                Text("Back", fontSize = 16.sp)
            }
            Text(
                text = "SAVE",
                color = saveColor,
                fontSize = 16.sp,
                modifier = Modifier.clickable {
                    scope.launch {
                        val note = Note(
                            content = textFieldValue.text,
                            formattedContent = Note(0, null, null).toFormattedContent(annotatedString)
                        )
                        noteDao.insert(note)
                        saveColor = Color.Black
                        snackbarHostState.showSnackbar("貼上新便利貼了!")
                        context.startActivity(Intent(context, NoteListActivity::class.java))
                        (context as? ComponentActivity)?.finish()
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomButton("B", currentBold, { currentBold = !currentBold })
            CustomButton("I", currentItalic, { currentItalic = !currentItalic })
            CustomButton("Red", currentColor == Color.Red, { currentColor = if (currentColor == Color.Red) Color.Black else Color.Red })
            CustomButton("Blue", currentColor == Color.Blue, { currentColor = if (currentColor == Color.Blue) Color.Black else Color.Blue })
            CustomButton("Mark", currentMark, { currentMark = !currentMark })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomButton("Big", currentSize == 24.sp, { currentSize = if (currentSize == 24.sp) 20.sp else 24.sp })
            CustomButton("Small", currentSize == 12.sp, { currentSize = if (currentSize == 12.sp) 20.sp else 12.sp })
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color.Gray),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun CustomButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF606060) else Color(0xFF808080)
        ),
        modifier = Modifier.height(40.dp)
    ) {
        Text(text, color = Color.White)
    }
}