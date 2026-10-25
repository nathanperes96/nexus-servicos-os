package com.nexus.servicos.os.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nexus.servicos.os.data.*
import com.nexus.servicos.os.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicosFotosScreen(
    rascunho: RascunhoOs,
    itensIniciais: List<ItemServico>,
    fotosIniciais: List<FotoOs>,
    descontoInicial: Double,
    onVoltar: (RascunhoOs, List<ItemServico>, List<FotoOs>, Double) -> Unit,
    onGerarPdfVisualizar: (RascunhoOs, List<ItemServico>, List<FotoOs>, Double, Double, Boolean) -> Unit
) {
    val context = LocalContext.current
    var tipoAtual by remember { mutableStateOf(rascunho.tipo) }
    val corPrincipal = if (tipoAtual == TipoAtendimento.MAQUINAS) OrangeNeon else CyanNeon

    var listaEquipamentos by remember {
        mutableStateOf(
            if (rascunho.equipamentos.isNotEmpty()) rascunho.equipamentos
            else listOf(EquipamentoDraft(numeroSequencial = 1))
        )
    }
    var indiceEquipAtivo by remember { mutableStateOf(0) }
    val equipAtivo = listaEquipamentos.getOrElse(indiceEquipAtivo) { listaEquipamentos.first() }
    val seqAtiva = equipAtivo.numeroSequencial

    var tipoItemSelecionado by remember { mutableStateOf("SERVICO") }
    var descricaoItem by remember { mutableStateOf("") }
    var quantidadeItem by remember { mutableStateOf("1") }
    var valorUnitarioItem by remember { mutableStateOf("") }
    var legendaFotoOpcional by remember { mutableStateOf("") }

    var descontoTexto by remember {
        mutableStateOf(if (descontoInicial > 0.0) String.format(Locale.US, "%.2f", descontoInicial) else "")
    }

    var itensAdicionados by remember { mutableStateOf(itensIniciais) }
    var fotosRegistradas by remember { mutableStateOf(fotosIniciais) }
    var indiceFotoViewer by remember { mutableStateOf<Int?>(null) }

    fun atualizarEquipAtivo(bloco: (EquipamentoDraft) -> EquipamentoDraft) {
        listaEquipamentos = listaEquipamentos.mapIndexed { idx, eq ->
            if (idx == indiceEquipAtivo) bloco(eq) else eq
        }
    }

    // Itens ativos apenas dos equipamentos que NÃO foram reprovados/cancelados
    val subtotalCentavos = itensAdicionados.sumOf { item ->
        val equipDesteItem = listaEquipamentos.firstOrNull { it.numeroSequencial == item.equipamentoNumeroSeq }
        val estaReprovado = equipDesteItem?.statusEquipamento == StatusOrdem.REPROVADO || equipDesteItem?.statusEquipamento == StatusOrdem.CANCELADO
        if (estaReprovado) 0L
        else {
            val unitCentavos = if (item.valorUnitarioCentavos > 0L) item.valorUnitarioCentavos else CalculadoraOrcamento.reaisParaCentavos(item.valor)
            CalculadoraOrcamento.calcularSubtotalItemCentavos(item.quantidade, unitCentavos)
        }
    }

    val descontoCentavos = CalculadoraOrcamento.textoParaCentavos(descontoTexto)
    val descontoValor = CalculadoraOrcamento.centavosParaReais(descontoCentavos)
    val totalLiquidoCentavos = (subtotalCentavos - descontoCentavos).coerceAtLeast(0L)
    val valorTotal = CalculadoraOrcamento.centavosParaReais(totalLiquidoCentavos)

    var uriTemporaria by remember { mutableStateOf<Uri?>(null) }
    var etapaCapturaAtual by remember { mutableStateOf("ENTRADA") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        val uri = uriTemporaria
        if (sucesso && uri != null) {
            fotosRegistradas = fotosRegistradas + FotoOs(
                osId = rascunho.osIdExistente,
                equipamentoNumeroSeq = seqAtiva,
                uriPath = uri.toString(),
                tipoFoto = etapaCapturaAtual,
                legenda = legendaFotoOpcional.trim(),
                dataRegistro = System.currentTimeMillis()
            )
            legendaFotoOpcional = ""
        }
    }

    val galeriaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uriEscolhida ->
        if (uriEscolhida != null) {
            try {
                val dir = File(context.filesDir, "fotos").apply { mkdirs() }
                val fileDestino = File(dir, "PICK_${etapaCapturaAtual}_EQ${seqAtiva}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uriEscolhida)?.use { input ->
                    FileOutputStream(fileDestino).use { output -> input.copyTo(output) }
                }
                val uriSegura = FileProvider.getUriForFile(context, "${context.packageName}.provider", fileDestino)
                fotosRegistradas = fotosRegistradas + FotoOs(
                    osId = rascunho.osIdExistente,
                    equipamentoNumeroSeq = seqAtiva,
                    uriPath = uriSegura.toString(),
                    tipoFoto = etapaCapturaAtual,
                    legenda = legendaFotoOpcional.trim(),
                    dataRegistro = System.currentTimeMillis()
                )
                legendaFotoOpcional = ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dispararCamera(etapa: String) {
        etapaCapturaAtual = etapa
        val dir = File(context.filesDir, "fotos").apply { mkdirs() }
        val file = File(dir, "FOTO_${etapa}_EQ${seqAtiva}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        uriTemporaria = uri
        cameraLauncher.launch(uri)
    }

    fun rascunhoAtualizado() = rascunho.copy(tipo = tipoAtual, equipamentos = listaEquipamentos)

    BackHandler {
        onVoltar(rascunhoAtualizado(), itensAdicionados, fotosRegistradas, descontoValor)
    }

    val sugestoesRapidas = if (tipoAtual == TipoAtendimento.MAQUINAS) {
        listOf("Limpeza do carburador" to 8000L, "Troca de vela" to 4000L, "Ajuste geral" to 6000L)
    } else {
        listOf("Limpeza + pasta térmica" to 12000L, "Instalação do sistema" to 12000L, "Drivers e antivírus" to 8000L)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fotos, Diagnóstico e Valores", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onVoltar(rascunhoAtualizado(), itensAdicionados, fotosRegistradas, descontoValor) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = corPrincipal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Abas de Seleção de Equipamento
            Text(
                "Selecione a máquina para diagnosticar, orçar ou recusar:",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listaEquipamentos.forEachIndexed { idx, eq ->
                    val sel = idx == indiceEquipAtivo
                    val ehReprovado = eq.statusEquipamento == StatusOrdem.REPROVADO
                    val corBordaAba = if (ehReprovado) StatusRed else corPrincipal
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) (if (ehReprovado) StatusRed else corPrincipal) else DarkSurface)
                            .border(1.dp, corBordaAba, RoundedCornerShape(10.dp))
                            .clickable { indiceEquipAtivo = idx }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (ehReprovado) "⛔ ${eq.rotulo()} (Recusado)" else eq.rotulo(),
                            color = if (sel) Color.White else (if (ehReprovado) StatusRed else TextPrimary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // SELEÇÃO DO STATUS DESTE EQUIPAMENTO (INCLUINDO REPROVADO / NÃO APROVADO)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (equipAtivo.statusEquipamento == StatusOrdem.REPROVADO) StatusRed.copy(alpha = 0.15f) else DarkSurface
                ),
                modifier = Modifier.fillMaxWidth().border(
                    1.dp,
                    if (equipAtivo.statusEquipamento == StatusOrdem.REPROVADO) StatusRed else BorderSubtle,
                    RoundedCornerShape(12.dp)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Status do ${equipAtivo.rotulo()}:",
                            color = if (equipAtivo.statusEquipamento == StatusOrdem.REPROVADO) StatusRed else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            equipAtivo.statusEquipamento,
                            color = if (equipAtivo.statusEquipamento == StatusOrdem.REPROVADO) StatusRed else corPrincipal,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            StatusOrdem.EM_ANALISE to "Em análise",
                            StatusOrdem.ORCAMENTO_ENVIADO to "Orçamento enviado",
                            StatusOrdem.APROVADO to "Aprovado",
                            StatusOrdem.REPROVADO to "⛔ Recusado / Negado",
                            StatusOrdem.EM_EXECUCAO to "Em execução",
                            StatusOrdem.CONCLUIDO to "Concluído"
                        ).forEach { (st, label) ->
                            val marcado = equipAtivo.statusEquipamento == st
                            val corChip = if (st == StatusOrdem.REPROVADO) StatusRed else corPrincipal
                            AssistChip(
                                onClick = { atualizarEquipAtivo { it.copy(statusEquipamento = st) } },
                                label = {
                                    Text(
                                        label,
                                        color = if (marcado) Color.White else (if (st == StatusOrdem.REPROVADO) StatusRed else TextSecondary),
                                        fontWeight = if (marcado) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (marcado) corChip else Color.Transparent
                                )
                            )
                        }
                    }

                    // NOVO CAMPO FUNDAMENTAL: Diagnóstico Técnico e Motivo de Recusa / Problema Constatado
                    OutlinedTextField(
                        value = equipAtivo.diagnostico,
                        onValueChange = { novo -> atualizarEquipAtivo { it.copy(diagnostico = novo) } },
                        label = {
                            Text(
                                if (equipAtivo.statusEquipamento == StatusOrdem.REPROVADO)
                                    "Motivo da Recusa / Problema Constatado no Teste *"
                                else "Diagnóstico Técnico / Problema Constatado após Desmontar/Testar"
                            )
                        },
                        placeholder = {
                            Text("Ex: Ao testar o equipamento, constatou-se motor queimado. Novo valor informado, porém cliente recusou o conserto.")
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = if (equipAtivo.statusEquipamento == StatusOrdem.REPROVADO) StatusRed else corPrincipal,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )
                }
            }

            // 1. Fotos do Equipamento Selecionado
            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fotosDesteEq = fotosRegistradas.filter { it.equipamentoNumeroSeq == seqAtiva }
                    Text(
                        "Fotos — ${equipAtivo.rotulo()} (${fotosDesteEq.size} nesta máquina)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = legendaFotoOpcional,
                        onValueChange = { legendaFotoOpcional = it },
                        label = { Text("Legenda opcional da próxima foto (Ex: Motor queimado, Tampa quebrada)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { dispararCamera("ENTRADA") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = corPrincipal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Entrada", color = TextPrimary, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { dispararCamera("ACESSORIO") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = corPrincipal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Acessório", color = TextPrimary, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { dispararCamera("SAIDA") },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Saída", color = TextPrimary, fontSize = 11.sp)
                        }
                        IconButton(
                            onClick = {
                                etapaCapturaAtual = "ENTRADA"
                                galeriaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeria", tint = corPrincipal)
                        }
                    }

                    if (fotosRegistradas.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(fotosRegistradas) { idxGlobal, foto ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(86.dp)
                                        .clickable { indiceFotoViewer = idxGlobal }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                1.5.dp,
                                                if (foto.tipoFoto == "SAIDA") StatusGreen else corPrincipal,
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        val bmp = remember(foto.uriPath) {
                                            try {
                                                val st = context.contentResolver.openInputStream(Uri.parse(foto.uriPath))
                                                val b = BitmapFactory.decodeStream(st)
                                                st?.close()
                                                b?.asImageBitmap()
                                            } catch (_: Exception) { null }
                                        }
                                        if (bmp != null) {
                                            Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                    }
                                    Text(
                                        "Eq.${foto.equipamentoNumeroSeq.toString().padStart(2, '0')} • ${foto.tipoFoto.take(4)}",
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Serviços e Peças
            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Orçamento — ${equipAtivo.rotulo()}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SERVICO" to "🔧 Serviço / Mão de Obra", "PECA" to "⚙ Peça / Componente").forEach { (cod, rot) ->
                            val sel = tipoItemSelecionado == cod
                            OutlinedButton(
                                onClick = { tipoItemSelecionado = cod },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (sel) corPrincipal.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text(rot, color = if (sel) corPrincipal else TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sugestoesRapidas.forEach { (nome, centavos) ->
                            AssistChip(
                                onClick = {
                                    itensAdicionados = itensAdicionados + ItemServico(
                                        osId = rascunho.osIdExistente,
                                        equipamentoNumeroSeq = seqAtiva,
                                        tipoItem = "SERVICO",
                                        descricao = nome,
                                        quantidade = 1,
                                        valorUnitarioCentavos = centavos,
                                        valor = CalculadoraOrcamento.centavosParaReais(centavos)
                                    )
                                },
                                label = { Text("+ ${nome.take(14)}", color = TextPrimary, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = descricaoItem,
                        onValueChange = { descricaoItem = it },
                        label = { Text("Descrição do Serviço ou Peça") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantidadeItem,
                            onValueChange = { quantidadeItem = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Qtd") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = valorUnitarioItem,
                            onValueChange = { valorUnitarioItem = it },
                            label = { Text("Valor Unitário (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            if (descricaoItem.isNotBlank() && valorUnitarioItem.isNotBlank()) {
                                val qtd = (quantidadeItem.toIntOrNull() ?: 1).coerceAtLeast(1)
                                val unitCentavos = CalculadoraOrcamento.textoParaCentavos(valorUnitarioItem)
                                val subCentavos = CalculadoraOrcamento.calcularSubtotalItemCentavos(qtd, unitCentavos)
                                itensAdicionados = itensAdicionados + ItemServico(
                                    osId = rascunho.osIdExistente,
                                    equipamentoNumeroSeq = seqAtiva,
                                    tipoItem = tipoItemSelecionado,
                                    descricao = descricaoItem.trim(),
                                    quantidade = qtd,
                                    valorUnitarioCentavos = unitCentavos,
                                    valor = CalculadoraOrcamento.centavosParaReais(subCentavos)
                                )
                                descricaoItem = ""
                                quantidadeItem = "1"
                                valorUnitarioItem = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = corPrincipal)
                    ) {
                        Text("+ Adicionar ao Equipamento ${seqAtiva.toString().padStart(2, '0')}", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (itensAdicionados.isNotEmpty()) {
                        HorizontalDivider(color = DarkBackground, thickness = 1.dp)
                        itensAdicionados.forEachIndexed { idx, item ->
                            val eqItem = listaEquipamentos.firstOrNull { it.numeroSequencial == item.equipamentoNumeroSeq }
                            val reprovado = eqItem?.statusEquipamento == StatusOrdem.REPROVADO

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "[Eq.${item.equipamentoNumeroSeq.toString().padStart(2, '0')}] ${item.quantidade}x ${item.descricao}",
                                        color = if (reprovado) TextSecondary else TextPrimary,
                                        fontSize = 13.sp
                                    )
                                    if (reprovado) {
                                        Text("(Equipamento Recusado — Não cobrado no total)", color = StatusRed, fontSize = 10.sp)
                                    }
                                }
                                Text(
                                    CalculadoraOrcamento.formatarMoeda(item.valor),
                                    color = if (reprovado) TextSecondary else corPrincipal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                IconButton(
                                    onClick = {
                                        itensAdicionados = itensAdicionados.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 3. Garantia
            Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Garantia — ${equipAtivo.rotulo()}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        if (equipAtivo.garantia.isBlank()) "Nenhuma selecionada (Toque para selecionar ou desmarcar)" else equipAtivo.garantia,
                        color = if (equipAtivo.garantia.isBlank()) TextSecondary else corPrincipal,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Sem garantia", "30 dias no serviço", "90 dias no serviço").forEach { op ->
                            val marcado = equipAtivo.garantia == op
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (marcado) corPrincipal else DarkBackground)
                                    .border(1.dp, if (marcado) corPrincipal else BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable {
                                        atualizarEquipAtivo { it.copy(garantia = if (marcado) "" else op) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = op.replace(" no serviço", ""),
                                    color = if (marcado) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (marcado) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Desconto e Total Geral da OS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = descontoTexto,
                    onValueChange = { descontoTexto = it },
                    label = { Text("Desconto (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(150.dp),
                    singleLine = true
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Aprovado a Pagar", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = CalculadoraOrcamento.formatarMoedaCentavos(totalLiquidoCentavos),
                        color = corPrincipal,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    onGerarPdfVisualizar(rascunhoAtualizado(), itensAdicionados, fotosRegistradas, descontoValor, valorTotal, false)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corPrincipal)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (itensAdicionados.isEmpty()) "Gerar PDF de Entrada" else "Gerar PDF Completo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = {
                    onGerarPdfVisualizar(rascunhoAtualizado(), itensAdicionados, fotosRegistradas, descontoValor, valorTotal, true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.5.dp, corPrincipal, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = WhatsAppGreen)
                Spacer(Modifier.width(8.dp))
                Text("Enviar via WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }

    indiceFotoViewer?.let { idx ->
        val fotoAtual = fotosRegistradas.getOrNull(idx)
        if (fotoAtual != null) {
            val bmpFull = remember(fotoAtual.uriPath) {
                try {
                    val st = context.contentResolver.openInputStream(Uri.parse(fotoAtual.uriPath))
                    val b = BitmapFactory.decodeStream(st)
                    st?.close()
                    b?.asImageBitmap()
                } catch (_: Exception) { null }
            }

            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).clickable {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Eq.${fotoAtual.equipamentoNumeroSeq.toString().padStart(2, '0')} (${fotoAtual.tipoFoto}) — ${idx + 1}/${fotosRegistradas.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { indiceFotoViewer = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (bmpFull != null) {
                            Image(bitmap = bmpFull, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        }
                        if (idx > 0) {
                            IconButton(
                                onClick = { indiceFotoViewer = idx - 1 },
                                modifier = Modifier.align(Alignment.CenterStart).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                        if (idx < fotosRegistradas.size - 1) {
                            IconButton(
                                onClick = { indiceFotoViewer = idx + 1 },
                                modifier = Modifier.align(Alignment.CenterEnd).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Próxima", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            fotosRegistradas = fotosRegistradas.toMutableList().also { it.removeAt(idx) }
                            indiceFotoViewer = if (fotosRegistradas.isEmpty()) null else (idx - 1).coerceAtLeast(0)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Excluir esta fotografia", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
// FIM DO ARQUIVO