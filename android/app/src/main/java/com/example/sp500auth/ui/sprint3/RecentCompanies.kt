package com.example.sp500auth.ui.sprint3

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

data class RecentCompany(val ticker:String?=null,val name:String?=null,val visited_at:String?=null)
@Composable fun RecentCompanies(items:List<RecentCompany>,open:(String)->Unit) {
    Column {
        Text("Vistos recientemente",style=MaterialTheme.typography.titleLarge)
        if(items.isEmpty())Text("Todavía no hay empresas consultadas.")
        items.forEach {company->Card {Column {Text(company.name ?: company.ticker.orEmpty());Text("${company.ticker} · ${company.visited_at}");TextButton(onClick={company.ticker?.let(open)}){Text("Ver ficha")}}}}
    }
}
