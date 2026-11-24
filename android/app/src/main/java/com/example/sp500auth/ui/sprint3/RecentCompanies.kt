package com.example.sp500auth.ui.sprint3

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sp500auth.ui.common.AppColors
import com.example.sp500auth.ui.common.CompanyLogo

data class RecentCompany(val ticker:String?=null,val name:String?=null,val visited_at:String?=null)
@Composable fun RecentCompanies(items:List<RecentCompany>,open:(String)->Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Vistos recientemente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) Text("Todavía no hay empresas consultadas.", color = AppColors.Muted, style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.ticker.orEmpty() + it.visited_at }) { company ->
                Card(Modifier.width(132.dp).clickable { company.ticker?.let(open) }, shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Panel)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompanyLogo(company.ticker.orEmpty(), size = 40.dp)
                        Text(company.ticker.orEmpty(), color = AppColors.Green, fontWeight = FontWeight.Bold)
                        Text(company.name ?: company.ticker.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, color = AppColors.Muted)
                        Text("Ver ficha", style = MaterialTheme.typography.labelSmall, color = AppColors.Blue)
                    }
                }
            }
        }
    }
}
