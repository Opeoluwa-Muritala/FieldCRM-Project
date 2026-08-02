package com.fieldcrm.android.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.fieldcrm.android.core.network.ApiResult
import kotlinx.serialization.json.*

private sealed interface UsersPageState {
    data object Loading : UsersPageState
    data class Loaded(val users: List<MobileUserItem>, val cached: Boolean = false) : UsersPageState
    data object Empty : UsersPageState
    data object PermissionDenied : UsersPageState
    data object SessionExpired : UsersPageState
    data class Error(val message: String) : UsersPageState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    onBackClick: (() -> Unit)? = null,
    onViewUser: (String) -> Unit = {}
) {
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()

    var pageState by remember { mutableStateOf<UsersPageState>(UsersPageState.Loading) }
    var searchQuery by remember { mutableStateOf("") }
    var branches by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var selectedUserForDetail by remember { mutableStateOf<MobileUserItem?>(null) }

    // Create user dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var createEmail by remember { mutableStateOf("") }
    var createRole by remember { mutableStateOf("loan_officer") }
    var createPassword by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    val roles = listOf(
        "account_officer" to "Relationship Officer",
        "branch_manager" to "Team Lead",
        "branch_supervisor" to "Supervisor",
        "credit_analyst" to "Credit Analyst",
        "auditor" to "Audit",
        "crm" to "CRM Officer",
        "head_crm" to "Head CRM",
        "legal" to "Legal",
        "ed" to "Executive Director",
        "md" to "Managing Director",
        "system_admin" to "System Admin",
    )

    LaunchedEffect(Unit) {
        scope.launch {
            val result = api.getBranches()
            if (result is ApiResult.Success) {
                branches = (result.data as? JsonObject)?.get("items")?.jsonArray?.mapNotNull { it as? JsonObject }.orEmpty()
            }
        }
        pageState = when (val result = api.listUsers()) {
            is ApiResult.Success -> if (result.data.isEmpty()) UsersPageState.Empty else UsersPageState.Loaded(result.data)
            is ApiResult.Error -> when (result.statusCode) {
                401 -> UsersPageState.SessionExpired
                403 -> UsersPageState.PermissionDenied
                else -> UsersPageState.Error(result.detail)
            }
            is ApiResult.NetworkError -> UsersPageState.Error(result.message)
            ApiResult.Loading -> UsersPageState.Loading
        }
    }

    fun refreshUsers() {
        scope.launch {
            pageState = UsersPageState.Loading
            pageState = when (val result = api.listUsers()) {
                is ApiResult.Success -> if (result.data.isEmpty()) UsersPageState.Empty else UsersPageState.Loaded(result.data)
                is ApiResult.Error -> when (result.statusCode) {
                    401 -> UsersPageState.SessionExpired
                    403 -> UsersPageState.PermissionDenied
                    else -> UsersPageState.Error(result.detail)
                }
                is ApiResult.NetworkError -> UsersPageState.Error(result.message)
                ApiResult.Loading -> UsersPageState.Loading
            }
        }
    }

    val users = (pageState as? UsersPageState.Loaded)?.users.orEmpty()
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
                navigationIcon = if (onBackClick != null) {
                    {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = FieldIcons.ArrowBackOutlined,
                                contentDescription = "Back",
                                tint = FieldTheme.colors.gray400
                            )
                        }
                    }
                } else null,
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
                pageState is UsersPageState.Loading -> {
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
                pageState is UsersPageState.PermissionDenied -> EmptyState(text = "You do not have permission to manage users.")
                pageState is UsersPageState.SessionExpired -> EmptyState(text = "Your session has expired. Sign in again.")
                pageState is UsersPageState.Error -> EmptyState(text = (pageState as UsersPageState.Error).message)
                pageState is UsersPageState.Empty -> EmptyState(text = "No users have been created for this organization.")
                filteredUsers.isEmpty() -> EmptyState(text = "No users match your search.")
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredUsers) { user ->
                            FieldCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedUserForDetail = user }
                            ) {
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
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = user.branch_name?.takeIf { it.isNotBlank() } ?: "No branch assigned",
                                            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.purple400
                                        )
                                        if (!user.active) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "INACTIVE",
                                                style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                                color = FieldTheme.colors.statusDanger
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
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

    if (selectedUserForDetail != null) {
        val user = selectedUserForDetail!!
        var userRole by remember(user) { mutableStateOf(user.role) }
        var userBranchId by remember(user) { mutableStateOf<String?>(null) }
        
        LaunchedEffect(user, branches) {
            val matchedBranch = branches.find { (it["name"] as? JsonPrimitive)?.contentOrNull == user.branch_name }
            userBranchId = matchedBranch?.get("id")?.jsonPrimitive?.contentOrNull
        }

        var isUpdatingUser by remember { mutableStateOf(false) }
        var updateError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isUpdatingUser) selectedUserForDetail = null },
            title = { Text("User Profile Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (updateError != null) {
                        Text(updateError!!, color = FieldTheme.colors.statusDanger, style = FieldTheme.typography.body)
                    }
                    
                    DetailRow(label = "Full Name", value = user.full_name)
                    DetailRow(label = "Email Address", value = user.email)
                    DetailRow(label = "Status", value = if (user.active) "Active" else "Inactive")
                    DetailRow(label = "Last Activity", value = user.last_activity_at?.replace('T', ' ')?.take(19) ?: "Never")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("MANAGE ROLE & BRANCH", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    
                    var roleExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = roles.find { it.first == userRole }?.second ?: userRole,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Role") },
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
                                    onClick = { userRole = value; roleExpanded = false }
                                )
                            }
                        }
                    }

                    var branchExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = it }
                    ) {
                        val currentBranchName = branches.find { it["id"]?.jsonPrimitive?.contentOrNull == userBranchId }
                            ?.get("name")?.jsonPrimitive?.contentOrNull ?: "No Branch Assigned"
                        OutlinedTextField(
                            value = currentBranchName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Branch") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FieldTheme.colors.purple600,
                                unfocusedBorderColor = FieldTheme.colors.gray700
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = branchExpanded,
                            onDismissRequest = { branchExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Branch Assigned") },
                                onClick = { userBranchId = null; branchExpanded = false }
                            )
                            branches.forEach { branch ->
                                val id = branch["id"]?.jsonPrimitive?.contentOrNull ?: ""
                                val name = branch["name"]?.jsonPrimitive?.contentOrNull ?: ""
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = { userBranchId = id; branchExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user.active) {
                        TextButton(
                            onClick = {
                                isUpdatingUser = true
                                scope.launch {
                                    if (api.deactivateUser(user.id) is ApiResult.Success) {
                                        selectedUserForDetail = null
                                        refreshUsers()
                                    } else {
                                        updateError = "Failed to deactivate user."
                                    }
                                    isUpdatingUser = false
                                }
                            },
                            enabled = !isUpdatingUser
                        ) {
                            Text("Deactivate", color = FieldTheme.colors.statusDanger)
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            isUpdatingUser = true
                            scope.launch {
                                val res = api.updateUserRole(user.id, userRole, userBranchId)
                                if (res is ApiResult.Success) {
                                    selectedUserForDetail = null
                                    refreshUsers()
                                } else {
                                    updateError = "Failed to update user."
                                }
                                isUpdatingUser = false
                            }
                        },
                        enabled = !isUpdatingUser
                    ) {
                        Text("Save", color = FieldTheme.colors.purple400)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedUserForDetail = null },
                    enabled = !isUpdatingUser
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = FieldTheme.typography.body, color = FieldTheme.colors.gray500)
        Text(value, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
    }
}
