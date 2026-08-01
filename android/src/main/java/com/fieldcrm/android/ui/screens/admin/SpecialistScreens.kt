package com.fieldcrm.android.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.core.network.ApiResult
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTopAppBar
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.koin.compose.koinInject

private fun JsonElement.objectItems(): List<JsonObject> =
    (this as? JsonObject)?.get("items")?.jsonArray?.mapNotNull { it as? JsonObject }.orEmpty()

private fun JsonObject.text(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

@Composable
private fun SpecialistScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(topBar = { FieldTopAppBar(title = title, navigationIcon = {
        TextButton(onClick = onBack) { Text("Back") }
    }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun LegalWorkspaceScreen(
    onBack: () -> Unit,
    onOpenApplication: (String) -> Unit
) {
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            when (val result = api.getLegalQueue()) {
                is ApiResult.Success -> rows = result.data.objectItems()
                is ApiResult.Error -> error = result.detail
                is ApiResult.NetworkError -> error = result.message
                ApiResult.Loading -> Unit
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    SpecialistScaffold("Legal Queue", onBack) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = FieldTheme.colors.statusDanger) }
        if (!loading && rows.isEmpty()) Text("No applications awaiting legal review.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rows) { row ->
                FieldCard {
                    Text(row.text("applicant_name").ifBlank { "Applicant" }, style = FieldTheme.typography.title)
                    Text(row.text("ref_no"), color = FieldTheme.colors.gray400)
                    Text(row.text("stage").replace("_", " "), color = FieldTheme.colors.gray400)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        text = "Open valuation",
                        onClick = { onOpenApplication(row.text("id")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ValuationEditorScreen(applicationId: String, onBack: () -> Unit) {
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var values by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var valuer by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(applicationId) {
        when (val result = api.getValuation(applicationId)) {
            is ApiResult.Success -> {
                items = result.data.objectItems()
                values = items.associate { it.text("id") to it.text("appraised_value") }
            }
            is ApiResult.Error -> message = result.detail
            is ApiResult.NetworkError -> message = result.message
            ApiResult.Loading -> Unit
        }
    }

    SpecialistScaffold("Collateral Valuation", onBack) {
        message?.let { Text(it, color = FieldTheme.colors.statusDanger) }
        OutlinedTextField(valuer, { valuer = it }, label = { Text("Valuer name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(license, { license = it }, label = { Text("Licence number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(date, { date = it }, label = { Text("Valuation date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item ->
                val id = item.text("id")
                FieldCard {
                    Text(item.text("item_name").ifBlank { "Pledged item" }, style = FieldTheme.typography.title)
                    Text(item.text("description"), color = FieldTheme.colors.gray400)
                    OutlinedTextField(
                        value = values[id].orEmpty(),
                        onValueChange = { values = values + (id to it) },
                        label = { Text("Appraised value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        PrimaryButton(
            text = if (saving) "Saving…" else "Save valuation",
            enabled = !saving && items.isNotEmpty(),
            onClick = {
                saving = true
                scope.launch {
                    val payload = buildJsonObject {
                        putJsonArray("items") {
                            items.forEach { item ->
                                addJsonObject {
                                    put("item_id", item.text("id"))
                                    put("appraised_value", values[item.text("id")]?.toDoubleOrNull() ?: 0.0)
                                    if (valuer.isNotBlank()) put("valuer_name", valuer)
                                    if (license.isNotBlank()) put("valuer_license_no", license)
                                    if (date.isNotBlank()) put("valuation_date", date)
                                }
                            }
                        }
                    }
                    when (val result = api.updateValuation(applicationId, payload)) {
                        is ApiResult.Success -> message = "Valuation saved."
                        is ApiResult.Error -> message = result.detail
                        is ApiResult.NetworkError -> message = result.message
                        ApiResult.Loading -> Unit
                    }
                    saving = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MccWorkspaceScreen(onBack: () -> Unit, canManage: Boolean = true) {
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()
    var dossiers by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JsonObject?>(null) }
    var mccDetail by remember { mutableStateOf<JsonObject?>(null) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            when (val result = api.getMcc()) {
                is ApiResult.Success -> dossiers = result.data.objectItems()
                is ApiResult.Error -> error = result.detail
                is ApiResult.NetworkError -> error = result.message
                ApiResult.Loading -> Unit
            }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    LaunchedEffect(selected) {
        mccDetail = null
        val sel = selected
        if (sel != null) {
            scope.launch {
                when (val result = api.getMccApplication(sel.text("id"))) {
                    is ApiResult.Success -> {
                        mccDetail = result.data as? JsonObject
                    }
                    else -> Unit
                }
            }
        }
    }

    SpecialistScaffold("Management Credit Committee", onBack) {
        error?.let { Text(it, color = FieldTheme.colors.statusDanger) }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(dossiers) { dossier ->
                FieldCard {
                    Text(dossier.text("applicant_name"), style = FieldTheme.typography.title)
                    Text("${dossier.text("ref_no")} · ${dossier.text("stage")}", color = FieldTheme.colors.gray400)
                    PrimaryButton(
                        if (canManage) "Review / vote" else "View dossier",
                        { selected = dossier },
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    selected?.let { dossier ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("MCC recommendation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(dossier.text("applicant_name"), style = FieldTheme.typography.title)
                    Text("${dossier.text("ref_no")} · ${dossier.text("stage")}")
                    Text("Requested Amount: " + dossier.text("amount").ifBlank { "Amount unavailable" })
                    
                    Text("MCC Votes Cast:", style = FieldTheme.typography.label, color = FieldTheme.colors.gray400)
                    val votesArray = mccDetail?.get("votes")?.jsonArray
                    if (votesArray != null && votesArray.isNotEmpty()) {
                        votesArray.forEach { voteEl ->
                            val v = voteEl.jsonObject
                            val name = v["member_name"]?.jsonPrimitive?.content ?: ""
                            val amt = v["recommended_amount"]?.jsonPrimitive?.content ?: "0"
                            val note = v["notes"]?.jsonPrimitive?.content ?: ""
                            Text("· $name: NGN $amt (Notes: $note)", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                        }
                    } else {
                        Text("No votes cast yet.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray500)
                    }

                    if (canManage) {
                        OutlinedTextField(amount, { amount = it }, label = { Text("Recommended/final amount") })
                        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
                    } else {
                        Text("Relationship Officers have read-only MCC access.", color = FieldTheme.colors.gray400)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!canManage) {
                        selected = null
                    } else scope.launch {
                        when (val result = api.submitMccVote(
                            dossier.text("id"), amount.toDoubleOrNull() ?: 0.0, notes
                        )) {
                            is ApiResult.Success -> { selected = null; refresh() }
                            is ApiResult.Error -> error = result.detail
                            is ApiResult.NetworkError -> error = result.message
                            ApiResult.Loading -> Unit
                        }
                    }
                }) { Text(if (canManage) "Submit vote" else "Close") }
            },
            dismissButton = {
                if (canManage) TextButton(onClick = {
                    scope.launch {
                        when (val result = api.finalizeMcc(dossier.text("id"), amount.toDoubleOrNull() ?: 0.0)) {
                            is ApiResult.Success -> { selected = null; refresh() }
                            is ApiResult.Error -> error = result.detail
                            is ApiResult.NetworkError -> error = result.message
                            ApiResult.Loading -> Unit
                        }
                    }
                }) { Text("Finalize amount") }
            }
        )
    }
}

@Composable
fun InterestPresetScreen(onBack: () -> Unit) {
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loanType by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var rateType by remember { mutableStateOf("annual") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            when (val result = api.getInterestPresets()) {
                is ApiResult.Success -> rows = result.data.objectItems()
                is ApiResult.Error -> error = result.detail
                is ApiResult.NetworkError -> error = result.message
                ApiResult.Loading -> Unit
            }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    SpecialistScaffold("Interest Presets", onBack) {
        error?.let { Text(it, color = FieldTheme.colors.statusDanger) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(loanType, { loanType = it }, label = { Text("Loan type") }, modifier = Modifier.weight(1f))
            OutlinedTextField(rate, { rate = it }, label = { Text("Rate") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(rateType, { rateType = it }, label = { Text("Rate type") }, modifier = Modifier.fillMaxWidth())
        PrimaryButton(if (editingId == null) "Create preset" else "Update preset", {
            scope.launch {
                val result = editingId?.let {
                    api.updateInterestPreset(it, loanType, rate.toDoubleOrNull() ?: 0.0, rateType)
                } ?: api.createInterestPreset(loanType, rate.toDoubleOrNull() ?: 0.0, rateType)
                when (result) {
                    is ApiResult.Success -> { loanType = ""; rate = ""; editingId = null; refresh() }
                    is ApiResult.Error -> error = result.detail
                    is ApiResult.NetworkError -> error = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }, Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows) { row ->
                FieldCard {
                    Text("${row.text("loan_type")} · ${row.text("rate")}%")
                    Text(row.text("rate_type"), color = FieldTheme.colors.gray400)
                    SecondaryButton("Edit", {
                        editingId = row.text("id")
                        loanType = row.text("loan_type")
                        rate = row.text("rate")
                        rateType = row.text("rate_type")
                    }, Modifier.fillMaxWidth())
                    SecondaryButton("Delete", {
                        scope.launch {
                            if (api.deleteInterestPreset(row.text("id")) is ApiResult.Success) refresh()
                        }
                    }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun BranchManagementScreen(onBack: () -> Unit) {
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            when (val result = api.getBranches()) {
                is ApiResult.Success -> rows = result.data.objectItems()
                is ApiResult.Error -> error = result.detail
                is ApiResult.NetworkError -> error = result.message
                ApiResult.Loading -> Unit
            }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    SpecialistScaffold("Branches", onBack) {
        error?.let { Text(it, color = FieldTheme.colors.statusDanger) }
        OutlinedTextField(name, { name = it }, label = { Text("Branch name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(code, { code = it }, label = { Text("Branch code") }, modifier = Modifier.fillMaxWidth())
        PrimaryButton("Create branch", {
            scope.launch {
                when (val result = api.createBranch(name, code)) {
                    is ApiResult.Success -> { name = ""; code = ""; refresh() }
                    is ApiResult.Error -> error = result.detail
                    is ApiResult.NetworkError -> error = result.message
                    ApiResult.Loading -> Unit
                }
            }
        }, Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows) { row ->
                FieldCard {
                    Text(row.text("name"), style = FieldTheme.typography.title)
                    Text(row.text("code"), color = FieldTheme.colors.gray400)
                }
            }
        }
    }
}
