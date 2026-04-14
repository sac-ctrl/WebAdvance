package com.cylonid.nativealpha.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cylonid.nativealpha.manager.Credential
import com.cylonid.nativealpha.viewmodel.CredentialViewModel
import com.cylonid.nativealpha.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialScreen(
    webAppId: Long? = null, // null for global
    viewModel: CredentialViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val credentials by viewModel.credentials.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val editingCredential by viewModel.editingCredential.collectAsState()

    LaunchedEffect(webAppId) {
        viewModel.loadCredentials(webAppId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (webAppId == null) "Global Credentials" else "App Credentials") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddCredentialDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add credential")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search credentials") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            )

            if (credentials.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No credentials stored")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(credentials) { credential ->
                        CredentialCard(
                            credential = credential,
                            onCopyUsername = { viewModel.copyUsername(credential, context) },
                            onCopyPassword = { viewModel.copyPassword(credential, context) },
                            onAutoFill = {},
                            onEdit = { viewModel.editCredential(credential) },
                            onDelete = { viewModel.deleteCredential(credential) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCredentialDialog(
                credential = editingCredential,
                onDismiss = { viewModel.hideAddCredentialDialog() },
                onSave = { cred: Credential ->
                    viewModel.saveCredential(webAppId ?: 0L, cred)
                }
            )
        }
    }
}