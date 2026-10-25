package com.nexus.servicos.os.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale
import kotlin.math.roundToLong

enum class TipoAtendimento { MAQUINAS, TECNOLOGIA }

object StatusOrdem {
    const val EM_ANALISE = "Em análise"
    const val ORCAMENTO_ENVIADO = "Orçamento enviado"
    const val AGUARDANDO_APROVACAO = "Aguardando aprovação"
    const val APROVADO = "Aprovado"
    const val REPROVADO = "Reprovado"
    const val EM_EXECUCAO = "Em execução"
    const val AGUARDANDO_PECA = "Aguardando peça"
    const val CONCLUIDO = "Concluído"
    const val PRONTO_RETIRADA = "Pronto para retirada"
    const val ENTREGUE = "Entregue"
    const val CANCELADO = "Cancelado"

    val TODOS = listOf(
        EM_ANALISE,
        ORCAMENTO_ENVIADO,
        AGUARDANDO_APROVACAO,
        APROVADO,
        REPROVADO,
        EM_EXECUCAO,
        AGUARDANDO_PECA,
        CONCLUIDO,
        PRONTO_RETIRADA,
        ENTREGUE,
        CANCELADO
    )
}

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val telefone: String,
    val telefoneSecundario: String = "",
    val endereco: String = "",
    val observacoes: String = ""
)

@Entity(tableName = "ordens_servico")
data class OrdemServico(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val numeroOs: String,
    val clienteId: Int = 0,
    val nomeCliente: String,
    val telefoneCliente: String,
    val enderecoCliente: String = "",
    val tipo: TipoAtendimento,
    val categoriaEquipamento: String = "",
    val equipamento: String,
    val numeroSerie: String = "",
    val defeitoRelatado: String,
    val acessorios: String = "",
    val garantia: String = "",
    val observacoes: String = "",
    val status: String = StatusOrdem.EM_ANALISE,
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataOrcamento: Long = 0L,
    val dataSaida: Long = 0L,
    val desconto: Double = 0.0,
    val valorTotal: Double = 0.0
)

@Entity(tableName = "equipamentos_os")
data class EquipamentoOs(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val osId: Int,
    val numeroSequencial: Int,
    val tipoEquipamento: String,
    val marcaModelo: String,
    val numeroSerie: String = "",
    val defeitoRelatado: String = "",
    val estadoFisico: String = "",
    val acessorios: String = "",
    val diagnostico: String = "",
    val observacoes: String = "",
    val garantia: String = "",
    val statusEquipamento: String = StatusOrdem.EM_ANALISE
) {
    fun rotuloIdentificacao(): String {
        val seq = numeroSequencial.toString().padStart(2, '0')
        val nome = marcaModelo.ifBlank { tipoEquipamento.ifBlank { "Equipamento" } }
        return "Equipamento $seq — $nome"
    }
}

@Entity(tableName = "historico_status")
data class HistoricoStatus(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val osId: Int,
    val statusAnterior: String,
    val statusNovo: String,
    val descricao: String = "",
    val dataHora: Long = System.currentTimeMillis()
)

@Entity(tableName = "configuracoes")
data class ConfiguracaoOficina(
    @PrimaryKey val id: Int = 1,
    val nomeOficina: String = "NEXUS SERVIÇOS",
    val whatsapp: String = "",
    val cidadeUf: String = "Tuiuti - SP",
    val garantiaPadrao: String = "90 dias no serviço",
    val validadeOrcamentoDias: Int = 7,
    // Frase de peças novas removida permanentemente
    val observacoesPadrao: String = "Qualquer dúvida, entre em contato pelo WhatsApp."
)

data class EquipamentoDraft(
    val idBanco: Int = 0,
    val numeroSequencial: Int = 1,
    val tipoEquipamento: String = "",
    val marcaModelo: String = "",
    val numeroSerie: String = "",
    val defeitoRelatado: String = "",
    val estadoFisico: String = "",
    val acessorios: String = "",
    val diagnostico: String = "",
    val observacoes: String = "",
    val garantia: String = "",
    val statusEquipamento: String = StatusOrdem.EM_ANALISE
) {
    fun rotulo(): String {
        val seq = numeroSequencial.toString().padStart(2, '0')
        val nome = marcaModelo.ifBlank { tipoEquipamento.ifBlank { "Equipamento" } }
        return "Equipamento $seq — $nome"
    }
}

data class RascunhoOs(
    val osIdExistente: Int = 0,
    val numeroOsExistente: String = "",
    val dataCriacaoExistente: Long = 0L,
    val dataOrcamentoExistente: Long = 0L,
    val dataSaidaExistente: Long = 0L,
    val statusExistente: String = StatusOrdem.EM_ANALISE,
    val clienteIdSelecionado: Int = 0,
    val tipo: TipoAtendimento = TipoAtendimento.MAQUINAS,
    val nomeCliente: String = "",
    val telefonePrincipal: String = "",
    val telefoneSecundario: String = "",
    val endereco: String = "",
    val observacoesGerais: String = "",
    val equipamentos: List<EquipamentoDraft> = listOf(EquipamentoDraft(numeroSequencial = 1))
)

object CalculadoraOrcamento {
    fun reaisParaCentavos(valorReais: Double): Long {
        if (valorReais.isNaN() || valorReais.isInfinite() || valorReais < 0.0) return 0L
        return (valorReais * 100.0).roundToLong()
    }

    fun textoParaCentavos(texto: String): Long {
        val limpo = texto.replace(Regex("[^0-9.,]"), "").replace(",", ".")
        val duplo = limpo.toDoubleOrNull() ?: 0.0
        return reaisParaCentavos(duplo)
    }

    fun centavosParaReais(centavos: Long): Double = centavos / 100.0

    fun formatarMoedaCentavos(centavos: Long): String {
        return String.format(Locale.getDefault(), "R$ %.2f", centavosParaReais(centavos))
    }

    fun formatarMoeda(valorReais: Double): String {
        return formatarMoedaCentavos(reaisParaCentavos(valorReais))
    }

    fun calcularSubtotalItemCentavos(quantidade: Int, valorUnitarioCentavos: Long): Long {
        val qtdSegura = quantidade.coerceAtLeast(1)
        val unitSeguro = valorUnitarioCentavos.coerceAtLeast(0L)
        return qtdSegura.toLong() * unitSeguro
    }

    fun calcularTotalLiquidoCentavos(itens: List<ItemServico>, descontoCentavos: Long): Long {
        val somaCentavos = itens.sumOf { item ->
            val unitCentavos = if (item.valorUnitarioCentavos > 0L) {
                item.valorUnitarioCentavos
            } else {
                reaisParaCentavos(item.valor)
            }
            calcularSubtotalItemCentavos(item.quantidade, unitCentavos)
        }
        return (somaCentavos - descontoCentavos.coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    fun consolidarStatusOs(statusAtualOs: String, equipamentos: List<EquipamentoDraft>): String {
        if (equipamentos.isEmpty()) return statusAtualOs
        val statusList = equipamentos.map { it.statusEquipamento }
        return when {
            statusList.all { it == StatusOrdem.REPROVADO } -> StatusOrdem.REPROVADO
            statusList.all { it == StatusOrdem.CANCELADO } -> StatusOrdem.CANCELADO
            statusList.all { it == StatusOrdem.ENTREGUE } -> StatusOrdem.ENTREGUE
            statusList.all { it == StatusOrdem.PRONTO_RETIRADA || it == StatusOrdem.ENTREGUE } -> StatusOrdem.PRONTO_RETIRADA
            statusList.all { it == StatusOrdem.CONCLUIDO || it == StatusOrdem.PRONTO_RETIRADA || it == StatusOrdem.ENTREGUE || it == StatusOrdem.REPROVADO } -> StatusOrdem.CONCLUIDO
            statusList.any { it == StatusOrdem.EM_EXECUCAO || it == StatusOrdem.CONCLUIDO } -> StatusOrdem.EM_EXECUCAO
            statusList.any { it == StatusOrdem.AGUARDANDO_PECA } -> StatusOrdem.AGUARDANDO_PECA
            statusList.all { it == StatusOrdem.APROVADO } -> StatusOrdem.APROVADO
            statusList.any { it == StatusOrdem.AGUARDANDO_APROVACAO || it == StatusOrdem.ORCAMENTO_ENVIADO } -> StatusOrdem.AGUARDANDO_APROVACAO
            else -> statusAtualOs
        }
    }
}

fun formatarTelefoneBr(digitos: String): String {
    val limpo = digitos.filter { it.isDigit() }.take(11)
    return when {
        limpo.isEmpty() -> ""
        limpo.length <= 2 -> "(${limpo}"
        limpo.length <= 6 -> "(${limpo.substring(0, 2)}) ${limpo.substring(2)}"
        limpo.length <= 10 -> "(${limpo.substring(0, 2)}) ${limpo.substring(2, 6)}-${limpo.substring(6)}"
        else -> "(${limpo.substring(0, 2)}) ${limpo.substring(2, 7)}-${limpo.substring(7)}"
    }
}

fun capitalizarIniciais(texto: String): String {
    if (texto.isEmpty()) return texto
    val sb = StringBuilder(texto.length)
    var capitalizarProxima = true
    for (c in texto) {
        if (c.isWhitespace()) {
            sb.append(c)
            capitalizarProxima = true
        } else if (capitalizarProxima) {
            sb.append(c.uppercaseChar())
            capitalizarProxima = false
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}
// FIM DO ARQUIVO