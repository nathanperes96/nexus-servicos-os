package com.nexus.servicos.os.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nexus.servicos.os.data.*
import com.nexus.servicos.os.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    ordens: List<OrdemServico>,
    onNavigateToNovaOs: (TipoAtendimento) -> Unit,
    onNavigateToHistorico: () -> Unit,
    onNavigateToClientes: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    val context = LocalContext.current

    val qtdAnalise = ordens.count { it.status == StatusOrdem.EM_ANALISE }
    val qtdAguardando = ordens.count { it.status == StatusOrdem.AGUARDANDO_APROVACAO || it.status == StatusOrdem.ORCAMENTO_ENVIADO }
    val qtdExecucao = ordens.count { it.status == StatusOrdem.APROVADO || it.status == StatusOrdem.EM_EXECUCAO || it.status == StatusOrdem.AGUARDANDO_PECA }
    val qtdProntos = ordens.count { it.status == StatusOrdem.CONCLUIDO || it.status == StatusOrdem.PRONTO_RETIRADA }
    val qtdEntregues = ordens.count { it.status == StatusOrdem.ENTREGUE }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(containerColor = DarkSurface, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OrangeNeon,
                        selectedTextColor = OrangeNeon,
                        indicatorColor = OrangeDark
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistorico,
                    icon = { Icon(Icons.Default.Description, contentDescription = "Orçamentos", tint = TextSecondary) },
                    label = { Text("Orçamentos", fontSize = 11.sp, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToClientes,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Clientes", tint = TextSecondary) },
                    label = { Text("Clientes", fontSize = 11.sp, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToConfig,
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Backup", tint = TextSecondary) },
                    label = { Text("Backup", fontSize = 11.sp, color = TextSecondary) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoNexusVetorial(modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("NEXUS SERVIÇOS", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text("MÁQUINAS & INFORMÁTICA", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = { onNavigateToNovaOs(TipoAtendimento.MAQUINAS) },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+ Nova OS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Olá, Nathan!", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Resumo em tempo real da oficina:", color = TextSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IndicadorStatusCard("Em análise", qtdAnalise, OrangeNeon, onNavigateToHistorico)
                IndicadorStatusCard("Aguardando", qtdAguardando, StatusAmber, onNavigateToHistorico)
                IndicadorStatusCard("Em execução", qtdExecucao, StatusGreen, onNavigateToHistorico)
                IndicadorStatusCard("Prontos", qtdProntos, CyanNeon, onNavigateToHistorico)
                IndicadorStatusCard("Entregues", qtdEntregues, TextSecondary, onNavigateToHistorico)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CategoryCard(
                    title = "Máquinas",
                    subtitle = "Motosserra, roçadeira,\ncortador, gerador, etc.",
                    icon = Icons.Default.Build,
                    accentColor = OrangeNeon,
                    backgroundColor = OrangeDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToNovaOs(TipoAtendimento.MAQUINAS) }
                )
                CategoryCard(
                    title = "Tecnologia",
                    subtitle = "Computadores, notebooks,\ncelulares, redes, etc.",
                    icon = Icons.Default.Computer,
                    accentColor = CyanNeon,
                    backgroundColor = CyanDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToNovaOs(TipoAtendimento.TECNOLOGIA) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuButton("Orçamentos", Icons.Default.Assignment, OrangeNeon, Modifier.weight(1f), onNavigateToHistorico)
                MenuButton("Clientes", Icons.Default.People, CyanNeon, Modifier.weight(1f), onNavigateToClientes)
                MenuButton("Histórico OS", Icons.Default.History, OrangeNeon, Modifier.weight(1f), onNavigateToHistorico)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuButton("Backup / Excel", Icons.Default.CloudUpload, StatusGreen, Modifier.weight(1f), onNavigateToConfig)
                MenuButton("Configurações", Icons.Default.Settings, OrangeNeon, Modifier.weight(1f), onNavigateToConfig)
                MenuButton("Sair", Icons.AutoMirrored.Filled.ExitToApp, OrangeNeon, Modifier.weight(1f)) {
                    (context as? Activity)?.finish()
                }
            }
        }
    }
}

@Composable
fun IndicadorStatusCard(titulo: String, quantidade: Int, cor: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .border(1.dp, cor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(quantidade.toString(), color = cor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(titulo, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.85f), backgroundColor)))
            .border(1.5.dp, accentColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 3)
    }
}

@Composable
fun MenuButton(title: String, icon: ImageVector, corIcone: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = corIcone, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, color = TextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizarPdfScreen(
    arquivoPdf: File?,
    numeroOs: String,
    tipo: TipoAtendimento,
    onVoltarHome: () -> Unit,
    onCompartilhar: () -> Unit
) {
    val corPrincipal = if (tipo == TipoAtendimento.MAQUINAS) OrangeNeon else CyanNeon
    val numFormatado = numeroOs.padStart(6, '0')
    var paginaSelecionada by remember { mutableStateOf(0) }
    var totalPaginas by remember { mutableStateOf(1) }

    val bitmapPdf = remember(arquivoPdf, paginaSelecionada) {
        if (arquivoPdf != null && arquivoPdf.exists()) {
            try {
                val pfd = ParcelFileDescriptor.open(arquivoPdf, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                totalPaginas = renderer.pageCount.coerceAtLeast(1)
                val idxSeguro = paginaSelecionada.coerceIn(0, totalPaginas - 1)
                val page = renderer.openPage(idxSeguro)
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                bmp.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OS_$numFormatado.pdf", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Pág. ${paginaSelecionada + 1}/$totalPaginas",
                            color = corPrincipal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltarHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = corPrincipal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (totalPaginas > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (paginaSelecionada > 0) paginaSelecionada-- },
                        enabled = paginaSelecionada > 0
                    ) {
                        Text("◄ Pág. Anterior", color = TextPrimary, fontSize = 12.sp)
                    }
                    Text(
                        if (paginaSelecionada == 0) "Resumo da OS" else "Registro Fotográfico",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedButton(
                        onClick = { if (paginaSelecionada < totalPaginas - 1) paginaSelecionada++ },
                        enabled = paginaSelecionada < totalPaginas - 1
                    ) {
                        Text("Próxima Pág. ►", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.5.dp, corPrincipal, RoundedCornerShape(12.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmapPdf != null) {
                    Image(
                        bitmap = bitmapPdf,
                        contentDescription = "Página do PDF",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("PDF pronto para compartilhamento!", color = TextPrimary)
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCompartilhar,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corPrincipal)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Compartilhar PDF ($totalPaginas pág.)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    ordens: List<OrdemServico>,
    onVoltar: () -> Unit,
    onMudarStatus: (OrdemServico, String) -> Unit,
    onEditarDadosClienteOs: (OrdemServico) -> Unit,
    onEditarOrcarOs: (OrdemServico) -> Unit,
    onAdicionarFotoSaida: (OrdemServico, String) -> Unit,
    onExcluirFotoOs: (OrdemServico, String) -> Unit,
    onVisualizarOuEnviarPdf: (OrdemServico) -> Unit
) {
    val context = LocalContext.current
    var filtroCategoria by remember { mutableStateOf("Todos") }
    var busca by remember { mutableStateOf("") }
    var osParaFotoSaida by remember { mutableStateOf<OrdemServico?>(null) }
    var uriTempSaida by remember { mutableStateOf<Uri?>(null) }
    var osParaEscolherStatus by remember { mutableStateOf<OrdemServico?>(null) }

    val cameraSaidaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val os = osParaFotoSaida
        val uri = uriTempSaida
        if (ok && os != null && uri != null) {
            onAdicionarFotoSaida(os, uri.toString())
        }
    }

    val listaFiltrada = ordens.filter { os ->
        val bateCat = when (filtroCategoria) {
            "Máquinas" -> os.tipo == TipoAtendimento.MAQUINAS
            "Tecnologia" -> os.tipo == TipoAtendimento.TECNOLOGIA
            else -> true
        }
        val termo = busca.trim().lowercase()
        val numSemZero = os.numeroOs.trim().lowercase().removePrefix("0")
        val termoSemZero = termo.removePrefix("0")

        val bateBusca = termo.isBlank() ||
                os.nomeCliente.lowercase().contains(termo) ||
                os.equipamento.lowercase().contains(termo) ||
                os.telefoneCliente.lowercase().contains(termo) ||
                os.status.lowercase().contains(termo) ||
                os.numeroOs.lowercase().contains(termo) ||
                numSemZero.contains(termoSemZero)

        bateCat && bateBusca
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ordens de Serviço e Histórico", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = OrangeNeon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Máquinas", "Tecnologia").forEach { aba ->
                    val sel = filtroCategoria == aba
                    val corAba = if (aba == "Tecnologia") CyanNeon else OrangeNeon
                    Button(
                        onClick = { filtroCategoria = aba },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sel) corAba else DarkSurface,
                            contentColor = if (sel) Color.White else TextSecondary
                        )
                    ) {
                        Text(aba, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Buscar por Nº OS, cliente, equipamento, telefone ou status") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OrangeNeon) }
            )

            Spacer(Modifier.height(12.dp))
            if (listaFiltrada.isEmpty()) {
                EstadoVazioIlustrado(
                    icone = Icons.Default.SearchOff,
                    titulo = if (ordens.isEmpty()) "Nenhuma ordem de serviço cadastrada" else "Nenhum resultado para a pesquisa",
                    subtitulo = "Inicie um novo atendimento na tela inicial para registrar máquinas ou equipamentos de TI."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(listaFiltrada) { os ->
                        val corCard = if (os.tipo == TipoAtendimento.MAQUINAS) OrangeNeon else CyanNeon
                        val corStatus = when (os.status) {
                            StatusOrdem.APROVADO, StatusOrdem.EM_EXECUCAO -> StatusGreen
                            StatusOrdem.CONCLUIDO, StatusOrdem.PRONTO_RETIRADA, StatusOrdem.ENTREGUE -> CyanNeon
                            StatusOrdem.REPROVADO, StatusOrdem.CANCELADO -> StatusRed
                            else -> OrangeNeon
                        }
                        val dataFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(os.dataCriacao))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, corCard.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (os.tipo == TipoAtendimento.MAQUINAS) Icons.Default.Build else Icons.Default.Computer,
                                            contentDescription = null,
                                            tint = corCard,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text("#${os.numeroOs.padStart(6, '0')} • ${os.nomeCliente}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            if (os.telefoneCliente.isNotBlank()) {
                                                Text("Tel: ${os.telefoneCliente}", color = corCard, fontSize = 12.sp)
                                            }
                                            Text(os.equipamento, color = TextPrimary, fontSize = 13.sp)
                                            Text(
                                                "$dataFmt  •  ${if (os.valorTotal > 0.0) CalculadoraOrcamento.formatarMoeda(os.valorTotal) else "Em análise / A Orçar"}",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(corStatus)
                                            .clickable { osParaEscolherStatus = os }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(os.status, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onEditarDadosClienteOs(os) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("Equipamentos", fontSize = 10.sp, color = TextPrimary)
                                    }

                                    OutlinedButton(
                                        onClick = { onEditarOrcarOs(os) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("Orçar / Fotos", fontSize = 10.sp, color = corCard)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            osParaFotoSaida = os
                                            val dir = File(context.filesDir, "fotos").apply { mkdirs() }
                                            val file = File(dir, "SAIDA_${System.currentTimeMillis()}.jpg")
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                            uriTempSaida = uri
                                            cameraSaidaLauncher.launch(uri)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("+ Foto Saída", fontSize = 10.sp, color = TextPrimary)
                                    }

                                    Button(
                                        onClick = { onVisualizarOuEnviarPdf(os) },
                                        colors = ButtonDefaults.buttonColors(containerColor = corCard),
                                        modifier = Modifier.weight(0.9f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("PDF", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        osParaEscolherStatus?.let { osSel ->
            AlertDialog(
                onDismissRequest = { osParaEscolherStatus = null },
                containerColor = DarkSurface,
                title = { Text("Alterar Status — OS #${osSel.numeroOs}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusOrdem.TODOS.forEach { st ->
                            val ativo = osSel.status == st
                            OutlinedButton(
                                onClick = {
                                    onMudarStatus(osSel, st)
                                    osParaEscolherStatus = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (ativo) OrangeNeon.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text(st, color = if (ativo) OrangeNeon else TextPrimary, fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { osParaEscolherStatus = null }) {
                        Text("Fechar", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun EstadoVazioIlustrado(icone: ImageVector, titulo: String, subtitulo: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icone, contentDescription = null, tint = OrangeNeon.copy(alpha = 0.7f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitulo, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    clientes: List<Cliente>,
    ordens: List<OrdemServico>,
    onVoltar: () -> Unit,
    onSalvarCliente: (Cliente, String) -> Unit,
    onNovaOsParaCliente: (Cliente) -> Unit = {}
) {
    var clienteEmEdicao by remember { mutableStateOf<Cliente?>(null) }
    var nomeOriginalAntesEdicao by remember { mutableStateOf("") }
    var buscaCliente by remember { mutableStateOf("") }

    val clientesFiltrados = clientes.filter {
        buscaCliente.isBlank() ||
                it.nome.contains(buscaCliente, ignoreCase = true) ||
                it.telefone.contains(buscaCliente)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clientes (${clientes.size})", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = CyanNeon)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        nomeOriginalAntesEdicao = ""
                        clienteEmEdicao = Cliente(nome = "", telefone = "")
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = CyanNeon)
                        Spacer(Modifier.width(4.dp))
                        Text("Novo", color = CyanNeon, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = buscaCliente,
                onValueChange = { buscaCliente = it },
                label = { Text("Pesquisar cliente por nome ou telefone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon) }
            )
            Spacer(Modifier.height(12.dp))

            if (clientesFiltrados.isEmpty()) {
                EstadoVazioIlustrado(
                    icone = Icons.Default.PeopleOutline,
                    titulo = "Nenhum cliente encontrado",
                    subtitulo = "Toque em 'Novo' no topo direito para cadastrar um cliente rapidamente."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(clientesFiltrados) { cli ->
                        val ordensDoCliente = ordens.filter {
                            (it.clienteId > 0 && it.clienteId == cli.id) || it.nomeCliente.equals(cli.nome, ignoreCase = true)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cli.nome, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        val telFmt = formatarTelefoneBr(cli.telefone)
                                        Text(
                                            text = if (telFmt.isNotBlank()) "WhatsApp: $telFmt" else "Sem telefone (Toque no lápis p/ adicionar)",
                                            color = if (telFmt.isNotBlank()) CyanNeon else OrangeNeon,
                                            fontSize = 13.sp
                                        )
                                        if (cli.endereco.isNotBlank()) {
                                            Text("Endereço: ${cli.endereco}", color = TextSecondary, fontSize = 12.sp)
                                        }
                                        Text("Histórico: ${ordensDoCliente.size} OS registrada(s)", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Row {
                                        IconButton(onClick = { onNovaOsParaCliente(cli) }) {
                                            Icon(Icons.Default.NoteAdd, contentDescription = "Nova OS", tint = OrangeNeon)
                                        }
                                        IconButton(onClick = {
                                            nomeOriginalAntesEdicao = cli.nome
                                            clienteEmEdicao = cli
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar Cliente", tint = CyanNeon)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        clienteEmEdicao?.let { cliAtual ->
            var nomeEdit by remember(cliAtual) { mutableStateOf(capitalizarIniciais(cliAtual.nome)) }
            var tel1Edit by remember(cliAtual) { mutableStateOf(cliAtual.telefone.filter { it.isDigit() }.take(11)) }
            var tel2Edit by remember(cliAtual) { mutableStateOf(cliAtual.telefoneSecundario.filter { it.isDigit() }.take(11)) }
            var endEdit by remember(cliAtual) { mutableStateOf(cliAtual.endereco) }

            AlertDialog(
                onDismissRequest = { clienteEmEdicao = null },
                containerColor = DarkSurface,
                title = { Text(if (cliAtual.id == 0) "Novo Cliente" else "Editar Cliente", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = nomeEdit,
                            onValueChange = { nomeEdit = capitalizarIniciais(it) },
                            label = { Text("Nome do Cliente") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = tel1Edit,
                            onValueChange = { tel1Edit = it.filter { c -> c.isDigit() }.take(11) },
                            label = { Text("Telefone / WhatsApp") },
                            visualTransformation = TelefoneBrVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = tel2Edit,
                            onValueChange = { tel2Edit = it.filter { c -> c.isDigit() }.take(11) },
                            label = { Text("2º Telefone (Opcional)") },
                            visualTransformation = TelefoneBrVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = endEdit,
                            onValueChange = { endEdit = capitalizarIniciais(it) },
                            label = { Text("Endereço") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nomeEdit.isNotBlank()) {
                                onSalvarCliente(
                                    cliAtual.copy(
                                        nome = capitalizarIniciais(nomeEdit.trim()),
                                        telefone = tel1Edit,
                                        telefoneSecundario = tel2Edit,
                                        endereco = endEdit.trim()
                                    ),
                                    nomeOriginalAntesEdicao
                                )
                                clienteEmEdicao = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { clienteEmEdicao = null }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// --- TELA DE CONFIGURAÇÕES E CENTRAL DE BACKUP (EXCEL + GOOGLE DRIVE + RESTAURAÇÃO) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    configAtual: ConfiguracaoOficina,
    onVoltar: () -> Unit,
    onSalvarConfig: (ConfiguracaoOficina) -> Unit,
    onExportarPlanilhaExcel: () -> Unit = {},
    onSalvarBackupGoogleDrive: () -> Unit = {},
    onRestaurarBackupZip: (Uri) -> Unit = {},
    onCarregarDemo: () -> Unit,
    onLimparBanco: () -> Unit
) {
    var nomeOficina by remember(configAtual) { mutableStateOf(configAtual.nomeOficina) }
    var whatsapp by remember(configAtual) { mutableStateOf(configAtual.whatsapp) }
    var cidadeUf by remember(configAtual) { mutableStateOf(configAtual.cidadeUf) }
    var garantiaPadrao by remember(configAtual) { mutableStateOf(configAtual.garantiaPadrao) }
    var observacoesPadrao by remember(configAtual) { mutableStateOf(configAtual.observacoesPadrao) }

    val restaurarZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uriEscolhida ->
        if (uriEscolhida != null) {
            onRestaurarBackupZip(uriEscolhida)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup, Excel e Configurações", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = OrangeNeon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CARTÃO DE SEGURANÇA E BACKUP (GOOGLE DRIVE + PLANILHA EXCEL + AUTO-BACKUP)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, StatusGreen, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Segurança e Backup Automático Ativo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "A cada OS criada ou editada, o backup e a planilha são atualizados automaticamente na pasta Downloads/NexusServicos_Backup do celular.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = onSalvarBackupGoogleDrive,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Salvar Backup Completo no Google Drive (.ZIP)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onExportarPlanilhaExcel,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar Planilha Excel de Clientes e OS (.CSV)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { restaurarZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = OrangeNeon)
                        Spacer(Modifier.width(8.dp))
                        Text("Restaurar Backup (.ZIP do Drive ou Downloads)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            Text("Dados da Oficina no PDF", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

            OutlinedTextField(value = nomeOficina, onValueChange = { nomeOficina = it }, label = { Text("Nome da Oficina") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp no PDF") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cidadeUf, onValueChange = { cidadeUf = it }, label = { Text("Endereço / Cidade - UF") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = garantiaPadrao, onValueChange = { garantiaPadrao = it }, label = { Text("Texto Padrão de Garantia") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = observacoesPadrao,
                onValueChange = { observacoesPadrao = it },
                label = { Text("Observações Padrão no PDF") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSalvarConfig(
                        configAtual.copy(
                            nomeOficina = nomeOficina,
                            whatsapp = whatsapp,
                            cidadeUf = cidadeUf,
                            garantiaPadrao = garantiaPadrao,
                            observacoesPadrao = observacoesPadrao
                        )
                    )
                    onVoltar()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon)
            ) {
                Text("Salvar Configurações", color = Color.White, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            Text("Ambiente de Demonstração / Limpeza", color = TextSecondary, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCarregarDemo, modifier = Modifier.weight(1f)) {
                    Text("Gerar OS Demo (4 Máquinas)", color = CyanNeon, fontSize = 11.sp)
                }
                OutlinedButton(onClick = onLimparBanco, modifier = Modifier.weight(1f)) {
                    Text("Apagar Dados Teste", color = StatusRed, fontSize = 11.sp)
                }
            }
        }
    }
}
// FIM DO ARQUIVO