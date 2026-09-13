package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.model.LendingContact
import com.example.data.model.LendingEntry
import com.example.formatInRupee
import com.example.parseHexColor
import com.example.ui.theme.liquidGlass
import com.example.ui.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val LENDING_COLORS = listOf(
    "#4D96FF", "#6BCB77", "#FF6B6B", "#FFD93D",
    "#9D4EDD", "#FF9F45", "#00B4D8", "#F72585",
    "#48CAE4", "#52B788", "#E76F51", "#7209B7"
)

val LENDING_ICONS = listOf(
    "person" to Icons.Default.Person,
    "group" to Icons.Default.Groups,
    "business" to Icons.Default.Business,
    "handshake" to Icons.Default.Handshake,
    "home" to Icons.Default.Home,
    "shopping" to Icons.Default.ShoppingBag,
    "work" to Icons.Default.Work,
    "star" to Icons.Default.Star
)

fun getLendingIconVector(iconName: String?): ImageVector {
    return when (iconName?.lowercase()) {
        "group", "groups" -> Icons.Default.Groups
        "business" -> Icons.Default.Business
        "handshake" -> Icons.Default.Handshake
        "home" -> Icons.Default.Home
        "shopping" -> Icons.Default.ShoppingBag
        "work" -> Icons.Default.Work
        "star" -> Icons.Default.Star
        else -> Icons.Default.Person
    }
}

// -------------------------------------------------------------
// Home Dashboard Compact Lending Summary Card
// -------------------------------------------------------------
@Composable
fun LendingSummaryCard(
    totalOutstanding: Double,
    currencySymbol: String,
    contactsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, elevation = 4.dp)
            .clickable { onClick() }
            .testTag("lending_summary_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = "Lending Ledger",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Lending Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $contactsCount ${if (contactsCount == 1) "contact" else "contacts"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (totalOutstanding >= 0.0) "Owed to you" else "You owe",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatInRupee(kotlin.math.abs(totalOutstanding), currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Lending Ledger",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// Lending List Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendingListScreen(
    viewModel: CategoryViewModel,
    navController: NavController,
    currencySymbol: String,
    onBack: () -> Unit
) {
    val contacts by viewModel.lendingContacts.collectAsStateWithLifecycle()
    val entries by viewModel.lendingEntries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showAddContactDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<LendingContact?>(null) }
    var contactToEdit by remember { mutableStateOf<LendingContact?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Calculate total outstanding across all contacts
    val totalOutstanding = remember(entries) {
        entries.fold(0.0) { acc, entry ->
            if (entry.direction == "LENT") acc + entry.amount else acc - entry.amount
        }
    }

    val totalLentAll = remember(entries) {
        entries.filter { it.direction == "LENT" }.sumOf { it.amount }
    }

    val totalRepaidAll = remember(entries) {
        entries.filter { it.direction == "REPAID" }.sumOf { it.amount }
    }

    // Filter contacts based on search query
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            (it.note != null && it.note.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lending Ledger",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("lending_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showAddContactDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_contact_fab"),
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact") },
                text = { Text("Add Contact", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Hero Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lending_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Owed to you",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatInRupee(totalOutstanding, currencySymbol),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalOutstanding > 0.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${contacts.size} Contact${if (contacts.size == 1) "" else "s"}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mini Stats Row: Total Lent vs Repaid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.06f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Total Lent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatInRupee(totalLentAll, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.06f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Total Repaid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatInRupee(totalRepaidAll, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar (if more than 2 contacts)
            if (contacts.size > 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lending_search_input"),
                    placeholder = { Text("Search by name or note...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Contact List or Empty State
            if (contacts.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .testTag("lending_empty_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = "No contacts",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "No Lending Contacts Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Keep track of money lent to friends, family, or colleagues in a separate ledger without affecting your regular budget or expense analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { showAddContactDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_add_contact_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Contact", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No contacts matching '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("lending_contacts_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        val contactEntries = entries.filter { it.contactId == contact.id }
                        val outstanding = contactEntries.fold(0.0) { acc, e ->
                            if (e.direction == "LENT") acc + e.amount else acc - e.amount
                        }

                        LendingContactRow(
                            contact = contact,
                            outstanding = outstanding,
                            entriesCount = contactEntries.size,
                            currencySymbol = currencySymbol,
                            onClick = {
                                navController.navigate("lending_contact/${contact.id}")
                            },
                            onEdit = { contactToEdit = contact },
                            onDelete = { contactToDelete = contact }
                        )
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AddEditContactDialog(
            contact = null,
            currencySymbol = currencySymbol,
            onDismiss = { showAddContactDialog = false },
            onSave = { name, note, colorHex, iconName, initialAmount, initialDesc ->
                viewModel.addLendingContact(
                    name = name,
                    note = note,
                    colorHex = colorHex,
                    iconName = iconName,
                    initialLentAmount = initialAmount,
                    initialDescription = initialDesc
                )
                showAddContactDialog = false
                Toast.makeText(context, "Contact added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Contact Dialog
    contactToEdit?.let { contact ->
        AddEditContactDialog(
            contact = contact,
            currencySymbol = currencySymbol,
            onDismiss = { contactToEdit = null },
            onSave = { name, note, colorHex, iconName, _, _ ->
                viewModel.updateLendingContact(
                    contact.copy(
                        name = name,
                        note = note,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
                contactToEdit = null
                Toast.makeText(context, "Contact updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Contact Confirmation Dialog
    contactToDelete?.let { contact ->
        val contactEntriesCount = entries.count { it.contactId == contact.id }
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = {
                Text(
                    text = "Delete Contact?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${contact.name}'?\n\nThis will permanently delete:\n• $contactEntriesCount associated lending record${if (contactEntriesCount == 1) "" else "s"}\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.deleteLendingContact(contact)
                        contactToDelete = null
                        Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_contact_btn")
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { contactToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_contact_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Contact Row Item
// -------------------------------------------------------------
@Composable
fun LendingContactRow(
    contact: LendingContact,
    outstanding: Double,
    entriesCount: Int,
    currencySymbol: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val contactColor = contact.colorHex?.let { parseHexColor(it) }
        ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("lending_contact_row_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Name + Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(contactColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getLendingIconVector(contact.iconName),
                        contentDescription = contact.name,
                        tint = contactColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!contact.note.isNullOrBlank()) {
                        Text(
                            text = contact.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "$entriesCount record${if (entriesCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Right: Outstanding Balance Badge & Chevron
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    if (outstanding > 0.0) {
                        Text(
                            text = "Owed to you",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatInRupee(outstanding, currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32)
                        )
                    } else if (outstanding == 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Settled",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    } else {
                        // Negative balance means contact overpaid
                        Text(
                            text = "You owe",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatInRupee(-outstanding, currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Contact Detail Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Int,
    viewModel: CategoryViewModel,
    navController: NavController,
    currencySymbol: String,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val contacts by viewModel.lendingContacts.collectAsStateWithLifecycle()
    val allEntries by viewModel.lendingEntries.collectAsStateWithLifecycle()
    val contact = contacts.find { it.id == contactId }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showLendDialog by remember { mutableStateOf(false) }
    var showRepayDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<LendingEntry?>(null) }
    var showDeleteContactConfirm by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }

    if (contact == null) {
        // Contact was deleted or not found
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    val contactEntries = remember(allEntries, contactId) {
        allEntries.filter { it.contactId == contactId }.sortedByDescending { it.timestamp }
    }

    // Outstanding = sum(LENT) - sum(REPAID)
    val outstanding = remember(contactEntries) {
        contactEntries.fold(0.0) { acc, entry ->
            if (entry.direction == "LENT") acc + entry.amount else acc - entry.amount
        }
    }

    val totalLent = remember(contactEntries) {
        contactEntries.filter { it.direction == "LENT" }.sumOf { it.amount }
    }

    val totalRepaid = remember(contactEntries) {
        contactEntries.filter { it.direction == "REPAID" }.sumOf { it.amount }
    }

    val contactColor = contact.colorHex?.let { parseHexColor(it) }
        ?: MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditContactDialog = true },
                        modifier = Modifier.testTag("detail_edit_contact_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Contact")
                    }
                    IconButton(
                        onClick = { showDeleteContactConfirm = true },
                        modifier = Modifier.testTag("detail_delete_contact_btn")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Contact",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Outstanding Hero Card at the top (with "Owed to you" label)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_outstanding_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(contactColor.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getLendingIconVector(contact.iconName),
                                    contentDescription = contact.name,
                                    tint = contactColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Owed to you",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = formatInRupee(outstanding, currencySymbol),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (outstanding > 0.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (outstanding == 0.0 && contactEntries.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Fully Repaid",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    if (!contact.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Note: ${contact.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mini Stats: Total Lent and Total Repaid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.06f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Total Lent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatInRupee(totalLent, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.06f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Total Repaid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatInRupee(totalRepaid, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            // Two Action Buttons: "Lend more" and "Record repayment"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showLendDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("lend_more_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Lend more",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lend More", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showRepayDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("record_repayment_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Record repayment",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Repayment", fontWeight = FontWeight.Bold)
                }
            }

            // Chronological List of Entries
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ledger History (${contactEntries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (contactEntries.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .testTag("detail_empty_entries_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "No entries",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No records for ${contact.name} yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use 'Lend More' to track money you give, and 'Repayment' when money is returned.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("contact_entries_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(contactEntries, key = { it.id }) { entry ->
                        LendingEntryRow(
                            entry = entry,
                            currencySymbol = currencySymbol,
                            onEdit = { entryToEdit = entry },
                            onDelete = {
                                scope.launch {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    val deletedEntry = entry
                                    viewModel.deleteLendingEntry(deletedEntry)
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Lending record deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.reinsertLendingEntry(deletedEntry)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Lend More
    if (showLendDialog) {
        AddEditLendingEntryDialog(
            entry = null,
            fixedDirection = "LENT",
            currencySymbol = currencySymbol,
            onDismiss = { showLendDialog = false },
            onSave = { amount, direction, description, timestamp ->
                viewModel.addLendingEntry(
                    contactId = contact.id,
                    amount = amount,
                    direction = direction,
                    description = description,
                    timestamp = timestamp
                )
                showLendDialog = false
                Toast.makeText(context, "Lending record added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Record Repayment
    if (showRepayDialog) {
        AddEditLendingEntryDialog(
            entry = null,
            fixedDirection = "REPAID",
            currencySymbol = currencySymbol,
            onDismiss = { showRepayDialog = false },
            onSave = { amount, direction, description, timestamp ->
                viewModel.addLendingEntry(
                    contactId = contact.id,
                    amount = amount,
                    direction = direction,
                    description = description,
                    timestamp = timestamp
                )
                showRepayDialog = false
                Toast.makeText(context, "Repayment recorded", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Edit Entry
    entryToEdit?.let { entry ->
        AddEditLendingEntryDialog(
            entry = entry,
            fixedDirection = null,
            currencySymbol = currencySymbol,
            onDismiss = { entryToEdit = null },
            onSave = { amount, direction, description, timestamp ->
                viewModel.updateLendingEntry(
                    entry.copy(
                        amount = amount,
                        direction = direction,
                        description = description,
                        timestamp = timestamp
                    )
                )
                entryToEdit = null
                Toast.makeText(context, "Record updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Edit Contact
    if (showEditContactDialog) {
        AddEditContactDialog(
            contact = contact,
            currencySymbol = currencySymbol,
            onDismiss = { showEditContactDialog = false },
            onSave = { name, note, colorHex, iconName, _, _ ->
                viewModel.updateLendingContact(
                    contact.copy(
                        name = name,
                        note = note,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
                showEditContactDialog = false
                Toast.makeText(context, "Contact updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Delete Contact Confirmation
    if (showDeleteContactConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteContactConfirm = false },
            title = {
                Text(
                    text = "Delete Contact?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${contact.name}'?\n\nThis will permanently delete ${contactEntries.size} associated lending records.\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.deleteLendingContact(contact)
                        showDeleteContactConfirm = false
                        Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_detail_contact_btn")
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteContactConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Entry Row Item
// -------------------------------------------------------------
@Composable
fun LendingEntryRow(
    entry: LendingEntry,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLent = entry.direction == "LENT"
    val badgeColor = if (isLent) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
    val badgeBg = if (isLent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else Color(0xFF2E7D32).copy(alpha = 0.12f)
    val dateStr = remember(entry.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lending_entry_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Direction indicator badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isLent) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = entry.direction,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLent) "LENT" else "REPAID",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (entry.description.isNotBlank()) entry.description else if (isLent) "Money lent" else "Repayment received",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Right: Amount + Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatInRupee(entry.amount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLent) MaterialTheme.colorScheme.onSurface else Color(0xFF2E7D32)
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_entry_btn_${entry.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_entry_btn_${entry.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Add / Edit Contact Dialog
// -------------------------------------------------------------
@Composable
fun AddEditContactDialog(
    contact: LendingContact?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, note: String?, colorHex: String?, iconName: String?, initialAmount: Double?, initialDesc: String?) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var note by remember { mutableStateOf(contact?.note ?: "") }
    var selectedColor by remember { mutableStateOf(contact?.colorHex ?: LENDING_COLORS.first()) }
    var selectedIcon by remember { mutableStateOf(contact?.iconName ?: "person") }
    var initialAmountText by remember { mutableStateOf("") }
    var initialDesc by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_contact_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (contact == null) "Add Lending Contact" else "Edit Contact",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (hasError && it.isNotBlank()) hasError = false
                    },
                    label = { Text("Contact Name *") },
                    placeholder = { Text("e.g. Alice, Bob, Organization") },
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text("Name is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Tag (Optional)") },
                    placeholder = { Text("e.g. Colleague, Roommate, Trip split") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Color Selection
                Text(
                    text = "Color Badge",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LENDING_COLORS.forEach { hex ->
                        val color = parseHexColor(hex)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Icon Selection
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LENDING_ICONS.forEach { (iconKey, iconVec) ->
                        val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedIcon = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = iconKey,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Initial Loan Option (only for new contacts)
                if (contact == null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Initial Loan (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = initialAmountText,
                        onValueChange = { initialAmountText = it },
                        label = { Text("Amount Lent ($currencySymbol)") },
                        placeholder = { Text("e.g. 500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_initial_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (initialAmountText.isNotBlank()) {
                        OutlinedTextField(
                            value = initialDesc,
                            onValueChange = { initialDesc = it },
                            label = { Text("Loan Description") },
                            placeholder = { Text("e.g. Dinner, Emergency cash") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contact_initial_desc_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_contact_btn")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                hasError = true
                                return@Button
                            }
                            val initAmt = initialAmountText.toDoubleOrNull()?.takeIf { it > 0 }
                            onSave(
                                name.trim(),
                                note.trim().ifBlank { null },
                                selectedColor,
                                selectedIcon,
                                initAmt,
                                initialDesc.trim().ifBlank { null }
                            )
                        },
                        modifier = Modifier.testTag("save_contact_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Contact", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Add / Edit Lending Entry Dialog (Lend More / Record Repayment)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLendingEntryDialog(
    entry: LendingEntry?,
    fixedDirection: String?, // "LENT", "REPAID", or null if editable
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, direction: String, description: String, timestamp: Long) -> Unit
) {
    var amountText by remember {
        mutableStateOf(entry?.let { if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString() } ?: "")
    }
    var direction by remember { mutableStateOf(fixedDirection ?: entry?.direction ?: "LENT") }
    var description by remember { mutableStateOf(entry?.description ?: "") }
    var timestamp by remember { mutableStateOf(entry?.timestamp ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val isLent = direction == "LENT"
    val themeColor = if (isLent) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
    val title = when {
        entry != null -> "Edit Record"
        isLent -> "Lend More Money"
        else -> "Record Repayment"
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        timestamp = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_lending_entry_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )

                // Direction Toggle (only if fixedDirection is null, i.e. when editing)
                if (fixedDirection == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = direction == "LENT",
                            onClick = { direction = "LENT" },
                            label = { Text("Lent", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Lent", modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = direction == "REPAID",
                            onClick = { direction = "REPAID" },
                            label = { Text("Repaid", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = "Repaid", modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (amountError && it.toDoubleOrNull() != null && it.toDouble() > 0) {
                            amountError = false
                        }
                    },
                    label = { Text("Amount ($currencySymbol) *") },
                    placeholder = { Text("e.g. 500") },
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Enter a valid positive amount", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text(if (isLent) "e.g. Dinner split, Gas money" else "e.g. Partial repayment, UPI transfer") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_description_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Date Picker trigger
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_date_picker_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(timestamp)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_entry_btn")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0) {
                                amountError = true
                                return@Button
                            }
                            onSave(
                                parsedAmount,
                                direction,
                                description.trim().ifBlank { if (isLent) "Money lent" else "Repayment" },
                                timestamp
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        modifier = Modifier.testTag("save_entry_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (entry != null) "Update Record" else if (isLent) "Record Loan" else "Record Repayment",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
