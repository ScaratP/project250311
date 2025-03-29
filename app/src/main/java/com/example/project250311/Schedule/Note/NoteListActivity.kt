package com.example.project250311.Schedule.Note

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project250311.Data.Note
import com.example.project250311.Data.NoteDatabase
import kotlinx.coroutines.launch

class NoteListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteListScreen()
        }
    }
}

@Composable
fun NoteListScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = NoteDatabase.getDatabase(context)
    val noteDao = db.noteDao()
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var deletedNote by remember { mutableStateOf<Note?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val expandedNotes = remember { mutableStateMapOf<Int, Boolean>() }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(searchQuery, refreshTrigger) {
        scope.launch {
            try {
                notes = if (searchQuery.isEmpty()) {
                    noteDao.getAllNotes()
                } else {
                    noteDao.searchNotes("%$searchQuery%")
                }
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load notes: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshTrigger++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Sticky Notes", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search notes...") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { context.startActivity(Intent(context, NoteActivity::class.java)) }) {
            Text("New Note")
        }
        Spacer(modifier = Modifier.height(8.dp))

        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        LazyColumn {
            items(notes) { note ->
                val isExpanded = expandedNotes[note.id] ?: false
                AnimatedVisibility(
                    visible = note != deletedNote,
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable { expandedNotes[note.id] = !isExpanded },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(note.timestamp),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(animationSpec = tween(300))
                            ) {
                                Text(
                                    text = note.toAnnotatedString(),
                                    fontSize = 16.sp
                                )
                            }
                            if (!isExpanded) {
                                val truncatedText = try {
                                    note.toAnnotatedString().let { annotated ->
                                        if (annotated.length > 50) {
                                            annotated.subSequence(0, 50) + AnnotatedString("...")
                                        } else {
                                            annotated
                                        }
                                    }
                                } catch (e: Exception) {
                                    AnnotatedString(note.content ?: "")
                                }
                                Text(
                                    text = truncatedText,
                                    fontSize = 16.sp,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        deletedNote = note
                                        noteDao.delete(note)
                                        notes = notes.filter { it != note }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to delete note: ${e.message}"
                                    }
                                }
                            }) {
                                Text("Delete", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}