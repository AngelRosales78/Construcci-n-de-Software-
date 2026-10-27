package com.example.sp500auth.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sp500auth.ui.navigation.NavRoutes

// Paleta de Colores
private val DarkBg = Color(0xFF0D1117)
private val CardBg = Color(0xFF161B22)
private val InnerBg = Color(0xFF21262D)
private val PrimaryGreen = Color(0xFF00FF87)
private val NegativeRed = Color(0xFFFF5252)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF8B949E)
private val BorderDark = Color(0xFF30363D)

data class StockAsset(
    val symbol: String,
    val name: String,
    val category: String,
    val marketCap: String,
    val marketCapValue: Double, // Valor numérico para ordenar por Capitalización
    val price: String,
    val priceValue: Double,     // Valor numérico para ordenar por Precio
    val change: String,
    val changeValue: Double,    // Valor numérico para ordenar por % de cambio
    val isPositive: Boolean,
    val weighting: String,
    val sparklinePoints: List<Float>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController? = null,
    viewModel: AuthViewModel? = null
) {
    var selectedCategory by remember { mutableStateOf("Todos") }
    var sortOption by remember { mutableStateOf("Cap.") }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    val masterStockList = remember {
        listOf(
            StockAsset(
                symbol = "AAPL",
                name = "Apple Inc.",
                category = "Tecnología",
                marketCap = "$2.92T",
                marketCapValue = 2920.0,
                price = "$189.45",
                priceValue = 189.45,
                change = "+1.85%",
                changeValue = 1.85,
                isPositive = true,
                weighting = "7.12%",
                sparklinePoints = listOf(10f, 15f, 12f, 18f, 25f, 20f, 28f)
            ),
            StockAsset(
                symbol = "MSFT",
                name = "Microsoft Corporation",
                category = "Tecnología",
                marketCap = "$3.14T",
                marketCapValue = 3140.0,
                price = "$422.18",
                priceValue = 422.18,
                change = "+0.92%",
                changeValue = 0.92,
                isPositive = true,
                weighting = "6.85%",
                sparklinePoints = listOf(15f, 14f, 16f, 12f, 10f, 14f, 18f)
            ),
            StockAsset(
                symbol = "NVDA",
                name = "NVIDIA Corporation",
                category = "Semiconductores",
                marketCap = "$2.15T",
                marketCapValue = 2150.0,
                price = "$875.28",
                priceValue = 875.28,
                change = "+3.42%",
                changeValue = 3.42,
                isPositive = true,
                weighting = "5.04%",
                sparklinePoints = listOf(5f, 10f, 15f, 20f, 22f, 28f, 32f)
            ),
            StockAsset(
                symbol = "JPM",
                name = "JPMorgan Chase & Co.",
                category = "Finanzas",
                marketCap = "$570B",
                marketCapValue = 570.0,
                price = "$198.50",
                priceValue = 198.50,
                change = "-0.45%",
                changeValue = -0.45,
                isPositive = false,
                weighting = "1.28%",
                sparklinePoints = listOf(25f, 22f, 18f, 15f, 20f, 16f, 14f)
            ),
            StockAsset(
                symbol = "JNJ",
                name = "Johnson & Johnson",
                category = "Salud",
                marketCap = "$375B",
                marketCapValue = 375.0,
                price = "$156.28",
                priceValue = 156.28,
                change = "+0.25%",
                changeValue = 0.25,
                isPositive = true,
                weighting = "0.94%",
                sparklinePoints = listOf(12f, 13f, 12f, 14f, 13f, 15f, 15f)
            )
        )
    }

    // Filtrado y Ordenamiento Dinámico
    val displayedStockList = remember(selectedCategory, sortOption) {
        val filtered = if (selectedCategory == "Todos") {
            masterStockList
        } else {
            masterStockList.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        when (sortOption) {
            "Precio" -> filtered.sortedByDescending { it.priceValue }
            "Variación" -> filtered.sortedByDescending { it.changeValue }
            "Nombre" -> filtered.sortedBy { it.name }
            else -> filtered.sortedByDescending { it.marketCapValue } // "Cap." por defecto
        }
    }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            Surface(
                color = CardBg,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, BorderDark, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ficha / Mercados", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PrimaryGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Explorar", color = PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel?.logout() }
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Acceso", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Encabezado
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Explorar",
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CardBg)
                                .border(1.dp, BorderDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Chips de Categorías
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf("Todos", "Tecnología", "Salud", "Semiconductores", "Finanzas")
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryGreen else CardBg)
                                .border(1.dp, if (isSelected) PrimaryGreen else BorderDark, RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isSelected) "✓ $category" else category,
                                color = if (isSelected) Color.Black else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Banner S&P 500
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Índice S&P 500", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("5,649.76 USD • NYSE Activo", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("+0.74%", color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("503 activos", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Subtítulo y Menú Desplegable de Ordenamiento
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Principales\nPonderaciones",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(InnerBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("${displayedStockList.size} activos", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(InnerBg)
                                    .clickable { isSortMenuExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Orden: $sortOption ▾", color = PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false },
                                modifier = Modifier
                                    .background(CardBg)
                                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            ) {
                                listOf("Cap.", "Precio", "Variación", "Nombre").forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option,
                                                color = if (sortOption == option) PrimaryGreen else TextWhite,
                                                fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            )
                                        },
                                        onClick = {
                                            sortOption = option
                                            isSortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lista Filtrada
            items(displayedStockList) { stock ->
                StockCard(stock = stock, navController = navController)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun StockCard(stock: StockAsset, navController: NavController? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(InnerBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stock.symbol,
                            color = PrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(stock.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${stock.category} • Cap ${stock.marketCap}", color = TextMuted, fontSize = 11.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stock.price, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(" USD", color = TextMuted, fontSize = 10.sp)
                    }
                    Text(
                        text = if (stock.isPositive) "▲ ${stock.change}" else "▼ ${stock.change}",
                        color = if (stock.isPositive) PrimaryGreen else NegativeRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SparklineChart(
                points = stock.sparklinePoints,
                color = if (stock.isPositive) PrimaryGreen else NegativeRed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ponderación: ${stock.weighting}", color = TextMuted, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        navController?.navigate(NavRoutes.AssetDetail.route)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InnerBg,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Ver Info ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun SparklineChart(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val path = Path()
        val maxX = size.width
        val maxY = size.height
        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 1f
        val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

        points.forEachIndexed { index, point ->
            val x = (index.toFloat() / (points.size - 1)) * maxX
            val y = maxY - ((point - minVal) / range) * (maxY * 0.7f) - (maxY * 0.15f)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}