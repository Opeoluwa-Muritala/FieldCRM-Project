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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme

private data class UserItem(
    val name: String,
    val role: String,
    val email: String,
    val userId: String
)

private val placeholderUsers = listOf(
    UserItem("Samuel Okeke", "Loan Officer", "s.okeke@fieldcrm.com", "usr_001"),
    UserItem("Grace Nwosu", "Loan Officer", "g.nwosu@fieldcrm.com", "usr_002"),
    UserItem("Chidi Okafor", "Branch Manager", "c.okafor@fieldcrm.com", "usr_003"),
    UserItem("Aisha Mohammed", "Credit Officer", "a.mohammed@fieldcrm.com", "usr_004"),
    UserItem("Kemi Adeleke", "Auditor", "k.adeleke@fieldcrm.com", "usr_005"),
    UserItem("Tunde Bakare", "System Admin", "t.bakare@fieldcrm.com", "usr_006")
)

@Composable
fun UsersScreen(
    onBackClick: () -> Unit,
    onViewUser: (String) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(placeholderUsers, searchQuery) {
        if (searchQuery.isBlank()) placeholderUsers
        else placeholderUsers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true)
        }
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

            FieldTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search Users",
                placeholder = "Name, email, or role...",
                trailingIcon = {
                    Icon(
                        imageVector = FieldIcons.SearchOutlined,
                        contentDescription = "Search",
                        tint = FieldTheme.colors.gray500
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
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
            } else if (filteredUsers.isEmpty()) {
                EmptyState(text = "No users match your search.")
            } else {
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
                                        text = user.name,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = user.email,
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray400
                                    )
                                }
                                RoleBadge(role = user.role)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Users Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewUsersScreen() {
    FieldCRMTheme {
        UsersScreen(onBackClick = {})
    }
}
