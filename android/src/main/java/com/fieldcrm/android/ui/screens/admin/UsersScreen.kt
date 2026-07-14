package com.fieldcrm.android.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.data.api.MobileUserItem
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    onBackClick: () -> Unit,
    onViewUser: (String) -> Unit = {}
) {
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<MobileUserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Create user dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var createEmail by remember { mutableStateOf("") }
    var createRole by remember { mutableStateOf("loan_officer") }
    var createPassword by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    val roles = listOf(
        "loan_officer" to "Loan Officer",
        "branch_manager" to "Branch Manager",
        "auditor" to "Auditor",
        "crm" to "CRM Officer",
        "committee" to "Committee Member",
        "ed" to "Executive Director",
        "md" to "Managing Director",
        "system_admin" to "System Admin",
    )

    LaunchedEffect(Unit) {
        isLoading = true
        loadError = null
        val result = api.listUsers()
        if (result.isEmpty() && isLoading) {
            loadError = "No users found or unable to load."
        }
        users = result
        isLoading = false
    }

    fun refreshUsers() {
        scope.launch {
            isLoading = true
            loadError = null
            users = api.listUsers()
            isLoading = false
        }
    }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.full_name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.display_role.contains(searchQuery, ignoreCase = true)
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreating) showCreateDialog = false },
            title = { Text("Create New User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (createError != null) {
                        Text(createError!!, color = FieldTheme.colors.statusDanger, style = FieldTheme.typography.body)
                    }
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FieldTheme.colors.purple600,
                            unfocusedBorderColor = FieldTheme.colors.gray700
                        )
                    )
                    OutlinedTextField(
                        value = createEmail,
                        onValueChange = { createEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FieldTheme.colors.purple600,
                            unfocusedBorderColor = FieldTheme.colors.gray700
                        )
                    )
                    OutlinedTextField(
                        value = createPassword,
                        onValueChange = { createPassword = it },
                        label = { Text("Password (min 8 chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FieldTheme.colors.purple600,
                            unfocusedBorderColor = FieldTheme.colors.gray700
                        )
                    )
                    var roleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = roles.find { it.first == createRole }?.second ?: createRole,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FieldTheme.colors.purple600,
                                unfocusedBorderColor = FieldTheme.colors.gray700
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            roles.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { createRole = value; roleExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = createName.isNotBlank() && createEmail.isNotBlank() && createPassword.length >= 8 && !isCreating,
                    onClick = {
                        isCreating = true
                        createError = null
                        scope.launch {
                            val ok = api.createUser(createName.trim(), createEmail.trim(), createRole, createPassword)
                            if (ok) {
                                showCreateDialog = false
                                createName = ""; createEmail = ""; createPassword = ""; createRole = "loan_officer"
                                refreshUsers()
                            } else {
                                createError = "Failed to create user. Email may already exist."
                            }
                            isCreating = false
                        }
                    }
                ) {
                    Text(if (isCreating) "Creating…" else "Create", color = FieldTheme.colors.purple600)
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isCreating) showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "User Management",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .background(
                                FieldTheme.colors.gray800,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .border(
                                0.5.dp,
                                FieldTheme.colors.gray700,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${filteredUsers.size} USERS",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.purple400
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search Users",
                    placeholder = "Name, email, or role...",
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        Icon(
                            imageVector = FieldIcons.SearchOutlined,
                            contentDescription = "Search",
                            tint = FieldTheme.colors.gray500
                        )
                    }
                )
                PrimaryButton(
                    text = "+ Add",
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.wrapContentWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(6) {
                            FieldCard(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        LoadingSkeleton(height = 16.dp, width = 140.dp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LoadingSkeleton(height = 12.dp, width = 200.dp)
                                    }
                                    LoadingSkeleton(height = 20.dp, width = 80.dp, cornerRadius = 4.dp)
                                }
                            }
                        }
                    }
                }
                filteredUsers.isEmpty() -> EmptyState(text = if (searchQuery.isNotBlank()) "No users match your search." else "No users found.")
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredUsers) { user ->
                            FieldCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.full_name,
                                            style = FieldTheme.typography.bodyStrong,
                                            color = FieldTheme.colors.gray100
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = user.email,
                                            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.gray400
                                        )
                                        if (!user.active) {
                                            Text(
                                                text = "INACTIVE",
                                                style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                                color = FieldTheme.colors.statusDanger
                                            )
                                        }
                                    }
                                    RoleBadge(role = user.display_role.ifBlank { user.role.replace("_", " ").replaceFirstChar { it.uppercaseChar() } })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
