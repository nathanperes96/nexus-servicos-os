package com.nexus.servicos.os

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.servicos.os.data.*
import com.nexus.servicos.os.ui.screens.*
import com.nexus.servicos.os.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.nexusDao()

        setContent {
            NexusServiçosOSTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    val listaOrdens by dao.buscarTodasOs().collectAsState(initial = emptyList())
                    val listaClientes by dao.buscarTodosClientes().collectAsState(initial = emptyList())
                    val configSalva by dao.buscarConfiguracao().collectAsState(initial = ConfiguracaoOficina())
                    val configAtual = configSalva ?: ConfiguracaoOficina()

                    var rascunhoAtual by remember { mutableStateOf(RascunhoOs()) }
                    var itensAtuais by remember { mutableStateOf(listOf<ItemServico>()) }
                    var fotosAtuais by remember { mutableStateOf(listOf<FotoOs>()) }
                    var descontoAtual by remember { mutableStateOf(0.0) }

                    var ultimoArquivoPdf by remember { mutableStateOf<File?>(null) }
                    var ultimoNumeroOsPdf by remember { mutableStateOf("000001") }
                    var ultimoClientePdf by remember { mutableStateOf("") }
                    var ultimoTipoPdf by remember { mutableStateOf(TipoAtendimento.MAQUINAS) }

                    // Estados reais do Compressor: Mensagem + Progresso (0.0f até 1.0f sem zerar no meio)
                    var mensagemCarregando by remember { mutableStateOf<String?>(null) }
                    var progressoCarregando by remember { mutableFloatStateOf(0f) }

                    suspend fun carregarOsParaRascunho(os: OrdemServico) {
                        progressoCarregando = 0.25f
                        val equipsBanco = withContext(Dispatchers.IO) { dao.buscarEquipamentosDaOs(os.id) }
                        progressoCarregando = 0.55f
                        val itensSalvos = withContext(Dispatchers.IO) { dao.buscarItensDaOs(os.id) }
                        val fotosSalvas = withContext(Dispatchers.IO) { dao.buscarFotosDaOs(os.id) }
                        progressoCarregando = 0.85f

                        val partesTel = os.telefoneCliente.split("/")
                        val tel1 = partesTel.getOrNull(0)?.filter { it.isDigit() } ?: ""
                        val tel2 = partesTel.getOrNull(1)?.filter { it.isDigit() } ?: ""

                        val draftsEquip = if (equipsBanco.isNotEmpty()) {
                            equipsBanco.map { eq ->
                                EquipamentoDraft(
                                    idBanco = eq.id,
                                    numeroSequencial = eq.numeroSequencial,
                                    tipoEquipamento = eq.tipoEquipamento,
                                    marcaModelo = eq.marcaModelo,
                                    numeroSerie = eq.numeroSerie,
                                    defeitoRelatado = eq.defeitoRelatado,
                                    estadoFisico = eq.estadoFisico,
                                    acessorios = eq.acessorios,
                                    diagnostico = eq.diagnostico,
                                    observacoes = eq.observacoes,
                                    garantia = eq.garantia,
                                    statusEquipamento = eq.statusEquipamento
                                )
                            }
                        } else {
                            listOf(
                                EquipamentoDraft(
                                    numeroSequencial = 1,
                                    tipoEquipamento = os.categoriaEquipamento,
                                    marcaModelo = os.equipamento,
                                    numeroSerie = os.numeroSerie,
                                    defeitoRelatado = os.defeitoRelatado,
                                    acessorios = os.acessorios,
                                    garantia = os.garantia,
                                    statusEquipamento = os.status
                                )
                            )
                        }

                        rascunhoAtual = RascunhoOs(
                            osIdExistente = os.id,
                            numeroOsExistente = os.numeroOs,
                            dataCriacaoExistente = os.dataCriacao,
                            dataOrcamentoExistente = os.dataOrcamento,
                            dataSaidaExistente = os.dataSaida,
                            statusExistente = os.status,
                            clienteIdSelecionado = os.clienteId,
                            tipo = os.tipo,
                            nomeCliente = capitalizarIniciais(os.nomeCliente),
                            telefonePrincipal = tel1,
                            telefoneSecundario = tel2,
                            endereco = os.enderecoCliente,
                            observacoesGerais = os.observacoes,
                            equipamentos = draftsEquip
                        )
                        itensAtuais = itensSalvos
                        fotosAtuais = fotosSalvas
                        descontoAtual = os.desconto
                        progressoCarregando = 1.0f
                    }

                    suspend fun persistirOsAgora(
                        rasc: RascunhoOs,
                        itens: List<ItemServico>,
                        fotos: List<FotoOs>,
                        desc: Double,
                        faixaInicio: Float = 0.05f,
                        faixaFim: Float = 1.0f
                    ): RascunhoOs = withContext(Dispatchers.IO) {
                        fun atualizarFaixa(p: Float) {
                            progressoCarregando = faixaInicio + (faixaFim - faixaInicio) * p.coerceIn(0f, 1f)
                        }

                        atualizarFaixa(0.10f)
                        val nomeFormatado = capitalizarIniciais(rasc.nomeCliente.trim())
                        val telFmt1 = formatarTelefoneBr(rasc.telefonePrincipal)
                        val telFmt2 = formatarTelefoneBr(rasc.telefoneSecundario)
                        val telefones = if (telFmt2.isNotBlank()) "$telFmt1 / $telFmt2" else telFmt1

                        val editando = rasc.osIdExistente > 0
                        val totalOrdens = dao.contarOrdens()
                        val numeroOsFinal = if (editando && rasc.numeroOsExistente.isNotBlank()) {
                            rasc.numeroOsExistente
                        } else {
                            (totalOrdens + 1).toString().padStart(6, '0')
                        }

                        val agora = System.currentTimeMillis()
                        val dataEntradaFinal = if (editando && rasc.dataCriacaoExistente > 0L) rasc.dataCriacaoExistente else agora
                        val dataOrcFinal = if (itens.isNotEmpty()) {
                            if (rasc.dataOrcamentoExistente > 0L) rasc.dataOrcamentoExistente else agora
                        } else 0L
                        val temFotoSaida = fotos.any { it.tipoFoto == "SAIDA" }
                        val dataSaiFinal = if (temFotoSaida) {
                            if (rasc.dataSaidaExistente > 0L) rasc.dataSaidaExistente else agora
                        } else rasc.dataSaidaExistente

                        val descCentavos = CalculadoraOrcamento.reaisParaCentavos(desc)
                        val itensAtivos = itens.filter { item ->
                            val eq = rasc.equipamentos.firstOrNull { it.numeroSequencial == item.equipamentoNumeroSeq }
                            eq?.statusEquipamento != StatusOrdem.REPROVADO && eq?.statusEquipamento != StatusOrdem.CANCELADO
                        }
                        val totalCentavos = CalculadoraOrcamento.calcularTotalLiquidoCentavos(itensAtivos, descCentavos)
                        val totalCalculado = CalculadoraOrcamento.centavosParaReais(totalCentavos)

                        val statusConsolidado = CalculadoraOrcamento.consolidarStatusOs(rasc.statusExistente, rasc.equipamentos)

                        var idClienteReal = rasc.clienteIdSelecionado
                        if (nomeFormatado.isNotBlank()) {
                            val clienteExistente = listaClientes.firstOrNull {
                                (idClienteReal > 0 && it.id == idClienteReal) || it.nome.equals(nomeFormatado, ignoreCase = true)
                            }
                            idClienteReal = dao.inserirCliente(
                                Cliente(
                                    id = clienteExistente?.id ?: 0,
                                    nome = nomeFormatado,
                                    telefone = rasc.telefonePrincipal,
                                    telefoneSecundario = rasc.telefoneSecundario,
                                    endereco = rasc.endereco
                                )
                            ).toInt()
                        }
                        atualizarFaixa(0.25f)

                        val resumoEquipamentos = if (rasc.equipamentos.size == 1) {
                            val unico = rasc.equipamentos.first()
                            unico.marcaModelo.ifBlank { unico.tipoEquipamento.ifBlank { "Equipamento" } }
                        } else {
                            "${rasc.equipamentos.size} Equipamentos: " + rasc.equipamentos.joinToString(", ") {
                                it.marcaModelo.ifBlank { it.tipoEquipamento.ifBlank { "Máquina" } }
                            }
                        }

                        val primeiroEq = rasc.equipamentos.firstOrNull() ?: EquipamentoDraft()

                        val osEntidade = OrdemServico(
                            id = if (editando) rasc.osIdExistente else 0,
                            numeroOs = numeroOsFinal,
                            clienteId = idClienteReal,
                            nomeCliente = nomeFormatado.ifBlank { "Cliente sem nome" },
                            telefoneCliente = telefones,
                            enderecoCliente = rasc.endereco,
                            tipo = rasc.tipo,
                            categoriaEquipamento = primeiroEq.tipoEquipamento,
                            equipamento = resumoEquipamentos,
                            numeroSerie = primeiroEq.numeroSerie,
                            defeitoRelatado = primeiroEq.defeitoRelatado,
                            acessorios = primeiroEq.acessorios,
                            garantia = primeiroEq.garantia,
                            observacoes = rasc.observacoesGerais,
                            status = statusConsolidado,
                            dataCriacao = dataEntradaFinal,
                            dataOrcamento = dataOrcFinal,
                            dataSaida = dataSaiFinal,
                            desconto = desc,
                            valorTotal = totalCalculado
                        )

                        val idReal = if (editando) {
                            dao.atualizarOs(osEntidade)
                            dao.deletarEquipamentosDaOs(rasc.osIdExistente)
                            dao.deletarItensDaOs(rasc.osIdExistente)
                            dao.deletarFotosDaOs(rasc.osIdExistente)
                            rasc.osIdExistente
                        } else {
                            val novoId = dao.inserirOs(osEntidade).toInt()
                            dao.inserirHistoricoStatus(
                                HistoricoStatus(
                                    osId = novoId,
                                    statusAnterior = "",
                                    statusNovo = statusConsolidado,
                                    descricao = "Ordem de Serviço aberta com ${rasc.equipamentos.size} equipamento(s)."
                                )
                            )
                            novoId
                        }
                        atualizarFaixa(0.40f)

                        rasc.equipamentos.forEach { eq ->
                            dao.inserirEquipamento(
                                EquipamentoOs(
                                    osId = idReal,
                                    numeroSequencial = eq.numeroSequencial,
                                    tipoEquipamento = eq.tipoEquipamento,
                                    marcaModelo = eq.marcaModelo.ifBlank { eq.tipoEquipamento.ifBlank { "Equipamento" } },
                                    numeroSerie = eq.numeroSerie,
                                    defeitoRelatado = eq.defeitoRelatado,
                                    estadoFisico = eq.estadoFisico,
                                    acessorios = eq.acessorios,
                                    diagnostico = eq.diagnostico,
                                    observacoes = eq.observacoes,
                                    garantia = eq.garantia,
                                    statusEquipamento = eq.statusEquipamento
                                )
                            )
                        }

                        itens.forEach { dao.inserirItem(it.copy(id = 0, osId = idReal)) }
                        fotos.forEach { dao.inserirFoto(it.copy(id = 0, osId = idReal)) }
                        atualizarFaixa(0.55f)

                        // Atualiza o Backup e a Planilha reportando o progresso real até o final da faixa
                        GerenciadorBackup.executarAutoBackup(context, dao) { progBackup ->
                            atualizarFaixa(0.55f + (0.45f * progBackup))
                        }

                        rasc.copy(
                            osIdExistente = idReal,
                            numeroOsExistente = numeroOsFinal,
                            clienteIdSelecionado = idClienteReal,
                            nomeCliente = nomeFormatado,
                            dataCriacaoExistente = dataEntradaFinal,
                            dataOrcamentoExistente = dataOrcFinal,
                            dataSaidaExistente = dataSaiFinal,
                            statusExistente = statusConsolidado
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(navController = navController, startDestination = "home") {

                            composable("home") {
                                HomeScreen(
                                    ordens = listaOrdens,
                                    onNavigateToNovaOs = { tipoEscolhido ->
                                        rascunhoAtual = RascunhoOs(
                                            tipo = tipoEscolhido,
                                            equipamentos = listOf(EquipamentoDraft(numeroSequencial = 1))
                                        )
                                        itensAtuais = emptyList()
                                        fotosAtuais = emptyList()
                                        descontoAtual = 0.0
                                        navController.navigate("nova_os")
                                    },
                                    onNavigateToHistorico = { navController.navigate("historico") },
                                    onNavigateToClientes = { navController.navigate("clientes") },
                                    onNavigateToConfig = { navController.navigate("configuracoes") }
                                )
                            }

                            composable("nova_os") {
                                NovaOsScreen(
                                    rascunhoInicial = rascunhoAtual,
                                    clientesSalvos = listaClientes,
                                    onVoltar = { rascunhoSalvo ->
                                        rascunhoAtual = rascunhoSalvo
                                        navController.popBackStack()
                                    },
                                    onAvancar = { rascunhoSalvo ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.05f
                                            mensagemCarregando = "Salvando OS e atualizando Backup..."
                                            val rascunhoGravado = persistirOsAgora(
                                                rasc = rascunhoSalvo,
                                                itens = itensAtuais,
                                                fotos = fotosAtuais,
                                                desc = descontoAtual,
                                                faixaInicio = 0.05f,
                                                faixaFim = 1.0f
                                            )
                                            rascunhoAtual = rascunhoGravado
                                            delay(250) // Mostra o tanque 100% cheio antes de avançar
                                            mensagemCarregando = null
                                            Toast.makeText(
                                                context,
                                                "OS #${rascunhoGravado.numeroOsExistente} e Backup salvos!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.navigate("servicos_fotos")
                                        }
                                    }
                                )
                            }

                            composable("servicos_fotos") {
                                ServicosFotosScreen(
                                    rascunho = rascunhoAtual,
                                    itensIniciais = itensAtuais,
                                    fotosIniciais = fotosAtuais,
                                    descontoInicial = descontoAtual,
                                    onVoltar = { rascSalvo, itensSalvos, fotosSalvas, desc ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.05f
                                            mensagemCarregando = "Salvando alterações e Backup..."
                                            val rascGravado = persistirOsAgora(
                                                rasc = rascSalvo,
                                                itens = itensSalvos,
                                                fotos = fotosSalvas,
                                                desc = desc,
                                                faixaInicio = 0.05f,
                                                faixaFim = 1.0f
                                            )
                                            rascunhoAtual = rascGravado
                                            itensAtuais = itensSalvos
                                            fotosAtuais = fotosSalvas
                                            descontoAtual = desc
                                            delay(200)
                                            mensagemCarregando = null
                                            navController.popBackStack()
                                        }
                                    },
                                    onGerarPdfVisualizar = { rascFinal, itens, fotos, desc, total, diretoWhatsApp ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.05f
                                            mensagemCarregando = "Gravando OS e compactando Backup (1/2)..."
                                            // Primeira metade do tanque (5% a 65%): Salva banco + Backup ZIP/Excel
                                            val rascGravado = persistirOsAgora(
                                                rasc = rascFinal,
                                                itens = itens,
                                                fotos = fotos,
                                                desc = desc,
                                                faixaInicio = 0.05f,
                                                faixaFim = 0.65f
                                            )
                                            rascunhoAtual = rascGravado
                                            itensAtuais = itens
                                            fotosAtuais = fotos
                                            descontoAtual = desc

                                            // Segunda metade do tanque (65% a 100%): Desenha o PDF Multipágina
                                            mensagemCarregando = "Gerando PDF Multipágina (2/2)..."
                                            progressoCarregando = 0.78f

                                            val telFmt1 = formatarTelefoneBr(rascGravado.telefonePrincipal)
                                            val telFmt2 = formatarTelefoneBr(rascGravado.telefoneSecundario)
                                            val telefones = if (telFmt2.isNotBlank()) "$telFmt1 / $telFmt2" else telFmt1

                                            val arquivoGerado = withContext(Dispatchers.IO) {
                                                GeradorPdf.gerarArquivoPdf(
                                                    context = context,
                                                    numeroOs = rascGravado.numeroOsExistente,
                                                    tipo = rascGravado.tipo,
                                                    cliente = rascGravado.nomeCliente,
                                                    telefone = telefones,
                                                    endereco = rascGravado.endereco,
                                                    equipamentos = rascGravado.equipamentos,
                                                    observacoesGerais = rascGravado.observacoesGerais,
                                                    itens = itens,
                                                    fotos = fotos,
                                                    desconto = desc,
                                                    total = total,
                                                    dataCriacao = rascGravado.dataCriacaoExistente,
                                                    dataOrcamento = rascGravado.dataOrcamentoExistente,
                                                    dataSaida = rascGravado.dataSaidaExistente,
                                                    config = configAtual
                                                )
                                            }

                                            progressoCarregando = 1.0f // 100% (140 PSI) atingido!
                                            delay(280)

                                            ultimoArquivoPdf = arquivoGerado
                                            ultimoNumeroOsPdf = rascGravado.numeroOsExistente
                                            ultimoClientePdf = rascGravado.nomeCliente
                                            ultimoTipoPdf = rascGravado.tipo
                                            mensagemCarregando = null

                                            if (diretoWhatsApp) {
                                                GeradorPdf.compartilharPdf(
                                                    context = context,
                                                    file = arquivoGerado,
                                                    cliente = rascGravado.nomeCliente,
                                                    numeroOs = rascGravado.numeroOsExistente,
                                                    nomeOficina = configAtual.nomeOficina,
                                                    diretoWhatsApp = true
                                                )
                                            }
                                            navController.navigate("visualizar_pdf")
                                        }
                                    }
                                )
                            }

                            composable("visualizar_pdf") {
                                VisualizarPdfScreen(
                                    arquivoPdf = ultimoArquivoPdf,
                                    numeroOs = ultimoNumeroOsPdf,
                                    tipo = ultimoTipoPdf,
                                    onVoltarHome = {
                                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                                    },
                                    onCompartilhar = {
                                        ultimoArquivoPdf?.let { file ->
                                            GeradorPdf.compartilharPdf(
                                                context = context,
                                                file = file,
                                                cliente = ultimoClientePdf,
                                                numeroOs = ultimoNumeroOsPdf,
                                                nomeOficina = configAtual.nomeOficina,
                                                diretoWhatsApp = false
                                            )
                                        }
                                    }
                                )
                            }

                            composable("historico") {
                                HistoricoScreen(
                                    ordens = listaOrdens,
                                    onVoltar = { navController.popBackStack() },
                                    onMudarStatus = { os, novo ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val saiTime = if (novo == StatusOrdem.CONCLUIDO || novo == StatusOrdem.ENTREGUE) {
                                                System.currentTimeMillis()
                                            } else os.dataSaida
                                            dao.atualizarOs(os.copy(status = novo, dataSaida = saiTime))
                                            dao.inserirHistoricoStatus(
                                                HistoricoStatus(
                                                    osId = os.id,
                                                    statusAnterior = os.status,
                                                    statusNovo = novo,
                                                    descricao = "Status alterado manualmente pelo atendente."
                                                )
                                            )
                                            GerenciadorBackup.executarAutoBackup(context, dao)
                                        }
                                    },
                                    onEditarDadosClienteOs = { os ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.10f
                                            mensagemCarregando = "Carregando equipamentos da OS..."
                                            carregarOsParaRascunho(os)
                                            delay(200)
                                            mensagemCarregando = null
                                            navController.navigate("nova_os")
                                        }
                                    },
                                    onEditarOrcarOs = { os ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.10f
                                            mensagemCarregando = "Carregando fotos e orçamento..."
                                            carregarOsParaRascunho(os)
                                            delay(200)
                                            mensagemCarregando = null
                                            navController.navigate("servicos_fotos")
                                        }
                                    },
                                    onAdicionarFotoSaida = { os, uri ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.10f
                                            mensagemCarregando = "Salvando foto de saída e Backup..."
                                            withContext(Dispatchers.IO) {
                                                dao.inserirFoto(
                                                    FotoOs(
                                                        osId = os.id,
                                                        equipamentoNumeroSeq = 1,
                                                        uriPath = uri,
                                                        tipoFoto = "SAIDA",
                                                        dataRegistro = System.currentTimeMillis()
                                                    )
                                                )
                                                dao.atualizarOs(os.copy(dataSaida = System.currentTimeMillis()))
                                                GerenciadorBackup.executarAutoBackup(context, dao) { p ->
                                                    progressoCarregando = 0.20f + (0.80f * p)
                                                }
                                            }
                                            delay(250)
                                            mensagemCarregando = null
                                        }
                                    },
                                    onExcluirFotoOs = { _, _ -> },
                                    onVisualizarOuEnviarPdf = { os ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.10f
                                            mensagemCarregando = "Gerando documento PDF da OS #${os.numeroOs}..."
                                            carregarOsParaRascunho(os)
                                            progressoCarregando = 0.55f
                                            val arq = withContext(Dispatchers.IO) {
                                                GeradorPdf.gerarArquivoPdf(
                                                    context = context,
                                                    numeroOs = os.numeroOs,
                                                    tipo = os.tipo,
                                                    cliente = os.nomeCliente,
                                                    telefone = os.telefoneCliente,
                                                    endereco = os.enderecoCliente,
                                                    equipamentos = rascunhoAtual.equipamentos,
                                                    observacoesGerais = os.observacoes,
                                                    itens = itensAtuais,
                                                    fotos = fotosAtuais,
                                                    desconto = os.desconto,
                                                    total = os.valorTotal,
                                                    dataCriacao = os.dataCriacao,
                                                    dataOrcamento = os.dataOrcamento,
                                                    dataSaida = os.dataSaida,
                                                    config = configAtual
                                                )
                                            }
                                            progressoCarregando = 1.0f
                                            delay(250)
                                            ultimoArquivoPdf = arq
                                            ultimoNumeroOsPdf = os.numeroOs
                                            ultimoClientePdf = os.nomeCliente
                                            ultimoTipoPdf = os.tipo
                                            mensagemCarregando = null
                                            navController.navigate("visualizar_pdf")
                                        }
                                    }
                                )
                            }

                            composable("clientes") {
                                ClientesScreen(
                                    clientes = listaClientes,
                                    ordens = listaOrdens,
                                    onVoltar = { navController.popBackStack() },
                                    onSalvarCliente = { clienteEditado, nomeAntigo ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.10f
                                            mensagemCarregando = "Salvando cliente e atualizando planilha..."
                                            withContext(Dispatchers.IO) {
                                                val idSalvo = dao.inserirCliente(clienteEditado).toInt()
                                                val telFmt1 = formatarTelefoneBr(clienteEditado.telefone)
                                                val telFmt2 = formatarTelefoneBr(clienteEditado.telefoneSecundario)
                                                val telsCombinados = if (telFmt2.isNotBlank()) "$telFmt1 / $telFmt2" else telFmt1
                                                listaOrdens.filter {
                                                    (it.clienteId > 0 && it.clienteId == idSalvo) ||
                                                            it.nomeCliente.equals(nomeAntigo, ignoreCase = true) ||
                                                            it.nomeCliente.equals(clienteEditado.nome, ignoreCase = true)
                                                }.forEach { osAntiga ->
                                                    dao.atualizarOs(
                                                        osAntiga.copy(
                                                            clienteId = idSalvo,
                                                            nomeCliente = clienteEditado.nome,
                                                            telefoneCliente = telsCombinados,
                                                            enderecoCliente = clienteEditado.endereco.ifBlank { osAntiga.enderecoCliente }
                                                        )
                                                    )
                                                }
                                                GerenciadorBackup.executarAutoBackup(context, dao) { p ->
                                                    progressoCarregando = 0.25f + (0.75f * p)
                                                }
                                            }
                                            delay(250)
                                            mensagemCarregando = null
                                            Toast.makeText(context, "Cliente e Backup atualizados!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onNovaOsParaCliente = { cli ->
                                        rascunhoAtual = RascunhoOs(
                                            tipo = TipoAtendimento.MAQUINAS,
                                            clienteIdSelecionado = cli.id,
                                            nomeCliente = cli.nome,
                                            telefonePrincipal = cli.telefone,
                                            telefoneSecundario = cli.telefoneSecundario,
                                            endereco = cli.endereco,
                                            equipamentos = listOf(EquipamentoDraft(numeroSequencial = 1))
                                        )
                                        itensAtuais = emptyList()
                                        fotosAtuais = emptyList()
                                        descontoAtual = 0.0
                                        navController.navigate("nova_os")
                                    }
                                )
                            }

                            composable("configuracoes") {
                                ConfiguracoesScreen(
                                    configAtual = configAtual,
                                    onVoltar = { navController.popBackStack() },
                                    onSalvarConfig = { nova ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.10f
                                            mensagemCarregando = "Salvando configurações..."
                                            withContext(Dispatchers.IO) {
                                                dao.salvarConfiguracao(nova)
                                                GerenciadorBackup.executarAutoBackup(context, dao) { p ->
                                                    progressoCarregando = 0.20f + (0.80f * p)
                                                }
                                            }
                                            delay(250)
                                            mensagemCarregando = null
                                        }
                                    },
                                    onExportarPlanilhaExcel = {
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.05f
                                            mensagemCarregando = "Gerando Planilha Excel (.CSV)..."
                                            val (arquivoCsv, _) = GerenciadorBackup.executarAutoBackup(context, dao) { p ->
                                                progressoCarregando = p
                                            }
                                            delay(280)
                                            mensagemCarregando = null
                                            GerenciadorBackup.compartilharArquivoBackup(
                                                context = context,
                                                arquivo = arquivoCsv,
                                                mimeType = "text/csv",
                                                titulo = "Salvar ou Enviar Planilha Excel (.CSV)"
                                            )
                                        }
                                    },
                                    onSalvarBackupGoogleDrive = {
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.05f
                                            mensagemCarregando = "Compactando Fotos e Banco no Backup (.ZIP)..."
                                            val (_, arquivoZip) = GerenciadorBackup.executarAutoBackup(context, dao) { p ->
                                                progressoCarregando = p
                                            }
                                            delay(280) // Pausa rápida em 100% (140 PSI) para mostrar que concluiu!
                                            mensagemCarregando = null
                                            GerenciadorBackup.compartilharArquivoBackup(
                                                context = context,
                                                arquivo = arquivoZip,
                                                mimeType = "application/zip",
                                                titulo = "Salvar Backup Completo no Google Drive (.ZIP)"
                                            )
                                        }
                                    },
                                    onRestaurarBackupZip = { uriZip ->
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.05f
                                            mensagemCarregando = "Restaurando Clientes, OS e Fotos do Backup..."
                                            try {
                                                val qtdRestaurada = GerenciadorBackup.restaurarBackupDeUri(context, uriZip, dao) { p ->
                                                    progressoCarregando = p
                                                }
                                                delay(280)
                                                mensagemCarregando = null
                                                Toast.makeText(
                                                    context,
                                                    "Sucesso! $qtdRestaurada OS, clientes e fotos restaurados!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                mensagemCarregando = null
                                                Toast.makeText(context, "Arquivo inválido para restauração.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onCarregarDemo = {
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.10f
                                            mensagemCarregando = "Criando OS Demo com 4 máquinas..."
                                            withContext(Dispatchers.IO) {
                                                val idCli = dao.inserirCliente(
                                                    Cliente(nome = "João Silva", telefone = "11987654321", endereco = "Rua das Flores, 123 - Tuiuti/SP")
                                                ).toInt()
                                                val agora = System.currentTimeMillis()
                                                val idOs = dao.inserirOs(
                                                    OrdemServico(
                                                        numeroOs = "000123",
                                                        clienteId = idCli,
                                                        nomeCliente = "João Silva",
                                                        telefoneCliente = "(11) 98765-4321",
                                                        enderecoCliente = "Rua das Flores, 123 - Tuiuti/SP",
                                                        tipo = TipoAtendimento.MAQUINAS,
                                                        categoriaEquipamento = "Motosserra",
                                                        equipamento = "4 Equipamentos: Motosserra Stihl, Roçadeira Toyama, Gerador Honda, Compressor Schulz",
                                                        defeitoRelatado = "Revisão de lote de 4 máquinas",
                                                        status = StatusOrdem.EM_ANALISE,
                                                        dataCriacao = agora,
                                                        dataOrcamento = agora,
                                                        valorTotal = 360.0
                                                    )
                                                ).toInt()

                                                val maquinasDemo = listOf(
                                                    Triple(1, "Motosserra", "Motosserra Stihl MS 382"),
                                                    Triple(2, "Roçadeira", "Roçadeira Toyama TBC43X"),
                                                    Triple(3, "Gerador", "Gerador Honda EG1000"),
                                                    Triple(4, "Compressor", "Compressor Schulz 25L")
                                                )
                                                maquinasDemo.forEach { (seq, tipoEq, modelo) ->
                                                    dao.inserirEquipamento(
                                                        EquipamentoOs(
                                                            osId = idOs,
                                                            numeroSequencial = seq,
                                                            tipoEquipamento = tipoEq,
                                                            marcaModelo = modelo,
                                                            defeitoRelatado = "Revisão e limpeza",
                                                            acessorios = if (seq == 1) "Sabre e corrente" else "Sem acessórios",
                                                            garantia = "90 dias no serviço",
                                                            statusEquipamento = StatusOrdem.EM_ANALISE
                                                        )
                                                    )
                                                }
                                                dao.inserirItem(ItemServico(osId = idOs, equipamentoNumeroSeq = 1, descricao = "Limpeza do carburador", quantidade = 1, valorUnitarioCentavos = 12000L, valor = 120.0))
                                                dao.inserirItem(ItemServico(osId = idOs, equipamentoNumeroSeq = 2, descricao = "Troca de vela e filtro", quantidade = 2, valorUnitarioCentavos = 6000L, valor = 120.0))
                                                dao.inserirItem(ItemServico(osId = idOs, equipamentoNumeroSeq = 3, descricao = "Ajuste de tensão", quantidade = 1, valorUnitarioCentavos = 12000L, valor = 120.0))
                                                GerenciadorBackup.executarAutoBackup(context, dao) { p ->
                                                    progressoCarregando = 0.30f + (0.70f * p)
                                                }
                                            }
                                            delay(250)
                                            mensagemCarregando = null
                                            Toast.makeText(context, "Demo com 4 Máquinas criada!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onLimparBanco = {
                                        lifecycleScope.launch {
                                            progressoCarregando = 0.20f
                                            mensagemCarregando = "Limpando registros de teste..."
                                            withContext(Dispatchers.IO) {
                                                dao.limparTodasFotos()
                                                dao.limparTodosItens()
                                                dao.limparTodosEquipamentos()
                                                dao.limparTodoHistorico()
                                                dao.limparTodasOs()
                                                dao.limparTodosClientes()
                                            }
                                            progressoCarregando = 1.0f
                                            delay(250)
                                            mensagemCarregando = null
                                            Toast.makeText(context, "Dados de teste removidos!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }

                        // OVERLAY DO COMPRESSOR DE OFICINA COM ENCHIMENTO REAL DE 0% ATÉ 100%
                        mensagemCarregando?.let { msg ->
                            OverlayCompressorCarregando(
                                mensagem = msg,
                                progressoReal = progressoCarregando
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ilustração vetorial inspirada no compressor estacionário Pressure:
 * - O nível do tanque (nivelTanque) agora acompanha o progresso real da tarefa (0% -> 100%)
 *   sem nunca reiniciar no meio!
 */
@Composable
fun OverlayCompressorCarregando(
    mensagem: String,
    progressoReal: Float
) {
    // Anima suavemente o nível do tanque conforme o progresso real avança (nunca volta ao zero)
    val nivelTanque by animateFloatAsState(
        targetValue = progressoReal.coerceIn(0.05f, 1.0f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "nivel_real_tanque"
    )

    // Apenas o volante traseiro gira continuamente enquanto o motor trabalha
    val infiniteTransition = rememberInfiniteTransition(label = "motor_girando")
    val anguloVolante by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "giro_volante"
    )

    val corCabecoteVermelho = Color(0xFFD62828)
    val corCabecoteEscuro = Color(0xFF8F1616)
    val corTanqueAzulEscuro = Color(0xFF162B3C)
    val corChassiMetal = Color(0xFF3E4C5E)
    val corMotorCinza = Color(0xFFA5ABB6)
    val corMotorEscuro = Color(0xFF5A606B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .width(295.dp)
                .border(1.5.dp, OrangeNeon, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Canvas(modifier = Modifier.size(width = 240.dp, height = 165.dp)) {
                    val w = size.width
                    val h = size.height
                    val vibracao = if (nivelTanque < 0.99f) {
                        (sin(Math.toRadians(anguloVolante.toDouble() * 2)).toFloat() * 1.2f)
                    } else 0f

                    // 1. GRADE DE PROTEÇÃO DA CORREIA
                    val pathGrade = Path().apply {
                        moveTo(w * 0.28f, h * 0.44f)
                        lineTo(w * 0.28f, h * 0.16f)
                        quadraticBezierTo(w * 0.36f, h * 0.08f, w * 0.48f, h * 0.14f)
                        lineTo(w * 0.65f, h * 0.24f)
                        lineTo(w * 0.65f, h * 0.44f)
                        close()
                    }
                    drawPath(pathGrade, color = corCabecoteEscuro.copy(alpha = 0.55f))
                    drawPath(pathGrade, color = corCabecoteVermelho, style = Stroke(width = 3f))

                    // Volante raiado grande girando atrás do cabeçote em V
                    val centroVolante = Offset(w * 0.37f, h * 0.29f + vibracao)
                    val raioVolante = w * 0.115f
                    drawCircle(
                        color = corCabecoteEscuro,
                        radius = raioVolante,
                        center = centroVolante,
                        style = Stroke(width = 5f)
                    )
                    for (i in 0 until 4) {
                        val rad = Math.toRadians((anguloVolante + i * 90f).toDouble())
                        drawLine(
                            color = corCabecoteVermelho,
                            start = centroVolante,
                            end = Offset(
                                centroVolante.x + (cos(rad) * raioVolante).toFloat(),
                                centroVolante.y + (sin(rad) * raioVolante).toFloat()
                            ),
                            strokeWidth = 3.5f
                        )
                    }

                    // 2. CABEÇOTE EM V DE DOIS CILINDROS ALETADOS
                    val centroCarter = Offset(w * 0.37f, h * 0.34f + vibracao)

                    fun desenharCilindroAletado(anguloGraus: Float, filtroEsquerda: Boolean) {
                        rotate(degrees = anguloGraus, pivot = centroCarter) {
                            val largCamisa = w * 0.095f
                            val altCamisa = h * 0.19f
                            val topoCamisaY = centroCarter.y - altCamisa

                            drawRoundRect(
                                color = corCabecoteEscuro,
                                topLeft = Offset(centroCarter.x - largCamisa / 2f, topoCamisaY),
                                size = Size(largCamisa, altCamisa),
                                cornerRadius = CornerRadius(4f, 4f)
                            )

                            for (a in 0..3) {
                                val yAleta = topoCamisaY + (altCamisa * 0.24f) + (a * (altCamisa * 0.15f))
                                drawRoundRect(
                                    color = corCabecoteVermelho,
                                    topLeft = Offset(centroCarter.x - largCamisa * 0.65f, yAleta),
                                    size = Size(largCamisa * 1.30f, h * 0.022f),
                                    cornerRadius = CornerRadius(3f, 3f)
                                )
                            }

                            val largTopo = largCamisa * 1.42f
                            val altTopo = h * 0.065f
                            drawRoundRect(
                                color = corCabecoteVermelho,
                                topLeft = Offset(centroCarter.x - largTopo / 2f, topoCamisaY - altTopo * 0.6f),
                                size = Size(largTopo, altTopo),
                                cornerRadius = CornerRadius(5f, 5f)
                            )
                            drawRoundRect(
                                color = Color(0xFFFF6B6B),
                                topLeft = Offset(centroCarter.x - largTopo / 2f, topoCamisaY - altTopo * 0.6f),
                                size = Size(largTopo, altTopo),
                                cornerRadius = CornerRadius(5f, 5f),
                                style = Stroke(width = 1.5f)
                            )

                            val xFiltro = if (filtroEsquerda) {
                                centroCarter.x - largTopo * 0.55f
                            } else {
                                centroCarter.x + largTopo * 0.55f
                            }
                            drawCircle(
                                color = corCabecoteEscuro,
                                radius = w * 0.028f,
                                center = Offset(xFiltro, topoCamisaY - altTopo * 0.15f)
                            )
                            drawCircle(
                                color = Color(0xFF222530),
                                radius = w * 0.015f,
                                center = Offset(xFiltro, topoCamisaY - altTopo * 0.15f)
                            )
                        }
                    }

                    desenharCilindroAletado(anguloGraus = -38f, filtroEsquerda = true)
                    desenharCilindroAletado(anguloGraus = 36f, filtroEsquerda = false)

                    drawRoundRect(
                        color = corCabecoteEscuro,
                        topLeft = Offset(centroCarter.x - w * 0.09f, centroCarter.y + h * 0.02f),
                        size = Size(w * 0.18f, h * 0.08f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawCircle(color = corCabecoteVermelho, radius = w * 0.078f, center = centroCarter)
                    drawCircle(color = corCabecoteEscuro, radius = w * 0.078f, center = centroCarter, style = Stroke(width = 3f))
                    drawCircle(color = Color(0xFFFF4D4D), radius = w * 0.038f, center = centroCarter)

                    // 3. MOTOR ELÉTRICO CILÍNDRICO CINZA
                    val centroMotor = Offset(w * 0.61f, h * 0.31f + vibracao * 0.5f)
                    val raioMotor = w * 0.092f

                    drawRect(
                        color = corMotorEscuro,
                        topLeft = Offset(centroMotor.x - raioMotor * 0.8f, centroMotor.y + raioMotor * 0.65f),
                        size = Size(raioMotor * 1.6f, h * 0.05f)
                    )
                    drawCircle(color = corMotorEscuro, radius = raioMotor + 2f, center = centroMotor)
                    drawCircle(color = corMotorCinza, radius = raioMotor, center = centroMotor)

                    for (slot in 0 until 6) {
                        drawArc(
                            color = DarkBackground,
                            startAngle = slot * 60f + 12f,
                            sweepAngle = 36f,
                            useCenter = false,
                            topLeft = Offset(centroMotor.x - raioMotor * 0.68f, centroMotor.y - raioMotor * 0.68f),
                            size = Size(raioMotor * 1.36f, raioMotor * 1.36f),
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }
                    drawCircle(color = corMotorEscuro, radius = raioMotor * 0.28f, center = centroMotor)
                    drawCircle(color = Color.White, radius = raioMotor * 0.10f, center = centroMotor)

                    // 4. SAPATA SUPERIOR, PRESSOSTATO E MANÔMETRO
                    drawLine(
                        color = Color(0xFFD4AF37),
                        start = Offset(w * 0.19f, h * 0.43f),
                        end = Offset(w * 0.19f, h * 0.49f),
                        strokeWidth = 5f
                    )
                    drawRoundRect(
                        color = corMotorEscuro,
                        topLeft = Offset(w * 0.135f, h * 0.36f),
                        size = Size(w * 0.11f, h * 0.075f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = corMotorCinza,
                        topLeft = Offset(w * 0.14f, h * 0.365f),
                        size = Size(w * 0.10f, h * 0.045f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )

                    drawRoundRect(
                        color = corChassiMetal,
                        topLeft = Offset(w * 0.26f, h * 0.425f),
                        size = Size(w * 0.46f, h * 0.042f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRect(color = corChassiMetal, topLeft = Offset(w * 0.28f, h * 0.46f), size = Size(w * 0.08f, h * 0.035f))
                    drawRect(color = corChassiMetal, topLeft = Offset(w * 0.62f, h * 0.46f), size = Size(w * 0.08f, h * 0.035f))

                    // Manômetro na direita
                    val xManometro = w * 0.80f
                    val centroManometro = Offset(xManometro, h * 0.31f)
                    drawLine(color = corMotorEscuro, start = Offset(xManometro, h * 0.36f), end = Offset(xManometro, h * 0.49f), strokeWidth = 5f)
                    drawCircle(color = corCabecoteVermelho, radius = 4.5f, center = Offset(xManometro + 6f, h * 0.42f))
                    drawCircle(color = corMotorEscuro, radius = w * 0.048f, center = centroManometro)
                    drawCircle(color = Color.White, radius = w * 0.040f, center = centroManometro)

                    val angPonteiro = Math.toRadians(-145.0 + (nivelTanque * 230.0))
                    drawLine(
                        color = corCabecoteVermelho,
                        start = centroManometro,
                        end = Offset(
                            centroManometro.x + (cos(angPonteiro) * (w * 0.032f)).toFloat(),
                            centroManometro.y + (sin(angPonteiro) * (w * 0.032f)).toFloat()
                        ),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )

                    // 5. PÉS LARGOS DO TANQUE
                    val peEsqPath = Path().apply {
                        moveTo(w * 0.19f, h * 0.78f)
                        lineTo(w * 0.28f, h * 0.78f)
                        lineTo(w * 0.27f, h * 0.96f)
                        lineTo(w * 0.17f, h * 0.96f)
                        close()
                    }
                    val peDirPath = Path().apply {
                        moveTo(w * 0.72f, h * 0.78f)
                        lineTo(w * 0.81f, h * 0.78f)
                        lineTo(w * 0.83f, h * 0.96f)
                        lineTo(w * 0.73f, h * 0.96f)
                        close()
                    }
                    drawPath(peEsqPath, color = corTanqueAzulEscuro)
                    drawPath(peEsqPath, color = corChassiMetal, style = Stroke(width = 2.5f))
                    drawPath(peDirPath, color = corTanqueAzulEscuro)
                    drawPath(peDirPath, color = corChassiMetal, style = Stroke(width = 2.5f))

                    // 6. RESERVATÓRIO CILÍNDRICO HORIZONTAL (Enchimento único 0% -> 100%)
                    val tankLeft = w * 0.07f
                    val tankTop = h * 0.485f
                    val tankWidth = w * 0.86f
                    val tankHeight = h * 0.34f
                    val raioCalota = tankHeight / 2f

                    val tankPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(Offset(tankLeft, tankTop), Size(tankWidth, tankHeight)),
                                cornerRadius = CornerRadius(raioCalota, raioCalota)
                            )
                        )
                    }

                    drawPath(tankPath, color = corTanqueAzulEscuro)

                    clipPath(tankPath) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    OrangeNeon.copy(alpha = 0.85f),
                                    OrangeNeon.copy(alpha = 0.95f),
                                    if (nivelTanque >= 0.98f) StatusGreen else CyanNeon.copy(alpha = 0.90f)
                                )
                            ),
                            topLeft = Offset(tankLeft, tankTop),
                            size = Size(tankWidth * nivelTanque, tankHeight)
                        )

                        drawRect(
                            color = Color.White.copy(alpha = 0.14f),
                            topLeft = Offset(tankLeft, tankTop + tankHeight * 0.12f),
                            size = Size(tankWidth, tankHeight * 0.16f)
                        )
                    }

                    drawPath(path = tankPath, color = corChassiMetal, style = Stroke(width = 3.5f))

                    val labelW = tankWidth * 0.54f
                    val labelH = tankHeight * 0.40f
                    val labelX = tankLeft + (tankWidth - labelW) / 2f
                    val labelY = tankTop + (tankHeight - labelH) / 2f
                    drawRoundRect(
                        color = Color(0xFFEAECEF),
                        topLeft = Offset(labelX, labelY),
                        size = Size(labelW, labelH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = corCabecoteVermelho,
                        topLeft = Offset(labelX + 6f, labelY + 4f),
                        size = Size(labelW - 12f, labelH * 0.42f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRoundRect(
                        color = corTanqueAzulEscuro,
                        topLeft = Offset(labelX + 6f, labelY + labelH * 0.56f),
                        size = Size(labelW * 0.65f, labelH * 0.24f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val psiAtual = (nivelTanque * 140).toInt()
                val pctAtual = (nivelTanque * 100).toInt()
                Text(
                    text = if (pctAtual >= 100) "TANQUE CHEIO • 140 PSI (100%)" else "CARREGANDO PRESSÃO • $psiAtual PSI ($pctAtual%)",
                    color = if (pctAtual >= 100) StatusGreen else OrangeNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = mensagem,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
// FIM DO ARQUIVO