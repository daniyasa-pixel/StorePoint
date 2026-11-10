package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.Supplier
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*

@Composable
fun SupplierCrudDialog(
    editingSupplier: Supplier?,
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(editingSupplier?.name ?: "") }
    var contactPersonInput by remember { mutableStateOf(editingSupplier?.contactPerson ?: "") }
    var phoneInput by remember { mutableStateOf(editingSupplier?.phone ?: "") }
    var deliveryScheduleInput by remember { mutableStateOf(editingSupplier?.deliverySchedule ?: "") }
    var paymentTermsInput by remember { mutableStateOf(editingSupplier?.paymentTerms ?: "COD") }
    var notesInput by remember { mutableStateOf(editingSupplier?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingSupplier == null) "Add Supplier / Distributor" else "Edit Supplier Details") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroInput(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Company / Supplier Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                )

                HeroInput(
                    value = contactPersonInput,
                    onValueChange = { contactPersonInput = it },
                    label = { Text("Contact Person / Sales Agent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HeroInput(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Contact Phone / Mobile") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_phone_input")
                )

                HeroInput(
                    value = deliveryScheduleInput,
                    onValueChange = { deliveryScheduleInput = it },
                    label = { Text("Delivery Schedule (e.g., Every Tuesday)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Terms:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("COD", "Net 7", "Net 15", "Net 30", "Consignment").forEach { term ->
                        HeroFilterChip(
                            selected = paymentTermsInput == term,
                            onClick = { paymentTermsInput = term },
                            label = { Text(term) }
                        )
                    }
                }

                HeroInput(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes / Account Number / Delivery Instructions") },
                    maxLines = 3,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            HeroButton(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        val sup = Supplier(
                            id = editingSupplier?.id ?: 0,
                            name = nameInput.trim(),
                            contactPerson = contactPersonInput.trim(),
                            phone = phoneInput.trim(),
                            deliverySchedule = deliveryScheduleInput.trim(),
                            paymentTerms = paymentTermsInput.trim(),
                            notes = notesInput.trim()
                        )
                        viewModel.saveSupplier(sup) {
                            Toast.makeText(context, "Supplier '${sup.name}' saved.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                },
                enabled = nameInput.isNotBlank(),
                modifier = Modifier.testTag("save_supplier_btn"),
                text = "Save Supplier",
                color = HeroColor.Primary,
                variant = HeroButtonVariant.Solid,
                size = HeroSize.Md
            )
        },
        dismissButton = {
            HeroTextButton(onClick = onDismiss, text = "Cancel")
        }
    )
}
