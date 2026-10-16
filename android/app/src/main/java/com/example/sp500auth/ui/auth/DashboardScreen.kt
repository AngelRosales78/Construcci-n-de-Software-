package com.example.sp500auth.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val price: String,
    val change: String,
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

    val stockList = listOf(
        StockAsset("AAPL", "Apple Inc.", "Tecnología", "$2.92T", "$189.45", "+1.85%", true, "7.12%", listOf(10f, 15f, 12f, 18f, 25f, 20f, 28f)),
        StockAsset("MSFT", "Microsoft Corporation", "Tecnología", "$3.14T", "$422.18", "+0.92%", true, "6.85%", listOf(15f, 14f, 16f, 12f, 10f, 14f, 18f)),
        StockAsset("NVDA", "NVIDIA Corporation", "Semiconductores", "$2.15T", "$875.28", "+3.42%", true, "5.04%", listOf(5f, 10f, 15f, 20f, 22f, 28f, 32f)),
        StockAsset("JPM", "JPMorgan Chase & Co.", "Finanzas", "$570B", "$198.50", "-0.45%", false, "1.28%", listOf(25f, 22f, 18f, 15f, 20f, 16f, 14f)),
        StockAsset("JNJ", "Johnson & Johnson", "Salud", "$375B", "$156.28", "+0.25%", true, "0.94%", listOf(12f, 13f, 12f, 14f, 13f, 15f, 15f))
    )

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
                        Text("Salir", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Explorar · ejemplo",
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
                                .background(PrimaryGreen)
                                .clickable { navController?.navigate(NavRoutes.Profile.route) },
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

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todos", "Tecnología", "Salud").forEach { category ->
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
                                Text("Datos de demostración", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("Valores ilustrativos · consulta web para datos importados", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("+0.74%", color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("503 activos", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

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
                            Text("5 activos", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Orden: Cap. ▾", color = PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(stockList) { stock ->
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