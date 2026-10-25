package com.nexus.servicos.os

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nexus.servicos.os.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun osComQuatroEquipamentosEMaisDeDuasFotos_persisteEGeraPdfMultipagina() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.nexusDao()

        val clienteId = dao.inserirCliente(
            Cliente(nome = "Cliente Teste Quatro Máquinas", telefone = "11987654321")
        ).toInt()

        val osId = dao.inserirOs(
            OrdemServico(
                numeroOs = "000500",
                clienteId = clienteId,
                nomeCliente = "Cliente Teste Quatro Máquinas",
                telefoneCliente = "(11) 98765-4321",
                tipo = TipoAtendimento.MAQUINAS,
                equipamento = "4 Equipamentos",
                defeitoRelatado = "Revisão geral"
            )
        ).toInt()

        val drafts = listOf(
            EquipamentoDraft(numeroSequencial = 1, tipoEquipamento = "Motosserra", marcaModelo = "Motosserra Stihl", acessorios = "Sabre e corrente", garantia = "90 dias"),
            EquipamentoDraft(numeroSequencial = 2, tipoEquipamento = "Roçadeira", marcaModelo = "Roçadeira Toyama", acessorios = "Lâmina 3 pontas", garantia = "30 dias"),
            EquipamentoDraft(numeroSequencial = 3, tipoEquipamento = "Gerador", marcaModelo = "Gerador Honda", acessorios = "Cabo", garantia = "90 dias"),
            EquipamentoDraft(numeroSequencial = 4, tipoEquipamento = "Compressor", marcaModelo = "Compressor Schulz", acessorios = "Mangueira", garantia = "Sem garantia")
        )

        drafts.forEach { eq ->
            dao.inserirEquipamento(
                EquipamentoOs(
                    osId = osId,
                    numeroSequencial = eq.numeroSequencial,
                    tipoEquipamento = eq.tipoEquipamento,
                    marcaModelo = eq.marcaModelo,
                    acessorios = eq.acessorios,
                    garantia = eq.garantia
                )
            )
        }

        val fotosEntrada = listOf(
            FotoOs(osId = osId, equipamentoNumeroSeq = 1, uriPath = "content://teste/foto1.jpg", tipoFoto = "ENTRADA", legenda = "Motosserra frente"),
            FotoOs(osId = osId, equipamentoNumeroSeq = 2, uriPath = "content://teste/foto2.jpg", tipoFoto = "ENTRADA", legenda = "Roçadeira"),
            FotoOs(osId = osId, equipamentoNumeroSeq = 3, uriPath = "content://teste/foto3.jpg", tipoFoto = "ENTRADA", legenda = "Gerador"),
            FotoOs(osId = osId, equipamentoNumeroSeq = 4, uriPath = "content://teste/foto4.jpg", tipoFoto = "ACESSORIO", legenda = "Compressor e mangueira")
        )
        fotosEntrada.forEach { dao.inserirFoto(it) }

        val equipsBanco = dao.buscarEquipamentosDaOs(osId)
        val fotosBanco = dao.buscarFotosDaOs(osId)
        assertEquals(4, equipsBanco.size)
        assertEquals(4, fotosBanco.size)

        val pdfFile = GeradorPdf.gerarArquivoPdf(
            context = context,
            numeroOs = "000500",
            tipo = TipoAtendimento.MAQUINAS,
            cliente = "Cliente Teste Quatro Máquinas",
            telefone = "(11) 98765-4321",
            endereco = "Tuiuti - SP",
            equipamentos = drafts,
            observacoesGerais = "Teste de aceitação com 4 equipamentos",
            itens = emptyList(),
            fotos = fotosBanco,
            desconto = 0.0,
            total = 0.0
        )

        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0L)
        db.close()
    }
}