package com.rakshax.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.data.model.Contact
import com.rakshax.app.ui.theme.*

@Composable
fun ContactCard(
    contact: Contact,
    onToggleEnabled: (String) -> Unit,
    onEdit: (Contact) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (priorityText, priorityColor) = when (contact.priority) {
        1 -> Pair("PRIORITY 1 (IMMEDIATE)", EmergencyRed)
        2 -> Pair("PRIORITY 2 (SECONDARY)", WarningAmber)
        else -> Pair("PRIORITY 3 (FALLBACK)", InfoCyan)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(
                1.dp,
                if (contact.isEnabled) SurfaceBorder else SurfaceBorder.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(priorityColor.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = priorityColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.name,
                    color = if (contact.isEnabled) TextPrimary else TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${contact.relation})",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Text(
                text = contact.phone,
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = priorityText,
                color = priorityColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Switch(
                checked = contact.isEnabled,
                onCheckedChange = { onToggleEnabled(contact.id) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SafeGreen,
                    checkedTrackColor = SafeGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceElevated
                )
            )

            Row {
                IconButton(
                    onClick = { onEdit(contact) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onDelete(contact.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = EmergencyRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
