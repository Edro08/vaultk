package com.edro08.vaultk.ui.screen.storage

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.edro08.vaultk.R
import com.edro08.vaultk.domain.VaultSaveProgress
import com.edro08.vaultk.domain.VaultSaveStage
import com.edro08.vaultk.ui.component.VaultkCard
import com.edro08.vaultk.ui.theme.VaultkSpacing
import kotlinx.coroutines.delay

/** Show the current save stage and elapsed time without an estimated overall percentage. */
@Composable
fun VaultSaveDialog(progress: VaultSaveProgress, startedAtMillis: Long) {
    var elapsedMillis by remember(startedAtMillis) { mutableLongStateOf(0L) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            delay(100)
            elapsedMillis = (SystemClock.elapsedRealtime() - startedAtMillis).coerceAtLeast(0L)
        }
    }
    val stage = when (progress.stage) {
        VaultSaveStage.Preparing -> R.string.content_save_preparing
        VaultSaveStage.Writing -> R.string.content_save_writing
        VaultSaveStage.Verifying -> R.string.content_save_verifying
        VaultSaveStage.Replacing -> R.string.content_save_replacing
    }
    Dialog(onDismissRequest = {}, properties = DialogProperties(
        dismissOnBackPress = false, dismissOnClickOutside = false)) {
        VaultkCard(Modifier.testTag("vault-save-dialog")) {
            Row(Modifier.fillMaxWidth().padding(VaultkSpacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VaultkSpacing.medium)) {
                CircularProgressIndicator(Modifier.size(48.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaultkSpacing.tiny)) {
                    Text(stringResource(R.string.content_saving), style = MaterialTheme.typography.titleLarge,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(stage), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("vault-save-stage"))
                    val elapsed = formatOpeningElapsed(elapsedMillis)
                    val elapsedDescription = stringResource(R.string.content_save_elapsed_description, elapsed)
                    Text(elapsed, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { contentDescription = elapsedDescription }
                            .testTag("vault-save-elapsed"))
                }
            }
        }
    }
}
