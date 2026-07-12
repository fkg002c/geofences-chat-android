package com.ruinkogr.chatapp.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ruinkogr.chatapp.R

@Composable
fun AppLanguageDialog(
    currentLanguageCode: String,
    onSelect: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onSelect(null) },
        title = { Text(text = stringResource(R.string.select_app_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                supportedLanguages.forEach { language ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (language.code == currentLanguageCode),
                                onClick = {
                                    onSelect(language.code)
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (language.code == currentLanguageCode),
                            onClick = null
                        )
                        Text(
                            text = stringResource(language.nameRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(null) }) {
                Text(stringResource(R.string.label_cancel))
            }
        }
    )
}