package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LendingContact
import com.example.data.model.RecordKind
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

private val ChipShape = RoundedCornerShape(12.dp)

private val RecordKind.icon: ImageVector
    get() = when (this) {
        RecordKind.EXPENSE -> Icons.Default.ArrowUpward
        RecordKind.INCOME -> Icons.Default.ArrowDownward
        RecordKind.REFUND -> Icons.Default.Replay
        RecordKind.LENT -> Icons.Default.CallMade
        RecordKind.REPAID -> Icons.Default.CallReceived
        RecordKind.TRANSFER -> Icons.Default.SwapHoriz
    }

@Composable
private fun RecordKind.accent(): Color = when {
    this == RecordKind.TRANSFER -> MaterialTheme.colorScheme.outline
    isLending -> MaterialTheme.colorScheme.primary
    isInflow -> IncomeGreen
    else -> ExpenseRed
}

/** One "What is this?" choice in the add / edit record dialog. */
@Composable
fun RecordKindChip(kind: RecordKind, selected: Boolean, onClick: () -> Unit) {
    val accent = kind.accent()
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(if (selected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color.LightGray.copy(alpha = 0.3f),
                shape = ChipShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("tx_kind_${kind.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(kind.icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = kind.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** One line under the chips explaining how the choice affects the totals, for the non-obvious kinds. */
fun recordKindHint(kind: RecordKind): String? = when (kind) {
    RecordKind.REFUND -> "Money back for a purchase. It lowers that category's spending instead of counting as income."
    RecordKind.LENT -> "Goes to the lending ledger. Not counted as spending."
    RecordKind.REPAID -> "Money a friend paid back. Not counted as income."
    RecordKind.TRANSFER -> "Between your own accounts. Left out of spending and income."
    RecordKind.EXPENSE, RecordKind.INCOME -> null
}

/** Picks who a Lent / Got back record is with: an existing lending contact or a new name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LendingContactPicker(
    contacts: List<LendingContact>,
    selectedContactId: Int?,
    addingNew: Boolean,
    newName: String,
    onSelect: (Int) -> Unit,
    onStartNew: () -> Unit,
    onNewNameChange: (String) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Who with?",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().testTag("lending_contact_picker"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            contacts.sortedBy { it.name.lowercase() }.forEach { contact ->
                val selected = !addingNew && contact.id == selectedContactId
                Text(
                    text = contact.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(ChipShape)
                        .background(if (selected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(if (selected) 2.dp else 1.dp, if (selected) accent else Color.LightGray.copy(alpha = 0.3f), ChipShape)
                        .clickable { onSelect(contact.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .clip(ChipShape)
                    .background(if (addingNew) accent.copy(alpha = 0.15f) else Color.Transparent)
                    .border(BorderStroke(1.dp, accent.copy(alpha = 0.5f)), ChipShape)
                    .clickable(onClick = onStartNew)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("lending_new_contact_chip"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("New person", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }
        if (addingNew) {
            OutlinedTextField(
                value = newName,
                onValueChange = onNewNameChange,
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("lending_new_contact_name")
            )
        }
    }
}
