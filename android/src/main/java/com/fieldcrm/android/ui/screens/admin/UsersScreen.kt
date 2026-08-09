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
import com.fieldcrm.android.data.api.MobileBranchItem
import com.fieldcrm.android.data.api.MobileUserItem
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.fieldcrm.android.core.network.ApiResult

private sealed interface UsersPageState {
    data object Loading : UsersPageState
    data class Loaded(val users: List<MobileUserItem>) : UsersPageState
    data object Empty : UsersPageState
    data object PermissionDenied : UsersPageState
    data object SessionExpired : UsersPageState
    data class Error(val message: String) : UsersPageState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    onBackClick: (() -> Unit)? = null
) {
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pageState by remember { mutableStateOf<UsersPageState>(UsersPageState.Loading) }
    // Search text can contain user identifiers, so keep it out of saved instance state.
    var searchQuery by remember { mutableStateOf("") }
    var branches by remember { mutableStateOf<List<MobileBranchItem>>(emptyList()) }
    var selectedUserForDetail by remember { mutableStateOf<MobileUserItem?>(null) }
    var pendingDeactivation by remember { mutableStateOf<MobileUserItem?>(null) }
    var isDeactivating by remember { mutableStateOf(false) }

    // Create user dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var createEmail by remember { mutableStateOf("") }
    var createRole by remember { mutableStateOf("account_officer") }
    var createBranchId by remember { mutableStateOf("") }
    var createBranchName by remember { mutableStateOf("No Branch Assigned") }
    var branchLoadError by remember { mutableStateOf<String?>(null) }
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
                branches = result.data
                branchLoadError = null
            } else {
                branchLoadError = "Branches could not be loaded. You can still invite the user without a branch."
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
            title = { Text("Invite New User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (createError != null) {
                        Text(createError!!, color = FieldTheme.colors.statusDanger, style = FieldTheme.typography.body)
                    }
                    Text(
                        text = "Admin does not set passwords. The invited user will receive an email link to set their own secure credentials.",
                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                        color = FieldTheme.colors.purple400
                    )
                    if (branchLoadError != null) {
                        Text(
                            text = branchLoadError!!,
                            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                            color = FieldTheme.colors.statusWarning
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
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
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FieldTheme.colors.purple600,
                            unfocusedBorderColor = FieldTheme.colors.gray700
                        )
                    )
                    
                    // Branch Selector dropdown
                    var branchExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = createBranchName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Branch Assignment") },
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
                                onClick = {
                                    createBranchId = ""
                                    createBranchName = "No Branch Assigned"
                                    branchExpanded = false
                                }
                            )
                            branches.forEach { br ->
                                DropdownMenuItem(
                                    text = { Text(br.name) },
                                    onClick = { 
                                        createBranchId = br.id
                                        createBranchName = br.name
                                        branchExpanded = false 
                                    }
                                )
                            }
                        }
                    }

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
                    enabled = createName.isNotBlank() && createEmail.isNotBlank() && !isCreating,
                    onClick = {
                        isCreating = true
                        createError = null
                        scope.launch {
                            when (val result = api.inviteUser(
                                createName.trim(),
                                createEmail.trim(),
                                createRole,
                                createBranchId.ifBlank { null }
                            )) {
                                is ApiResult.Success -> {
                                    showCreateDialog = false
                                    createName = ""; createEmail = ""; createRole = "account_officer"
                                    createBranchId = ""; createBranchName = "No Branch Assigned"
                                    refreshUsers()
                                    snackbarHostState.showSnackbar(result.data.message)
                                }
                                is ApiResult.Error -> createError = result.detail
                                is ApiResult.NetworkError -> createError = result.message
                                ApiResult.Loading -> Unit
                            }
                            isCreating = false
                        }
                    }
                ) {
                    Text(if (isCreating) "Sending..." else "Send Invitation", color = FieldTheme.colors.purple600)
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
                    IconButton(onClick = { refreshUsers() }) {
                        Icon(
                            imageVector = FieldIcons.RefreshOutlined,
                            contentDescription = "Refresh users",
                            tint = FieldTheme.colors.gray400
                        )
                    }
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
                            text = "${filteredUsers.size} ${if (filteredUsers.size == 1) "USER" else "USERS"}",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.purple400
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            Text("ALL USERS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
            Spacer(modifier = Modifier.height(8.dp))

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
                        modifier = Modifier.fillMaxWidth().weight(1f)
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
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Manage", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
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
            userBranchId = branches.find { it.name == user.branch_name }?.id
        }

        var isUpdatingUser by remember { mutableStateOf(false) }
        var updateError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isUpdatingUser) selectedUserForDetail = null },
            title = { Text("Manage User") },
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
                        val currentBranchName = branches.find { it.id == userBranchId }
                            ?.name ?: "No Branch Assigned"
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
                                DropdownMenuItem(
                                    text = { Text(branch.name) },
                                    onClick = { userBranchId = branch.id; branchExpanded = false }
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
                            onClick = { pendingDeactivation = user },
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

    pendingDeactivation?.let { user ->
        AlertDialog(
            onDismissRequest = { if (!isDeactivating) pendingDeactivation = null },
            title = { Text("Deactivate user?") },
            text = {
                Text(
                    "${user.full_name} will lose access to FieldCRM. Their existing records and audit history will be retained."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeactivating,
                    onClick = {
                        isDeactivating = true
                        scope.launch {
                            when (val result = api.deactivateUser(user.id)) {
                                is ApiResult.Success -> {
                                    pendingDeactivation = null
                                    selectedUserForDetail = null
                                    refreshUsers()
                                    snackbarHostState.showSnackbar("${user.full_name} was deactivated.")
                                }
                                is ApiResult.Error -> snackbarHostState.showSnackbar(result.detail)
                                is ApiResult.NetworkError -> snackbarHostState.showSnackbar(result.message)
                                ApiResult.Loading -> Unit
                            }
                            isDeactivating = false
                        }
                    }
                ) {
                    Text(if (isDeactivating) "Deactivating..." else "Deactivate", color = FieldTheme.colors.statusDanger)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeactivating,
                    onClick = { pendingDeactivation = null }
                ) { Text("Cancel") }
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
