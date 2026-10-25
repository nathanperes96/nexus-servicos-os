package com.nexus.servicos.os

import com.nexus.servicos.os.data.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun calculoCentavos_evitaErrosDePontoFlutuante() {
        val itens = listOf(
            ItemServico(osId = 1, equipamentoNumeroSeq = 1, tipoItem = "PECA", descricao = "Vela", quantidade = 3, valorUnitarioCentavos = 3550L, valor = 106.50),
            ItemServico(osId = 1, equipamentoNumeroSeq = 2, tipoItem = "SERVICO", descricao = "Limpeza", quantidade = 1, valorUnitarioCentavos = 8025L, valor = 80.25)
        )
        // 3 * 35,50 (106,50) + 80,25 = 186,75 (18675 centavos)
        // Desconto: R$ 16,75 (1675 centavos) -> Total esperado: R$ 170,00 (17000 centavos)
        val totalCentavos = CalculadoraOrcamento.calcularTotalLiquidoCentavos(itens, 1675L)
        assertEquals(17000L, totalCentavos)
        assertEquals(170.00, CalculadoraOrcamento.centavosParaReais(totalCentavos), 0.001)
    }

    @Test
    fun consolidacaoStatusMultiplosEquipamentos_naoMarcaConcluidoSeHouverEquipamentoEmAnalise() {
        val quatroMaquinas = listOf(
            EquipamentoDraft(numeroSequencial = 1, tipoEquipamento = "Motosserra", statusEquipamento = StatusOrdem.CONCLUIDO),
            EquipamentoDraft(numeroSequencial = 2, tipoEquipamento = "Roçadeira", statusEquipamento = StatusOrdem.CONCLUIDO),
            EquipamentoDraft(numeroSequencial = 3, tipoEquipamento = "Gerador", statusEquipamento = StatusOrdem.EM_ANALISE),
            EquipamentoDraft(numeroSequencial = 4, tipoEquipamento = "Compressor", statusEquipamento = StatusOrdem.CONCLUIDO)
        )
        val statusGeral = CalculadoraOrcamento.consolidarStatusOs(StatusOrdem.EM_ANALISE, quatroMaquinas)
        // Como 1 máquina ainda está em análise e outras concluídas, o status geral é "Em execução" (nunca "Concluído" enganoso)
        assertEquals(StatusOrdem.EM_EXECUCAO, statusGeral)
    }

    @Test
    fun paginacaoPdf_quatroEquipamentosEMaisDeDuasFotos_geraSegundaPaginaDedicada() {
        // Caso obrigatório da Seção 11: 4 equipamentos e 5 fotos de entrada
        val paginas = GeradorPdf.calcularTotalPaginasPrevisto(
            qtdEquipamentos = 4,
            qtdFotosEntrada = 5,
            qtdFotosSaida = 0
        )
        // Página 1 (Resumo dos 4 equipamentos) + Página 2 (4 fotos) + Página 3 (5ª foto) = 3 páginas
        assertEquals(3, paginas)
    }

    @Test
    fun formatacaoTelefoneBr_suportaFixo8DigitosECelular9Digitos() {
        assertEquals("(11) 3456-7890", formatarTelefoneBr("1134567890"))
        assertEquals("(11) 98765-4321", formatarTelefoneBr("11987654321999"))
    }
}