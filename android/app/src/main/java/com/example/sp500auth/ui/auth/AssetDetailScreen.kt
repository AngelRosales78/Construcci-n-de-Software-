package com.example.sp500auth.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DarkBg = Color(0xFF0D1117)
private val CardBg = Color(0xFF161B22)
private val InnerBg = Color(0xFF21262D)
private val PrimaryGreen = Color(0xFF00FF87)
private val WarningYellow = Color(0xFFFFC107)
private val DangerRed = Color(0xFFFF5252)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF8B949E)
private val BorderDark = Color(0xFF30363D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    navController: NavController? = null
) {
    var selectedTimeFrame by remember { mutableStateOf("1Y") }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            // Barra inferior con botón de Operar
            Surface(
                color = CardBg,
                modifier = Modifier.border(1.dp, BorderDark, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(InnerBg)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = TextWhite)
                    }

                    Button(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Operar AAPL", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(InnerBg)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = TextWhite)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. TopBar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController?.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextWhite)
                        }
                        Text("Detalle De Activo", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextMuted)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 2. Precio Principal
            item {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$189.45", color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("USD", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PrimaryGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1.85% (+$3.44) Hoy", color = PrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Card Gráfico Comportamiento Técnico
            item {
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
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Comportamiento Técnico", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(InnerBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Táctil", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Simulación del gráfico técnico interactivo
                        TechnicalChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Time Filters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("1M", "6M", "1Y", "5Y").forEach { tf ->
                                val isSelected = tf == selectedTimeFrame
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryGreen.copy(alpha = 0.2f) else InnerBg)
                                        .border(1.dp, if (isSelected) PrimaryGreen else BorderDark, RoundedCornerShape(8.dp))
                                        .clickable { selectedTimeFrame = tf }
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(tf, color = if (isSelected) PrimaryGreen else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Métricas Fundamentales Grid
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Métricas Fundamentales", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Q4 Actualizado", color = TextMuted, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "P/E Ratio",
                            value = "28.4x",
                            subText = "Sector prom: 31.2x",
                            isGreen = false
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Retorno (ROE)",
                            value = "147.2%",
                            subText = "Alto rendimiento",
                            isGreen = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Market Cap",
                            value = "$2.92T",
                            subText = "Mega Cap",
                            isGreen = false
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Beta (Volatilidad)",
                            value = "1.28",
                            subText = "Riesgo moderado",
                            isGreen = false
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Card Dividendos Wide
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBg)
                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Rendimiento Dividendos", color = TextMuted, fontSize = 11.sp)
                                Text("0.55% anual", color = PrimaryGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$0.96 / acción", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Pago trimestral", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // 5. Card Gauge Solvencia Altman Z-Score
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solvencia (Altman Z-Score)", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Semicírculo Gauge
                        GaugeArc(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        )

                        Text("Z-Score 4.82", color = PrimaryGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Zona Segura", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Riesgo de quiebra estadísticamente nulo. Excelente fortaleza en balances corporativos.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LegendDot("Peligro (< 1.8)", DangerRed)
                            LegendDot("Moderado (1.8 - 3.0)", WarningYellow)
                            LegendDot("Seguro (> 3.0)", PrimaryGreen)
                        }
                    }
                }
            }

            // 6. Card Perfil de la Compañía
            item {
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Perfil de la Compañía", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Cupertino, CA", color = PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Card con imagen simulación
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(InnerBg)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ChipBadge("Hardware • Servicios • IA")
                                    ChipBadge("ESG Rating: AAA", PrimaryGreen)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Diseña, manufactura y comercializa smartphones, computadoras personales, tabletas, wearables y provee servicios digitales.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subText: String,
    isGreen: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = if (isGreen) PrimaryGreen else TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subText, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
fun ChipBadge(text: String, color: Color = TextWhite) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkBg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TechnicalChart(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path()
        val w = size.width
        val h = size.height

        val points = listOf(
            Offset(0f, h * 0.7f),
            Offset(w * 0.15f, h * 0.65f),
            Offset(w * 0.3f, h * 0.45f),
            Offset(w * 0.45f, h * 0.55f),
            Offset(w * 0.6f, h * 0.25f),
            Offset(w * 0.8f, h * 0.1f),
            Offset(w, h * 0.15f)
        )

        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }

        // Relleno de degradado
        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(PrimaryGreen.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        drawPath(
            path = path,
            color = PrimaryGreen,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun GaugeArc(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val diameter = w * 0.7f
        val topLeft = Offset((w - diameter) / 2, h * 0.1f)
        val arcSize = Size(diameter, diameter)
        val strokeWidth = 14.dp.toPx()

        // Sector Rojo
        drawArc(
            color = DangerRed,
            startAngle = 180f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Sector Amarillo
        drawArc(
            color = WarningYellow,
            startAngle = 240f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Sector Verde
        drawArc(
            color = PrimaryGreen,
            startAngle = 300f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Aguja indicadora hacia el Z-Score
        val angleRad = (330f * PI / 180f).toFloat()
        val centerX = w / 2
        val centerY = topLeft.y + diameter / 2
        val pointerLength = diameter * 0.45f

        val endX = centerX + pointerLength * cos(angleRad)
        val endY = centerY + pointerLength * sin(angleRad)

        drawLine(
            color = TextWhite,
            start = Offset(centerX, centerY),
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(color = TextWhite, radius = 6.dp.toPx(), center = Offset(centerX, centerY))
    }
}