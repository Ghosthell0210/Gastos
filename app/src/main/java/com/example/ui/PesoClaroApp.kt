package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOut
import com.example.data.Budget
import com.example.data.CategoryOption
import com.example.data.Debt
import com.example.data.Expense
import com.example.data.FinanceRepository
import com.example.data.Reminder
import com.example.notifications.ReminderScheduler
import com.example.ui.components.AccountSettingsSheet
import com.example.ui.components.AddEditDebtSheet
import com.example.ui.components.AddEditExpenseSheet
import com.example.ui.components.BudgetEditorSheet
import com.example.ui.components.CustomCategoryDialog
import com.example.ui.components.DebtDetailSheet
import com.example.ui.components.ReminderEditorSheet
import com.example.ui.screens.DeudasScreen
import com.example.ui.screens.GastosScreen
import com.example.ui.screens.PresupuestosScreen
import com.example.ui.screens.RecordatoriosScreen
import com.example.ui.screens.ReportesScreen
import com.example.ui.screens.ResumenScreen
import com.example.ui.state.FinanceDashboardData
import com.example.ui.state.FinanceViewModel
import com.example.ui.state.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_RESUMEN = "resumen"
const val ROUTE_GASTOS = "gastos"
const val ROUTE_DEUDAS = "deudas"
const val ROUTE_PRESUPUESTOS = "presupuestos"
const val ROUTE_REPORTES = "reportes"
const val ROUTE_RECORDATORIOS = "recordatorios"

private data class TopNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topNavItems = listOf(
    TopNavItem(ROUTE_RESUMEN, R.string.nav_overview, Icons.Rounded.Home, Icons.Outlined.Home, "nav_resumen"),
    TopNavItem(ROUTE_GASTOS, R.string.nav_expenses, Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_gastos"),
    TopNavItem(ROUTE_DEUDAS, R.string.nav_debts, Icons.Rounded.AccountBalance, Icons.Outlined.AccountBalance, "nav_deudas"),
    TopNavItem(ROUTE_PRESUPUESTOS, R.string.nav_budgets, Icons.Rounded.Savings, Icons.Outlined.Savings, "nav_presupuestos"),
    TopNavItem(ROUTE_REPORTES, R.string.nav_reports, Icons.Rounded.Analytics, Icons.Outlined.Analytics, "nav_reportes")
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedPesoClaroApp(
                currentUser = user,
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedPesoClaroApp(
    currentUser: FirebaseUser,
    onSignOut: () -> Unit
) {
    val uid = currentUser.uid
    val viewModel: FinanceViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FinanceViewModel(
                    repository = FinanceRepository(db),
                    currentUserId = uid,
                    reminderScheduler = ReminderScheduler(app)
                )
            }
        }
    )

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_RESUMEN

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Modal sheet / dialog state
    var showExpenseSheet by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    var showDebtSheet by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<Debt?>(null) }
    var detailDebtId by remember { mutableStateOf<String?>(null) }

    var showBudgetSheet by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }
    var preselectedBudgetCategory by remember { mutableStateOf<CategoryOption?>(null) }

    var showReminderSheet by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var preselectedReminderDebt by remember { mutableStateOf<Debt?>(null) }

    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    val dashboardData = (dashboardState as? UiState.Success<FinanceDashboardData>)?.data
    val activeRemindersCount = dashboardData?.reminders?.count { it.enabled } ?: 0

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp
        val isOnMainTab = currentRoute != ROUTE_RECORDATORIOS

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isOnMainTab) {
                    val title = when (currentRoute) {
                        ROUTE_GASTOS -> "Gastos"
                        ROUTE_DEUDAS -> "Deudas"
                        ROUTE_PRESUPUESTOS -> "Presupuestos"
                        ROUTE_REPORTES -> "Reportes mensuales"
                        else -> "PesoClaro"
                    }
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "CUP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navController.navigate(ROUTE_RECORDATORIOS) },
                                modifier = Modifier.testTag("open_reminders_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (activeRemindersCount > 0) {
                                            Badge { Text(activeRemindersCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = stringResource(R.string.action_reminders)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showAccountSheet = true },
                                modifier = Modifier.testTag("open_account_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = stringResource(R.string.action_account)
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!useNavRail && isOnMainTab) {
                    NavigationBar {
                        topNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isOnMainTab && dashboardData != null && currentRoute != ROUTE_REPORTES) {
                    val fabLabel = when (currentRoute) {
                        ROUTE_DEUDAS -> "Nueva deuda"
                        ROUTE_PRESUPUESTOS -> "Nuevo límite"
                        else -> "Nuevo gasto"
                    }
                    ExtendedFloatingActionButton(
                        onClick = {
                            when (currentRoute) {
                                ROUTE_DEUDAS -> {
                                    editingDebt = null
                                    showDebtSheet = true
                                }
                                ROUTE_PRESUPUESTOS -> {
                                    editingBudget = null
                                    preselectedBudgetCategory = null
                                    showBudgetSheet = true
                                }
                                else -> {
                                    editingExpense = null
                                    showExpenseSheet = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(fabLabel) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("main_fab")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && isOnMainTab) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        header = {
                            FloatingActionButton(
                                onClick = {
                                    editingExpense = null
                                    showExpenseSheet = true
                                }
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Nuevo gasto")
                            }
                        }
                    ) {
                        Spacer(Modifier.height(12.dp))
                        topNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                when (val state = dashboardState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Sincronizando tus finanzas...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is UiState.Success -> {
                        val data = state.data
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_RESUMEN,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(ROUTE_RESUMEN) {
                                ResumenScreen(
                                    data = data,
                                    onPreviousMonth = viewModel::previousMonth,
                                    onNextMonth = viewModel::nextMonth,
                                    onAddExpense = {
                                        editingExpense = null
                                        showExpenseSheet = true
                                    },
                                    onAddDebt = {
                                        editingDebt = null
                                        showDebtSheet = true
                                    },
                                    onEditExpense = { exp ->
                                        editingExpense = exp
                                        showExpenseSheet = true
                                    },
                                    onNavigateToExpenses = { navigateTopTab(ROUTE_GASTOS) },
                                    onNavigateToDebts = { navigateTopTab(ROUTE_DEUDAS) },
                                    onNavigateToBudgets = { navigateTopTab(ROUTE_PRESUPUESTOS) },
                                    onNavigateToReports = { navigateTopTab(ROUTE_REPORTES) },
                                    onNavigateToReminders = { navController.navigate(ROUTE_RECORDATORIOS) }
                                )
                            }
                            composable(ROUTE_GASTOS) {
                                GastosScreen(
                                    data = data,
                                    onPreviousMonth = viewModel::previousMonth,
                                    onNextMonth = viewModel::nextMonth,
                                    onAddExpense = {
                                        editingExpense = null
                                        showExpenseSheet = true
                                    },
                                    onEditExpense = { exp ->
                                        editingExpense = exp
                                        showExpenseSheet = true
                                    },
                                    onEditBudgetForCategory = { catId ->
                                        editingBudget = data.monthBudgets.firstOrNull { it.budget.categoryId == catId }?.budget
                                        preselectedBudgetCategory = data.categories.firstOrNull { it.id == catId }
                                        showBudgetSheet = true
                                    }
                                )
                            }
                            composable(ROUTE_DEUDAS) {
                                DeudasScreen(
                                    data = data,
                                    onAddDebt = {
                                        editingDebt = null
                                        showDebtSheet = true
                                    },
                                    onOpenDebtDetail = { debt -> detailDebtId = debt.id },
                                    onSettleDebt = { debt -> viewModel.settleDebt(debt) },
                                    onScheduleDebtReminder = { debt ->
                                        editingReminder = null
                                        preselectedReminderDebt = debt
                                        showReminderSheet = true
                                    }
                                )
                            }
                            composable(ROUTE_PRESUPUESTOS) {
                                PresupuestosScreen(
                                    data = data,
                                    onPreviousMonth = viewModel::previousMonth,
                                    onNextMonth = viewModel::nextMonth,
                                    onAddBudget = {
                                        editingBudget = null
                                        preselectedBudgetCategory = null
                                        showBudgetSheet = true
                                    },
                                    onAddBudgetForCategory = { cat ->
                                        editingBudget = null
                                        preselectedBudgetCategory = cat
                                        showBudgetSheet = true
                                    },
                                    onEditBudget = { budget ->
                                        editingBudget = budget
                                        preselectedBudgetCategory = null
                                        showBudgetSheet = true
                                    }
                                )
                            }
                            composable(ROUTE_REPORTES) {
                                ReportesScreen(
                                    data = data,
                                    onPreviousMonth = viewModel::previousMonth,
                                    onNextMonth = viewModel::nextMonth
                                )
                            }
                            composable(ROUTE_RECORDATORIOS) {
                                RecordatoriosScreen(
                                    reminders = data.reminders,
                                    onBack = { navController.popBackStack() },
                                    onAddReminder = {
                                        editingReminder = null
                                        preselectedReminderDebt = null
                                        showReminderSheet = true
                                    },
                                    onEditReminder = { rem ->
                                        editingReminder = rem
                                        preselectedReminderDebt = null
                                        showReminderSheet = true
                                    },
                                    onToggleReminder = viewModel::toggleReminder,
                                    onPreviewNotification = viewModel::previewReminderNotification
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Modal Bottom Sheets & Dialogs ---

    if (showExpenseSheet && dashboardData != null) {
        AddEditExpenseSheet(
            existingExpense = editingExpense,
            categories = dashboardData.categories,
            isSaving = isSaving,
            onDismiss = { showExpenseSheet = false },
            onAddCustomCategoryClick = { showCustomCategoryDialog = true },
            onSave = { title, amount, category, dateMillis, note ->
                viewModel.saveExpense(
                    existingId = editingExpense?.id,
                    title = title,
                    amount = amount,
                    category = category,
                    expenseDateMillis = dateMillis,
                    note = note,
                    onDone = { showExpenseSheet = false }
                )
            },
            onDelete = editingExpense?.let { exp ->
                {
                    viewModel.deleteExpense(exp.id) { showExpenseSheet = false }
                }
            }
        )
    }

    if (showDebtSheet) {
        AddEditDebtSheet(
            existingDebt = editingDebt,
            isSaving = isSaving,
            onDismiss = { showDebtSheet = false },
            onSave = { direction, counterparty, concept, totalAmount, dueDateMillis, note ->
                viewModel.saveDebt(
                    existingDebt = editingDebt,
                    direction = direction,
                    counterparty = counterparty,
                    concept = concept,
                    totalAmount = totalAmount,
                    dueDateMillis = dueDateMillis,
                    note = note,
                    onDone = { showDebtSheet = false }
                )
            },
            onDelete = editingDebt?.let { debt ->
                {
                    viewModel.deleteDebt(debt.id) {
                        showDebtSheet = false
                        detailDebtId = null
                    }
                }
            }
        )
    }

    val detailDebt = detailDebtId?.let { id ->
        dashboardData?.allDebts?.firstOrNull { it.id == id }
    }
    if (detailDebt != null) {
        val paymentsState by viewModel.observeDebtPayments(detailDebt.id)
            .collectAsStateWithLifecycle(initialValue = UiState.Loading)
        DebtDetailSheet(
            debt = detailDebt,
            paymentsState = paymentsState,
            isSaving = isSaving,
            onDismiss = { detailDebtId = null },
            onEditDebt = {
                editingDebt = detailDebt
                detailDebtId = null
                showDebtSheet = true
            },
            onScheduleReminder = {
                editingReminder = null
                preselectedReminderDebt = detailDebt
                detailDebtId = null
                showReminderSheet = true
            },
            onAddPayment = { amt, note ->
                viewModel.recordDebtPayment(detailDebt, amt, note)
            },
            onSettleDebt = {
                viewModel.settleDebt(detailDebt)
            }
        )
    }

    if (showBudgetSheet && dashboardData != null) {
        val initialBudget = editingBudget ?: preselectedBudgetCategory?.let { cat ->
            dashboardData.monthBudgets.firstOrNull { it.budget.categoryId == cat.id }?.budget
                ?: Budget(
                    categoryId = cat.id,
                    categoryName = cat.name,
                    yearMonth = dashboardData.selectedYearMonth,
                    limitAmount = 0.0
                )
        }
        BudgetEditorSheet(
            existingBudget = initialBudget?.takeIf { it.id.isNotBlank() || preselectedBudgetCategory != null },
            categories = dashboardData.categories,
            yearMonth = dashboardData.selectedYearMonth,
            isSaving = isSaving,
            onDismiss = { showBudgetSheet = false },
            onSave = { category, limitAmount ->
                val targetCat = preselectedBudgetCategory ?: category
                viewModel.saveBudget(
                    category = targetCat,
                    yearMonth = dashboardData.selectedYearMonth,
                    limitAmount = limitAmount,
                    onDone = { showBudgetSheet = false }
                )
            },
            onDelete = editingBudget?.takeIf { it.id.isNotBlank() }?.let { b ->
                {
                    viewModel.deleteBudget(b.id)
                    showBudgetSheet = false
                }
            }
        )
    }

    if (showReminderSheet && dashboardData != null) {
        ReminderEditorSheet(
            existingReminder = editingReminder,
            preselectedDebt = preselectedReminderDebt,
            debts = dashboardData.allDebts,
            categories = dashboardData.categories,
            isSaving = isSaving,
            onDismiss = { showReminderSheet = false },
            onSave = { title, type, targetId, targetName, freq, scheduledAt, enabled, note ->
                viewModel.saveReminder(
                    existingId = editingReminder?.id,
                    title = title,
                    reminderType = type,
                    targetId = targetId,
                    targetName = targetName,
                    frequency = freq,
                    scheduledAtMillis = scheduledAt,
                    enabled = enabled,
                    note = note,
                    onDone = { showReminderSheet = false }
                )
            },
            onDelete = editingReminder?.let { rem ->
                {
                    viewModel.deleteReminder(rem.id)
                    showReminderSheet = false
                }
            }
        )
    }

    if (showCustomCategoryDialog) {
        CustomCategoryDialog(
            isSaving = isSaving,
            onDismiss = { showCustomCategoryDialog = false },
            onSave = { name, iconKey, colorHex ->
                viewModel.addCustomCategory(name, iconKey, colorHex) {
                    showCustomCategoryDialog = false
                }
            }
        )
    }

    if (showAccountSheet) {
        AccountSettingsSheet(
            user = currentUser,
            onDismiss = { showAccountSheet = false },
            onOpenReminders = { navController.navigate(ROUTE_RECORDATORIOS) },
            onSignOut = {
                showAccountSheet = false
                onSignOut()
            }
        )
    }
}
