package com.stellarforge.composebooking.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Service

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListItem(
    service: Service,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        onClick = onClick
    ) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp), // ListItem'ın kendi iç padding'i
            headlineContent = {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium // Başlık stilini belirginleştir
                )
            },
            supportingContent = {
                Column {
                    if (service.description.isNotBlank()) {
                        Text(
                            service.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, // Açıklama için 2 satır yeterli olabilir
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Biraz daha soluk
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween, // Süre ve fiyatı ayırabilir
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.service_list_item_duration, service.durationMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = stringResource(id = R.string.service_list_item_price, service.price),
                            style = MaterialTheme.typography.labelLarge, // Fiyatı biraz daha belirgin yap
                            color = MaterialTheme.colorScheme.primary // Fiyatı ana renkle vurgula
                        )
                    }
                }
            },
            leadingContent = { // Opsiyonel: Sol tarafa bir ikon
                Icon(
                    imageVector = Icons.Filled.Menu, // Ya da servise özel bir ikon
                    contentDescription = null, // Servis adı zaten açıklayıcı
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp) // İkon boyutu
                )
            },
            trailingContent = { // Opsiyonel: Sağ tarafa bir ikon
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.view_details_desc),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            // ListItem'ın kendi tıklanabilirliğini kaldırabiliriz, çünkü Surface tıklanabilir.
            // VEYA Surface'ın onClick'ini kaldırıp ListItem'ınkini kullanabiliriz.
            // Eğer Surface'a onClick verdiysek, ListItem'daki .clickable(onClick = onClick) gereksiz.
        )
    }
}