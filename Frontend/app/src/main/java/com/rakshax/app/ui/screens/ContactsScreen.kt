package com.rakshax.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rakshax.app.data.model.Contact
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.data.sos.EmergencyCallHelper
import com.rakshax.app.ui.components.ContactCard
import com.rakshax.app.ui.theme.*

@Composable
fun ContactsScreen() {
    val contacts by MockDataRepository.contacts.collectAsState()
    val context = LocalContext.current

    var emergencyPermissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val emergencyPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        emergencyPermissionsGranted = results.values.all { it }
    }

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
                text = "Configured contacts will receive an immediate phone call and automated alerts sequentially during an SOS event triggered by the ESP32 device.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            if (!emergencyPermissionsGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Call & SMS Permissions",
                                color = WarningAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Grant permissions so SOS triggers an instant direct call and sends confirmation SMS links.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                emergencyPermissionsLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CALL_PHONE,
                                        Manifest.permission.SEND_SMS
                                    )
                                )
                            }
                        ) {
                            Text("ALLOW", color = SafeGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

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
                            onDelete = { MockDataRepository.deleteContact(it) },
                            onCall = { EmergencyCallHelper.callContact(context, it) }
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
