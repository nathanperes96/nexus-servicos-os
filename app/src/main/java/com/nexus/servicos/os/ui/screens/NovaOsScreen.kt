package com.nexus.servicos.os.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.servicos.os.data.*
import com.nexus.servicos.os.ui.theme.*

class TelefoneBrVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(11)
        val out = StringBuilder()
        val originalToTransformed = IntArray(digits.length + 1)
        val transformedToOriginal = mutableListOf<Int>()

        for (i in digits.indices) {
            if (i == 0) {
                out.append("(")
                transformedToOriginal.add(0)
            }
            if (i == 2) {
                out.append(") ")
                transformedToOriginal.add(2)
                transformedToOriginal.add(2)
            }
            val posTraco = if (digits.length <= 10) 6 else 7
            if (i == posTraco) {
                out.append("-")
                transformedToOriginal.add(i)
            }
            originalToTransformed[i] = out.length
            out.append(digits[i])
            transformedToOriginal.add(i + 1)
        }
        originalToTransformed[digits.length] = out.length
        transformedToOriginal.add(digits.length)

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, digits.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginal[offset.coerceIn(0, transformedToOriginal.size - 1)]
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaOsScreen(
    rascunhoInicial: RascunhoOs,
    clientesSalvos: List<Cliente>,
    onVoltar: (RascunhoOs) -> Unit,
    onAvancar: (RascunhoOs) -> Unit
) {
    var tipoSelecionado by remember { mutableStateOf(rascunhoInicial.tipo) }
    var clienteIdSelecionado by remember { mutableStateOf(rascunhoInicial.clienteIdSelecionado) }
    var nomeCliente by remember { mutableStateOf(capitalizarIniciais(rascunhoInicial.nomeCliente)) }
    var telPrincipal by remember { mutableStateOf(rascunhoInicial.telefonePrincipal.filter { it.isDigit() }.take(11)) }
    var telSecundario by remember { mutableStateOf(rascunhoInicial.telefoneSecundario.filter { it.isDigit() }.take(11)) }
    var mostrarSegundoTel by remember { mutableStateOf(rascunhoInicial.telefoneSecundario.isNotBlank()) }
    var endereco by remember { mutableStateOf(rascunhoInicial.endereco) }
    var observacoesGerais by remember { mutableStateOf(rascunhoInicial.observacoesGerais) }
    var mostrarListaClientes by remember { mutableStateOf(false) }

    // Lista de múltiplos equipamentos na mesma OS
    var listaEquipamentos by remember {
        mutableStateOf(
            if (rascunhoInicial.equipamentos.isNotEmpty()) rascunhoInicial.equipamentos
            else listOf(EquipamentoDraft(numeroSequencial = 1))
        )
    }
    var indiceEquipAtivo by remember { mutableStateOf(0) }

    val equipAtual = listaEquipamentos.getOrElse(indiceEquipAtivo) { listaEquipamentos.first() }

    fun atualizarEquipamentoAtivo(bloco: (EquipamentoDraft) -> EquipamentoDraft) {
        listaEquipamentos = listaEquipamentos.mapIndexed { index, item ->
            if (index == indiceEquipAtivo) bloco(item) else item
        }
    }

    val corPrincipal = if (tipoSelecionado == TipoAtendimento.MAQUINAS) OrangeNeon else CyanNeon
    val coresCampo = TextFieldDefaults.colors(
        focusedContainerColor = DarkSurface,
        unfocusedContainerColor = DarkSurface,
        focusedIndicatorColor = corPrincipal,
        focusedLabelColor = corPrincipal,
        unfocusedTextColor = TextPrimary,
        focusedTextColor = TextPrimary
    )

    // 6 ilustrações para Máquinas e 6 opções para Tecnologia (Incluindo Cortador de Grama, Computador, Notebook e Celular)
    val opcoesServico = if (tipoSelecionado == TipoAtendimento.MAQUINAS) {
        listOf("Motosserra", "Roçadeira", "Cortador de grama", "Gerador", "Compressor", "Ferramentas Elétricas")
    } else {
        listOf("Notebook", "Computador", "Celular", "Formatação", "Redes / Wi-Fi", "Outro")
    }

    fun construirRascunhoAtual() = rascunhoInicial.copy(
        tipo = tipoSelecionado,
        clienteIdSelecionado = clienteIdSelecionado,
        nomeCliente = capitalizarIniciais(nomeCliente.trim()),
        telefonePrincipal = telPrincipal,
        telefoneSecundario = telSecundario,
        endereco = endereco,
        observacoesGerais = observacoesGerais,
        equipamentos = listaEquipamentos
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (rascunhoInicial.osIdExistente > 0) "Editar OS #${rascunhoInicial.numeroOsExistente}" else "Nova Ordem de Serviço",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onVoltar(construirRascunhoAtual()) }) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Seletor de Categoria (Máquinas vs Tecnologia)
            Row(modifier = Modifier.fillMaxWidth()) {
                BotaoSelecao(
                    texto = "⚙ Máquinas",
                    selecionado = tipoSelecionado == TipoAtendimento.MAQUINAS,
                    corAtiva = OrangeNeon,
                    modifier = Modifier.weight(1f)
                ) { tipoSelecionado = TipoAtendimento.MAQUINAS }

                Spacer(Modifier.width(8.dp))

                BotaoSelecao(
                    texto = "💻 Tecnologia",
                    selecionado = tipoSelecionado == TipoAtendimento.TECNOLOGIA,
                    corAtiva = CyanNeon,
                    modifier = Modifier.weight(1f)
                ) { tipoSelecionado = TipoAtendimento.TECNOLOGIA }
            }

            // 1. Dados do Cliente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1. Dados do Cliente", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (clientesSalvos.isNotEmpty()) {
                    TextButton(onClick = { mostrarListaClientes = !mostrarListaClientes }) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = corPrincipal)
                        Spacer(Modifier.width(4.dp))
                        Text("Clientes salvos (${clientesSalvos.size})", color = corPrincipal, fontSize = 12.sp)
                    }
                }
            }

            if (mostrarListaClientes) {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        clientesSalvos.take(10).forEach { cli ->
                            TextButton(
                                onClick = {
                                    clienteIdSelecionado = cli.id
                                    nomeCliente = capitalizarIniciais(cli.nome)
                                    telPrincipal = cli.telefone.filter { it.isDigit() }.take(11)
                                    telSecundario = cli.telefoneSecundario.filter { it.isDigit() }.take(11)
                                    mostrarSegundoTel = telSecundario.isNotBlank()
                                    endereco = cli.endereco
                                    mostrarListaClientes = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${cli.nome} • ${formatarTelefoneBr(cli.telefone).ifBlank { "Sem telefone" }}", color = TextPrimary)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = nomeCliente,
                onValueChange = { nomeCliente = capitalizarIniciais(it) },
                label = { Text("Nome do Cliente *") },
                placeholder = { Text("Ex: João Silva") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = coresCampo
            )

            OutlinedTextField(
                value = telPrincipal,
                onValueChange = { novo -> telPrincipal = novo.filter { it.isDigit() }.take(11) },
                label = { Text("Telefone / WhatsApp *") },
                placeholder = { Text("DDD + número do celular") },
                visualTransformation = TelefoneBrVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Text("${telPrincipal.length}/11", color = TextSecondary, fontSize = 10.sp)
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(WhatsAppGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = coresCampo
            )

            if (!mostrarSegundoTel) {
                TextButton(onClick = { mostrarSegundoTel = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = corPrincipal)
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar 2º telefone / WhatsApp (opcional)", color = corPrincipal, fontSize = 12.sp)
                }
            } else {
                OutlinedTextField(
                    value = telSecundario,
                    onValueChange = { novo -> telSecundario = novo.filter { it.isDigit() }.take(11) },
                    label = { Text("2º Telefone (DDD + 8 ou 9 dígitos)") },
                    visualTransformation = TelefoneBrVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = coresCampo
                )
            }

            OutlinedTextField(
                value = endereco,
                onValueChange = { endereco = capitalizarIniciais(it) },
                label = { Text("Endereço (Opcional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = coresCampo
            )

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // 2. MÚLTIPLOS EQUIPAMENTOS NA MESMA OS (Requisito 9)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "2. Equipamentos da OS (${listaEquipamentos.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Cadastre 1, 4 ou mais máquinas na mesma Ordem",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = {
                        val proxSeq = (listaEquipamentos.maxOfOrNull { it.numeroSequencial } ?: 0) + 1
                        listaEquipamentos = listaEquipamentos + EquipamentoDraft(numeroSequencial = proxSeq)
                        indiceEquipAtivo = listaEquipamentos.lastIndex
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = corPrincipal),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Equipamento", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Abas horizontais para alternar entre Equipamento 01, Equipamento 02, Equipamento 03, Equipamento 04...
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listaEquipamentos.forEachIndexed { idx, eq ->
                    val selecionado = idx == indiceEquipAtivo
                    val seqFmt = eq.numeroSequencial.toString().padStart(2, '0')
                    val nomeCurto = eq.tipoEquipamento.ifBlank { eq.marcaModelo.ifBlank { "Máquina" } }.take(12)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selecionado) corPrincipal else DarkSurface)
                            .border(1.dp, corPrincipal, RoundedCornerShape(10.dp))
                            .clickable { indiceEquipAtivo = idx }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Eq. $seqFmt: $nomeCurto",
                            color = if (selecionado) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (listaEquipamentos.size > 1) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remover Equipamento",
                                tint = if (selecionado) Color.White else TextSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        val novaLista = listaEquipamentos.toMutableList().also { it.removeAt(idx) }
                                        listaEquipamentos = novaLista
                                        indiceEquipAtivo = indiceEquipAtivo.coerceIn(0, novaLista.lastIndex)
                                    }
                            )
                        }
                    }
                }
            }

            // Grelha 3x2 com Ilustrações Vetoriais para o Equipamento Selecionado
            Text(
                "Tipo do Equipamento ${equipAtual.numeroSequencial.toString().padStart(2, '0')}:",
                color = corPrincipal,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                opcoesServico.chunked(3).forEach { linha ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        linha.forEach { nomeCat ->
                            val ativo = equipAtual.tipoEquipamento == nomeCat
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (ativo) corPrincipal.copy(alpha = 0.2f) else DarkSurface)
                                    .border(
                                        1.5.dp,
                                        if (ativo) corPrincipal else corPrincipal.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        atualizarEquipamentoAtivo { eq ->
                                            eq.copy(
                                                tipoEquipamento = nomeCat,
                                                marcaModelo = if (eq.marcaModelo.isBlank() && nomeCat != "Outro") "$nomeCat " else eq.marcaModelo
                                            )
                                        }
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconeServicoCustom(nome = nomeCat, cor = corPrincipal, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = nomeCat,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = equipAtual.marcaModelo,
                onValueChange = { novo -> atualizarEquipamentoAtivo { it.copy(marcaModelo = capitalizarIniciais(novo)) } },
                label = {
                    Text(
                        if (tipoSelecionado == TipoAtendimento.MAQUINAS) "Marca / Modelo (Ex: Motosserra Stihl MS 382)"
                        else "Marca / Modelo (Ex: Notebook Dell Inspiron 15)"
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = coresCampo
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = equipAtual.numeroSerie,
                    onValueChange = { novo -> atualizarEquipamentoAtivo { it.copy(numeroSerie = novo) } },
                    label = { Text("Nº de Série / IMEI") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = coresCampo
                )
                OutlinedTextField(
                    value = equipAtual.estadoFisico,
                    onValueChange = { novo -> atualizarEquipamentoAtivo { it.copy(estadoFisico = capitalizarIniciais(novo)) } },
                    label = { Text("Estado Físico Entrada") },
                    placeholder = { Text("Ex: Usado, riscos") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = coresCampo
                )
            }

            OutlinedTextField(
                value = equipAtual.defeitoRelatado,
                onValueChange = { novo -> atualizarEquipamentoAtivo { it.copy(defeitoRelatado = novo) } },
                label = { Text("Defeito relatado para o Equipamento ${equipAtual.numeroSequencial.toString().padStart(2, '0')}") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = coresCampo
            )

            OutlinedTextField(
                value = equipAtual.acessorios,
                onValueChange = { novo -> atualizarEquipamentoAtivo { it.copy(acessorios = novo) } },
                label = {
                    Text(
                        if (tipoSelecionado == TipoAtendimento.MAQUINAS) "Acessórios deste equipamento (Ex: Sabre, corrente, capa)"
                        else "Acessórios deste equipamento (Ex: Fonte, cabo, bolsa)"
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = coresCampo
            )

            OutlinedTextField(
                value = observacoesGerais,
                onValueChange = { observacoesGerais = it },
                label = { Text("Observações gerais da nota (Opcional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = coresCampo
            )

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { onAvancar(construirRascunhoAtual()) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corPrincipal)
            ) {
                Text(
                    "Continuar para Fotos e Orçamento (${listaEquipamentos.size} Eq.)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// --- RECURSOS GRÁFICOS VETORIAIS NATIVOS (Seção 6: Logotipo, Máquinas, Tecnologia e Estados Vazios) ---
@Composable
fun LogoNexusVetorial(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centro = Offset(w / 2f, h / 2f)
        val raio = minOf(w, h) * 0.42f

        // Semicírculo Esquerdo: Engrenagem Mecânica Laranja
        drawArc(
            color = OrangeNeon,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centro.x - raio, centro.y - raio),
            size = Size(raio * 2, raio * 2),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
        // Semicírculo Direito: Circuito Tecnológico Ciano
        drawArc(
            color = CyanNeon,
            startAngle = 270f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centro.x - raio, centro.y - raio),
            size = Size(raio * 2, raio * 2),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
        // "N" Centralizado
        val pathN = Path().apply {
            moveTo(w * 0.36f, h * 0.66f)
            lineTo(w * 0.36f, h * 0.34f)
            lineTo(w * 0.64f, h * 0.66f)
            lineTo(w * 0.64f, h * 0.34f)
        }
        drawPath(pathN, color = Color.White, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconeServicoCustom(nome: String, cor: Color, modifier: Modifier = Modifier) {
    when (nome) {
        "Motosserra" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.44f, h * 0.42f),
                    size = Size(w * 0.52f, h * 0.20f),
                    cornerRadius = CornerRadius(h * 0.10f, h * 0.10f),
                    style = stroke
                )
                for (i in 0..3) {
                    val x = w * (0.52f + i * 0.10f)
                    drawLine(cor, Offset(x, h * 0.42f), Offset(x + w * 0.03f, h * 0.35f), strokeWidth = w * 0.05f)
                    drawLine(cor, Offset(x, h * 0.62f), Offset(x - w * 0.03f, h * 0.69f), strokeWidth = w * 0.05f)
                }
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.16f, h * 0.30f),
                    size = Size(w * 0.30f, h * 0.40f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                    style = stroke
                )
                drawLine(cor, Offset(w * 0.24f, h * 0.30f), Offset(w * 0.24f, h * 0.16f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                drawLine(cor, Offset(w * 0.24f, h * 0.16f), Offset(w * 0.40f, h * 0.16f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                drawLine(cor, Offset(w * 0.16f, h * 0.52f), Offset(w * 0.04f, h * 0.64f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
            }
        }
        "Roçadeira" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawLine(cor, Offset(w * 0.16f, h * 0.84f), Offset(w * 0.80f, h * 0.20f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.70f, h * 0.08f),
                    size = Size(w * 0.24f, h * 0.22f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
                    style = stroke
                )
                val pathGuidao = Path().apply {
                    moveTo(w * 0.28f, h * 0.32f)
                    lineTo(w * 0.48f, h * 0.52f)
                    lineTo(w * 0.68f, h * 0.62f)
                }
                drawPath(pathGuidao, color = cor, style = stroke)
                drawCircle(color = cor, radius = w * 0.11f, center = Offset(w * 0.16f, h * 0.84f), style = stroke)
            }
        }
        "Cortador de grama" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                // Chassi do carrinho cortador
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.14f, h * 0.52f),
                    size = Size(w * 0.64f, h * 0.22f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
                    style = stroke
                )
                // Motor superior
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.30f, h * 0.34f),
                    size = Size(w * 0.28f, h * 0.18f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = stroke
                )
                // Guidão de empurrar
                drawLine(cor, Offset(w * 0.68f, h * 0.52f), Offset(w * 0.88f, h * 0.16f), strokeWidth = w * 0.075f, cap = StrokeCap.Round)
                drawLine(cor, Offset(w * 0.78f, h * 0.16f), Offset(w * 0.94f, h * 0.16f), strokeWidth = w * 0.075f, cap = StrokeCap.Round)
                // Duas rodas
                drawCircle(color = cor, radius = w * 0.10f, center = Offset(w * 0.26f, h * 0.80f), style = stroke)
                drawCircle(color = cor, radius = w * 0.10f, center = Offset(w * 0.66f, h * 0.80f), style = stroke)
            }
        }
        "Compressor" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.12f, h * 0.42f),
                    size = Size(w * 0.78f, h * 0.30f),
                    cornerRadius = CornerRadius(h * 0.15f, h * 0.15f),
                    style = stroke
                )
                drawCircle(color = cor, radius = w * 0.10f, center = Offset(w * 0.28f, h * 0.83f), style = stroke)
                drawLine(cor, Offset(w * 0.74f, h * 0.72f), Offset(w * 0.74f, h * 0.88f), strokeWidth = w * 0.075f, cap = StrokeCap.Round)
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.24f, h * 0.20f),
                    size = Size(w * 0.26f, h * 0.22f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = stroke
                )
                drawCircle(color = cor, radius = w * 0.10f, center = Offset(w * 0.70f, h * 0.22f), style = stroke)
            }
        }
        "Gerador" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.62f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = stroke
                )
                val raio = Path().apply {
                    moveTo(w * 0.54f, h * 0.32f)
                    lineTo(w * 0.38f, h * 0.54f)
                    lineTo(w * 0.52f, h * 0.54f)
                    lineTo(w * 0.44f, h * 0.74f)
                }
                drawPath(raio, color = cor, style = stroke)
            }
        }
        "Ferramentas Elétricas" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.12f, h * 0.20f),
                    size = Size(w * 0.50f, h * 0.28f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                    style = stroke
                )
                drawLine(cor, Offset(w * 0.62f, h * 0.34f), Offset(w * 0.94f, h * 0.34f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                drawLine(cor, Offset(w * 0.26f, h * 0.48f), Offset(w * 0.20f, h * 0.80f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawLine(cor, Offset(w * 0.42f, h * 0.48f), Offset(w * 0.36f, h * 0.80f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
            }
        }
        "Notebook" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.16f, h * 0.20f),
                    size = Size(w * 0.68f, h * 0.46f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
                    style = stroke
                )
                drawLine(cor, Offset(w * 0.08f, h * 0.76f), Offset(w * 0.92f, h * 0.76f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
            }
        }
        "Computador" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                // Monitor
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.08f, h * 0.22f),
                    size = Size(w * 0.52f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = stroke
                )
                drawLine(cor, Offset(w * 0.34f, h * 0.64f), Offset(w * 0.34f, h * 0.80f), strokeWidth = w * 0.07f)
                drawLine(cor, Offset(w * 0.20f, h * 0.80f), Offset(w * 0.48f, h * 0.80f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
                // Gabinete Desktop
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.68f, h * 0.18f),
                    size = Size(w * 0.24f, h * 0.62f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = stroke
                )
                drawCircle(color = cor, radius = w * 0.03f, center = Offset(w * 0.80f, h * 0.30f))
            }
        }
        "Celular" -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.28f, h * 0.12f),
                    size = Size(w * 0.44f, h * 0.76f),
                    cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
                    style = stroke
                )
                drawCircle(color = cor, radius = w * 0.035f, center = Offset(w * 0.50f, h * 0.78f))
            }
        }
        "Formatação" -> Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = cor, modifier = modifier)
        "Redes / Wi-Fi" -> Icon(Icons.Default.Router, contentDescription = null, tint = cor, modifier = modifier)
        else -> Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = cor, modifier = modifier)
    }
}

@Composable
fun BotaoSelecao(texto: String, selecionado: Boolean, corAtiva: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selecionado) corAtiva else DarkSurface,
            contentColor = if (selecionado) Color.White else TextSecondary
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(texto, fontWeight = FontWeight.Bold)
    }
}