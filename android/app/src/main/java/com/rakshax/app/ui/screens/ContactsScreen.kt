package com.rakshax.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.data.model.Contact
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.ui.components.ContactCard
import com.rakshax.app.ui.theme.*

@Composable
fun ContactsScreen() {
    val contacts by MockDataRepository.contacts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }

    Scaffold(
        containerColor = BackgroundDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmergencyRed,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "EMERGENCY TRUSTED CIRCLE",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Configured contacts will receive instant SMS, WhatsApp, and push alerts sequentially based on priority level during an SOS event.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trusted contacts added yet.\nTap '+' to add your emergency circle.",
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(contacts) { contact ->
                        ContactCard(
                            contact = contact,
                            onToggleEnabled = { MockDataRepository.toggleContactEnabled(it) },
                            onEdit = { editingContact = it },
                            onDelete = { MockDataRepository.deleteContact(it) }
                        )
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddDialog) {
        ContactFormDialog(
            title = "Add Trusted Contact",
            initialName = "",
            initialPhone = "+91 ",
            initialPriority = 1,
            initialRelation = "Family",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, priority, relation ->
                MockDataRepository.addContact(name, phone, priority, relation)
                showAddDialog = false
            }
        )
    }

    // Edit Contact Dialog
    editingContact?.let { contact ->
        ContactFormDialog(
            title = "Edit Contact",
            initialName = contact.name,
            initialPhone = contact.phone,
            initialPriority = contact.priority,
            initialRelation = contact.relation,
            onDismiss = { editingContact = null },
            onConfirm = { name, phone, priority, relation ->
                MockDataRepository.updateContact(
                    contact.copy(name = name, phone = phone, priority = priority, relation = relation)
                )
                editingContact = null
            }
        )
    }
}

@Composable
private fun ContactFormDialog(
    title: String,
    initialName: String,
    initialPhone: String,
    initialPriority: Int,
    initialRelation: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, priority: Int, relation: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var priority by remember { mutableStateOf(initialPriority) }
    var relation by remember { mutableStateOf(initialRelation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceBorder
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceBorder
                    )
                )

                OutlinedTextField(
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text("Relationship (e.g. Mother, Friend)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = SurfaceBorder
                    )
                )

                Text(
                    text = "Alert Priority Escalation:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "P1 (First)", 2 to "P2 (Then)", 3 to "P3 (Last)").forEach { (p, label) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmergencyRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, phone, priority, relation)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
            ) {
                Text("SAVE CONTACT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
