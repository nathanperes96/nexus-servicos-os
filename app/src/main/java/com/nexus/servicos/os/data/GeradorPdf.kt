package com.nexus.servicos.os.data

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GeradorPdf {

    fun calcularTotalPaginasPrevisto(
        qtdEquipamentos: Int,
        qtdFotosEntrada: Int,
        qtdFotosSaida: Int
    ): Int {
        var paginas = 1
        val exigePaginaDedicadaEntrada = qtdFotosEntrada > 2 || (qtdFotosEntrada > 0 && qtdEquipamentos > 1)
        if (exigePaginaDedicadaEntrada) {
            paginas += (qtdFotosEntrada + 3) / 4
        }
        if (qtdFotosSaida > 0) {
            paginas += (qtdFotosSaida + 3) / 4
        }
        return paginas
    }

    fun gerarArquivoPdf(
        context: Context,
        numeroOs: String,
        tipo: TipoAtendimento,
        cliente: String,
        telefone: String,
        endereco: String,
        equipamentos: List<EquipamentoDraft>,
        observacoesGerais: String,
        itens: List<ItemServico>,
        fotos: List<FotoOs>,
        desconto: Double,
        total: Double,
        dataCriacao: Long = System.currentTimeMillis(),
        dataOrcamento: Long = 0L,
        dataSaida: Long = 0L,
        config: ConfiguracaoOficina = ConfiguracaoOficina()
    ): File {
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val isMaquinas = tipo == TipoAtendimento.MAQUINAS
        val corNeon = if (isMaquinas) Color.rgb(255, 138, 0) else Color.rgb(0, 200, 255)
        val corEscura = Color.rgb(14, 17, 24)
        val corCard = Color.rgb(244, 245, 248)
        val corBordaCard = Color.rgb(218, 222, 230)
        val corTextoEscuro = Color.rgb(22, 25, 32)
        val corTextoCinza = Color.rgb(90, 96, 108)
        val corVerdeWhats = Color.rgb(37, 211, 102)
        val corVermelhoReprovado = Color.rgb(213, 0, 0)

        val numFormatado = numeroOs.padStart(6, '0')
        val fotosEntrada = fotos.filter { it.tipoFoto == "ENTRADA" || it.tipoFoto == "ACESSORIO" }
            .sortedWith(compareBy({ it.equipamentoNumeroSeq }, { it.id }))
        val fotosSaida = fotos.filter { it.tipoFoto == "SAIDA" }
            .sortedWith(compareBy({ it.equipamentoNumeroSeq }, { it.id }))

        val usarPaginaDedicadaEntrada = fotosEntrada.size > 2 || (fotosEntrada.isNotEmpty() && equipamentos.size > 1)
        var numeroPaginaAtual = 1

        // --- PÁGINA 1: RESUMO DA ORDEM DE SERVIÇO E EQUIPAMENTOS ---
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, numeroPaginaAtual).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        desenharCabecalho(
            canvas = canvas1,
            paint = paint,
            isMaquinas = isMaquinas,
            corNeon = corNeon,
            corEscura = corEscura,
            corVerdeWhats = corVerdeWhats,
            config = config
        )

        var y = 122f
        val tituloDoc = if (itens.isEmpty()) "ORDEM DE ENTRADA Nº $numFormatado" else "ORÇAMENTO / OS Nº $numFormatado"

        val rectOsDestaque = RectF(26f, y - 15f, 315f, y + 9f)
        paint.color = corNeon
        canvas1.drawRoundRect(rectOsDestaque, 5f, 5f, paint)
        paint.color = Color.WHITE
        paint.textSize = 12.5f
        paint.isFakeBoldText = true
        canvas1.drawText(tituloDoc, 34f, y + 2f, paint)

        val sdfHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        paint.color = corTextoEscuro
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        val strEntrada = "Entrada: ${sdfHora.format(Date(if (dataCriacao > 0L) dataCriacao else System.currentTimeMillis()))}"
        canvas1.drawText(strEntrada, 325f, y - 4f, paint)

        if (dataOrcamento > 0L && itens.isNotEmpty()) {
            canvas1.drawText("Orçamento: ${sdfHora.format(Date(dataOrcamento))}", 325f, y + 7f, paint)
        }
        if (dataSaida > 0L) {
            canvas1.drawText("Saída: ${sdfHora.format(Date(dataSaida))}", 462f, y + 7f, paint)
        }

        y += 16f

        // Card Dados do Cliente
        val rectCliente = RectF(26f, y, 569f, y + 62f)
        desenharCard(canvas1, paint, rectCliente, corCard, corBordaCard)

        paint.color = corTextoEscuro
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas1.drawText("Dados do Cliente", 38f, y + 16f, paint)

        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        canvas1.drawText("Nome: ${cliente.ifBlank { "Não informado" }}", 38f, y + 32f, paint)
        canvas1.drawText("Telefone: ${telefone.ifBlank { "Não informado" }}", 38f, y + 46f, paint)
        val endFinal = if (endereco.isNotBlank()) endereco else config.cidadeUf
        canvas1.drawText("Endereço: ${endFinal.take(45)}", 290f, y + 32f, paint)
        canvas1.drawText("Total de Equipamentos na OS: ${equipamentos.size}", 290f, y + 46f, paint)

        y += 70f

        // Card Lista de Equipamentos
        val equipsParaPagina1 = equipamentos.take(4)
        val alturaPorEquip = 48f
        val alturaCardEquips = 24f + (equipsParaPagina1.size * alturaPorEquip)
        val rectEquips = RectF(26f, y, 569f, y + alturaCardEquips)
        desenharCard(canvas1, paint, rectEquips, corCard, corBordaCard)

        paint.color = corTextoEscuro
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas1.drawText("Equipamentos Registrados nesta Ordem", 38f, y + 16f, paint)

        var yEq = y + 32f
        equipsParaPagina1.forEach { eq ->
            val ehReprovado = eq.statusEquipamento == StatusOrdem.REPROVADO || eq.statusEquipamento == StatusOrdem.CANCELADO
            paint.color = if (ehReprovado) corVermelhoReprovado else corNeon
            paint.textSize = 10f
            paint.isFakeBoldText = true

            val tagStatus = if (ehReprovado) " [NÃO AUTORIZADO / RECUSADO]" else " [${eq.statusEquipamento}]"
            val serieStr = if (eq.numeroSerie.isNotBlank()) " (Série: ${eq.numeroSerie})" else ""
            canvas1.drawText("• ${eq.rotulo()}$serieStr$tagStatus", 38f, yEq, paint)

            paint.isFakeBoldText = false
            paint.color = corTextoCinza
            paint.textSize = 8.8f
            val defTxt = eq.defeitoRelatado.ifBlank { "Avaliação técnica" }.take(50)
            val acessTxt = if (eq.acessorios.isNotBlank()) " | Acess.: ${eq.acessorios.take(30)}" else ""
            canvas1.drawText("   Defeito inicial: $defTxt$acessTxt", 38f, yEq + 12f, paint)

            if (eq.diagnostico.isNotBlank()) {
                paint.color = if (ehReprovado) corVermelhoReprovado else corTextoEscuro
                paint.isFakeBoldText = ehReprovado
                val prefixo = if (ehReprovado) "   MOTIVO DA RECUSA: " else "   DIAGNÓSTICO: "
                canvas1.drawText("$prefixo${eq.diagnostico.take(70)}", 38f, yEq + 24f, paint)
            }
            yEq += alturaPorEquip
        }

        y += alturaCardEquips + 8f

        if (!usarPaginaDedicadaEntrada && fotosEntrada.isNotEmpty()) {
            val alturaFotoP1 = 120f
            val larguraFotoP1 = 265f
            val f1 = fotosEntrada.getOrNull(0)
            val f2 = fotosEntrada.getOrNull(1)
            val eqNome1 = equipamentos.firstOrNull { it.numeroSequencial == f1?.equipamentoNumeroSeq }?.rotulo() ?: "Equipamento 01"
            val eqNome2 = equipamentos.firstOrNull { it.numeroSequencial == f2?.equipamentoNumeroSeq }?.rotulo() ?: "Equipamento 01"

            if (f1 != null) {
                desenharQuadroFoto(
                    context = context,
                    canvas = canvas1,
                    paint = paint,
                    uriString = f1.uriPath,
                    tituloTopo = "$eqNome1 (${f1.tipoFoto})",
                    subtituloRodape = f1.legenda,
                    x = 26f,
                    y = y,
                    largura = larguraFotoP1,
                    altura = alturaFotoP1,
                    corBorda = corNeon
                )
            }
            if (f2 != null) {
                desenharQuadroFoto(
                    context = context,
                    canvas = canvas1,
                    paint = paint,
                    uriString = f2.uriPath,
                    tituloTopo = "$eqNome2 (${f2.tipoFoto})",
                    subtituloRodape = f2.legenda,
                    x = 304f,
                    y = y,
                    largura = larguraFotoP1,
                    altura = alturaFotoP1,
                    corBorda = corNeon
                )
            }
            y += alturaFotoP1 + 8f
        } else if (usarPaginaDedicadaEntrada) {
            val rectAvisoFotos = RectF(26f, y, 569f, y + 24f)
            paint.color = Color.rgb(232, 236, 244)
            canvas1.drawRoundRect(rectAvisoFotos, 6f, 6f, paint)
            paint.color = corTextoEscuro
            paint.textSize = 9.2f
            paint.isFakeBoldText = true
            canvas1.drawText(
                "📷 Registro Fotográfico (${fotosEntrada.size} fotos de entrada) detalhado por máquina na Página 2.",
                38f,
                y + 16f,
                paint
            )
            y += 30f
        }

        // Serviços/Peças
        val alturaServicos = 138f
        val rectServicos = RectF(26f, y, 378f, y + alturaServicos)
        desenharCard(canvas1, paint, rectServicos, corCard, corBordaCard)

        paint.color = corTextoEscuro
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas1.drawText(if (itens.isEmpty()) "Andamento do Atendimento" else "Serviços e Peças Autorizados", 38f, y + 18f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 9f
        var yItem = y + 33f
        if (itens.isEmpty()) {
            paint.color = corTextoCinza
            canvas1.drawText("• Equipamento(s) em avaliação técnica.", 38f, yItem, paint)
            yItem += 15f
            canvas1.drawText("• Orçamento será enviado após análise dos testes.", 38f, yItem, paint)
        } else {
            itens.take(6).forEach { item ->
                val eqItem = equipamentos.firstOrNull { it.numeroSequencial == item.equipamentoNumeroSeq }
                val reprovado = eqItem?.statusEquipamento == StatusOrdem.REPROVADO
                val tagEq = "Eq.${item.equipamentoNumeroSeq.toString().padStart(2, '0')}"
                val qtdStr = if (item.quantidade > 1) "${item.quantidade}x " else ""

                paint.color = if (reprovado) corVermelhoReprovado else corNeon
                paint.isFakeBoldText = true
                canvas1.drawText(if (reprovado) "✗ [$tagEq]" else "✓ [$tagEq]", 38f, yItem, paint)

                paint.color = if (reprovado) corTextoCinza else corTextoEscuro
                paint.isFakeBoldText = false
                val descItem = if (reprovado) "$qtdStr${item.descricao.take(22)} (RECUSADO)" else "$qtdStr${item.descricao.take(28)}"
                canvas1.drawText(descItem, 84f, yItem, paint)

                val precoStr = if (reprovado) "R$ 0,00" else CalculadoraOrcamento.formatarMoeda(item.valor)
                canvas1.drawText(precoStr, 310f, yItem, paint)
                yItem += 15f
            }
        }

        val garantiasAtivas = equipamentos.mapNotNull { eq ->
            if (eq.statusEquipamento != StatusOrdem.REPROVADO && eq.garantia.isNotBlank() && !eq.garantia.equals("Sem garantia", true)) {
                "Eq.${eq.numeroSequencial.toString().padStart(2, '0')}: ${eq.garantia}"
            } else null
        }
        val mostrarGarantia = garantiasAtivas.isNotEmpty()

        val alturaCardValor = if (mostrarGarantia) 72f else alturaServicos
        val rectValor = RectF(386f, y, 569f, y + alturaCardValor)
        desenharCard(canvas1, paint, rectValor, corCard, corBordaCard)

        paint.color = corTextoEscuro
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas1.drawText("Total Aprovado", 398f, y + 17f, paint)

        if (desconto > 0.0) {
            paint.color = corTextoCinza
            paint.textSize = 8.8f
            paint.isFakeBoldText = false
            canvas1.drawText("Desconto: -${CalculadoraOrcamento.formatarMoeda(desconto)}", 398f, y + 31f, paint)
        }

        paint.color = corTextoEscuro
        paint.textSize = if (itens.isEmpty()) 13.5f else 19f
        paint.isFakeBoldText = true
        val txtTotal = if (itens.isEmpty()) "Em Análise" else CalculadoraOrcamento.formatarMoeda(total)
        canvas1.drawText(txtTotal, 398f, y + if (mostrarGarantia) 54f else 75f, paint)

        if (mostrarGarantia) {
            val rectGarantia = RectF(386f, y + 78f, 569f, y + alturaServicos)
            desenharCard(canvas1, paint, rectGarantia, corCard, corBordaCard)
            paint.color = corNeon
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas1.drawText("★ Garantia Aprovada", 396f, y + 95f, paint)
            paint.color = corTextoCinza
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            garantiasAtivas.take(2).forEachIndexed { idx, gTxt ->
                canvas1.drawText(gTxt.take(28), 396f, y + 110f + (idx * 11f), paint)
            }
        }

        y += alturaServicos + 8f

        // Card Observações (Totalmente limpo de frases de peças ou testes genéricos)
        val alturaObs = (766f - y).coerceIn(46f, 68f)
        val rectObs = RectF(26f, y, 569f, y + alturaObs)
        desenharCard(canvas1, paint, rectObs, corCard, corBordaCard)

        paint.color = corTextoEscuro
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas1.drawText("Observações da Oficina", 38f, y + 15f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 8.5f
        val listaLinhasObs = mutableListOf<String>()

        // Adiciona observações manuais digitadas pelo atendente
        if (observacoesGerais.isNotBlank()) {
            observacoesGerais.split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.contains("peças novas", ignoreCase = true) && !it.contains("testado e está", ignoreCase = true) }
                .forEach { listaLinhasObs.add(it) }
        }

        // Alerta de máquinas recusadas/reprovadas
        val reprovados = equipamentos.filter { it.statusEquipamento == StatusOrdem.REPROVADO }
        if (reprovados.isNotEmpty()) {
            listaLinhasObs.add("Atenção: O(s) equipamento(s) ${reprovados.joinToString(", ") { "Eq.${it.numeroSequencial.toString().padStart(2, '0')}" }} tiveram orçamento recusado e serão devolvidos sem conserto.")
        }

        // Filtra qualquer texto das configurações que contenha "peças novas" ou "testado e está"
        config.observacoesPadrao.split("\n")
            .map { it.trim() }
            .filter {
                it.isNotBlank() &&
                        !it.contains("peças novas", ignoreCase = true) &&
                        !it.contains("peças de alta qualidade", ignoreCase = true) &&
                        !it.contains("testado e está funcionando", ignoreCase = true)
            }
            .forEach { listaLinhasObs.add(it) }

        var yObs = y + 29f
        listaLinhasObs.distinct().take(3).forEach { linha ->
            paint.color = corNeon
            canvas1.drawText("•", 38f, yObs, paint)
            paint.color = corTextoCinza
            canvas1.drawText(linha.take(94), 48f, yObs, paint)
            yObs += 12f
        }

        desenharRodape(canvas1, paint, corNeon, corEscura, corVerdeWhats, config, numeroPaginaAtual)
        pdfDocument.finishPage(page1)

        // --- PÁGINA 2+: FOTOS DE ENTRADA ---
        if (usarPaginaDedicadaEntrada) {
            val gruposPorPagina = fotosEntrada.chunked(4)
            gruposPorPagina.forEach { loteFotos ->
                numeroPaginaAtual++
                val pageInfoFoto = PdfDocument.PageInfo.Builder(595, 842, numeroPaginaAtual).create()
                val pageFoto = pdfDocument.startPage(pageInfoFoto)
                val canvasFoto = pageFoto.canvas

                desenharCabecalho(canvasFoto, paint, isMaquinas, corNeon, corEscura, corVerdeWhats, config)

                var yF = 124f
                val rectTituloFoto = RectF(26f, yF - 15f, 569f, yF + 11f)
                paint.color = corNeon
                canvasFoto.drawRoundRect(rectTituloFoto, 5f, 5f, paint)
                paint.color = Color.WHITE
                paint.textSize = 12f
                paint.isFakeBoldText = true
                canvasFoto.drawText("REGISTRO FOTOGRÁFICO DE ENTRADA — OS Nº $numFormatado", 36f, yF + 2f, paint)

                yF += 22f
                paint.color = corTextoEscuro
                paint.textSize = 9.5f
                paint.isFakeBoldText = false
                canvasFoto.drawText("Cliente: ${cliente.ifBlank { "Não informado" }}   •   Registro fotográfico individual por equipamento", 26f, yF, paint)

                yF += 14f

                val largQuadro = 264f
                val altQuadro = 275f
                loteFotos.forEachIndexed { index, foto ->
                    val col = index % 2
                    val row = index / 2
                    val xPos = if (col == 0) 26f else 305f
                    val yPos = yF + (row * (altQuadro + 16f))

                    val eqCorrespondente = equipamentos.firstOrNull { it.numeroSequencial == foto.equipamentoNumeroSeq }
                    val rotuloEq = eqCorrespondente?.rotulo() ?: "Equipamento ${foto.equipamentoNumeroSeq.toString().padStart(2, '0')}"
                    val dataFotoStr = if (foto.dataRegistro > 0L) sdfHora.format(Date(foto.dataRegistro)) else ""
                    val subLegenda = buildString {
                        append("Etapa: ${foto.tipoFoto}")
                        if (foto.legenda.isNotBlank()) append(" • ${foto.legenda}")
                        else if (eqCorrespondente?.acessorios?.isNotBlank() == true) append(" • Acess.: ${eqCorrespondente.acessorios}")
                        if (dataFotoStr.isNotBlank()) append(" ($dataFotoStr)")
                    }

                    desenharQuadroFoto(
                        context = context,
                        canvas = canvasFoto,
                        paint = paint,
                        uriString = foto.uriPath,
                        tituloTopo = rotuloEq,
                        subtituloRodape = subLegenda,
                        x = xPos,
                        y = yPos,
                        largura = largQuadro,
                        altura = altQuadro,
                        corBorda = corNeon
                    )
                }

                desenharRodape(canvasFoto, paint, corNeon, corEscura, corVerdeWhats, config, numeroPaginaAtual)
                pdfDocument.finishPage(pageFoto)
            }
        }

        // --- PÁGINA(S) DE SAÍDA ---
        if (fotosSaida.isNotEmpty()) {
            val gruposSaida = fotosSaida.chunked(4)
            gruposSaida.forEach { loteSaida ->
                numeroPaginaAtual++
                val pageInfoSaida = PdfDocument.PageInfo.Builder(595, 842, numeroPaginaAtual).create()
                val pageSaida = pdfDocument.startPage(pageInfoSaida)
                val canvasSaida = pageSaida.canvas

                desenharCabecalho(canvasSaida, paint, isMaquinas, corNeon, corEscura, corVerdeWhats, config)

                var yS = 124f
                val rectTituloSaida = RectF(26f, yS - 15f, 569f, yS + 11f)
                paint.color = corNeon
                canvasSaida.drawRoundRect(rectTituloSaida, 5f, 5f, paint)
                paint.color = Color.WHITE
                paint.textSize = 12f
                paint.isFakeBoldText = true
                canvasSaida.drawText("REGISTRO FOTOGRÁFICO DE SAÍDA — OS Nº $numFormatado", 36f, yS + 2f, paint)

                yS += 22f
                paint.color = corTextoEscuro
                paint.textSize = 9.5f
                paint.isFakeBoldText = false
                canvasSaida.drawText("Cliente: ${cliente.ifBlank { "Não informado" }}   •   Equipamentos concluídos para devolução", 26f, yS, paint)

                yS += 14f

                val largQuadro = 264f
                val altQuadro = 275f
                loteSaida.forEachIndexed { index, foto ->
                    val col = index % 2
                    val row = index / 2
                    val xPos = if (col == 0) 26f else 305f
                    val yPos = yS + (row * (altQuadro + 16f))

                    val eqCorrespondente = equipamentos.firstOrNull { it.numeroSequencial == foto.equipamentoNumeroSeq }
                    val rotuloEq = eqCorrespondente?.rotulo() ?: "Equipamento ${foto.equipamentoNumeroSeq.toString().padStart(2, '0')}"
                    val dataFotoStr = if (foto.dataRegistro > 0L) sdfHora.format(Date(foto.dataRegistro)) else ""
                    val subLegenda = "SAÍDA / DEVOLUÇÃO ${if (foto.legenda.isNotBlank()) "• ${foto.legenda} " else ""}($dataFotoStr)"

                    desenharQuadroFoto(
                        context = context,
                        canvas = canvasSaida,
                        paint = paint,
                        uriString = foto.uriPath,
                        tituloTopo = rotuloEq,
                        subtituloRodape = subLegenda,
                        x = xPos,
                        y = yPos,
                        largura = largQuadro,
                        altura = altQuadro,
                        corBorda = corNeon
                    )
                }

                desenharRodape(canvasSaida, paint, corNeon, corEscura, corVerdeWhats, config, numeroPaginaAtual)
                pdfDocument.finishPage(pageSaida)
            }
        }

        val nomeLimpo = cliente.ifBlank { "Cliente" }
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "OS_${numFormatado}_${nomeLimpo}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    fun compartilharPdf(
        context: Context,
        file: File,
        cliente: String,
        numeroOs: String,
        nomeOficina: String,
        diretoWhatsApp: Boolean = false
    ) {
        if (!file.exists()) return
        val numFormatado = numeroOs.padStart(6, '0')
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Olá $cliente! Segue o documento da OS Nº $numFormatado — $nomeOficina.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (diretoWhatsApp) {
                setPackage("com.whatsapp")
            }
        }
        try {
            context.startActivity(
                if (diretoWhatsApp) intent else Intent.createChooser(intent, "Compartilhar PDF da OS via...")
            )
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Olá $cliente! Segue o documento da OS Nº $numFormatado — $nomeOficina.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Compartilhar PDF da OS via..."))
        }
    }

    private fun desenharCabecalho(
        canvas: Canvas,
        paint: Paint,
        isMaquinas: Boolean,
        corNeon: Int,
        corEscura: Int,
        corVerdeWhats: Int,
        config: ConfiguracaoOficina
    ) {
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = corNeon
        canvas.drawRoundRect(RectF(10f, 10f, 585f, 832f), 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        val rectHeader = RectF(12f, 12f, 583f, 98f)
        paint.color = corEscura
        canvas.drawRoundRect(rectHeader, 10f, 10f, paint)
        paint.color = corNeon
        canvas.drawRect(12f, 94f, 583f, 98f, paint)

        paint.strokeWidth = 1.5f
        paint.color = Color.argb(65, Color.red(corNeon), Color.green(corNeon), Color.blue(corNeon))
        canvas.drawLine(250f, 12f, 315f, 94f, paint)
        canvas.drawLine(272f, 12f, 337f, 94f, paint)

        paint.color = corNeon
        if (isMaquinas) {
            canvas.drawCircle(44f, 53f, 17f, paint)
            paint.color = corEscura
            canvas.drawCircle(44f, 53f, 7f, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawRoundRect(RectF(27f, 36f, 61f, 64f), 4f, 4f, paint)
            paint.style = Paint.Style.FILL
            canvas.drawRect(36f, 66f, 52f, 71f, paint)
        }

        paint.color = Color.WHITE
        paint.textSize = 10.5f
        paint.isFakeBoldText = true
        canvas.drawText(if (isMaquinas) "CONSERTO DE" else "SERVIÇOS DE", 72f, 34f, paint)

        paint.color = corNeon
        paint.textSize = 23f
        canvas.drawText(if (isMaquinas) "MÁQUINAS" else "INFORMÁTICA", 72f, 59f, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        val slogan = if (isMaquinas) "SEU EQUIPAMENTO EM BOAS MÃOS!" else "SEU DISPOSITIVO SEMPRE FUNCIONANDO!"
        canvas.drawText("$slogan  •  ${config.nomeOficina}", 72f, 76f, paint)

        val rectWhats = RectF(375f, 22f, 568f, 82f)
        paint.color = Color.rgb(22, 26, 36)
        canvas.drawRoundRect(rectWhats, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = corNeon
        canvas.drawRoundRect(rectWhats, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.color = corVerdeWhats
        canvas.drawCircle(396f, 52f, 11f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("W", 391f, 56f, paint)

        paint.textSize = 13.5f
        canvas.drawText(config.whatsapp, 414f, 47f, paint)
        paint.color = Color.LTGRAY
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText(config.cidadeUf, 414f, 64f, paint)
    }

    private fun desenharRodape(
        canvas: Canvas,
        paint: Paint,
        corNeon: Int,
        corEscura: Int,
        corVerdeWhats: Int,
        config: ConfiguracaoOficina,
        paginaAtual: Int
    ) {
        val yRodape = 774f
        paint.color = corNeon
        canvas.drawRect(12f, yRodape - 3f, 583f, yRodape, paint)
        paint.color = corEscura
        canvas.drawRoundRect(RectF(12f, yRodape, 583f, 830f), 10f, 10f, paint)

        val selos = listOf("Qualidade" to 50f, "Agilidade" to 125f, "Confiança" to 200f)
        selos.forEach { (nomeSelo, posX) ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = corNeon
            canvas.drawCircle(posX, yRodape + 19f, 9f, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 8.5f
            canvas.drawText("✓", posX - 3.5f, yRodape + 22.5f, paint)
            paint.isFakeBoldText = true
            canvas.drawText(nomeSelo, posX - 20f, yRodape + 42f, paint)
        }

        paint.color = corVerdeWhats
        canvas.drawCircle(320f, yRodape + 28f, 13f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("W", 315f, yRodape + 32f, paint)

        paint.color = Color.LTGRAY
        paint.textSize = 8f
        canvas.drawText("CHAME NO WHATSAPP", 340f, yRodape + 21f, paint)
        paint.color = Color.WHITE
        paint.textSize = 13f
        canvas.drawText(config.whatsapp, 340f, yRodape + 39f, paint)

        paint.color = Color.LTGRAY
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Pág. $paginaAtual", 480f, yRodape + 32f, paint)

        desenharQrCodeDecorativo(canvas, paint, 528f, yRodape + 9f, 38f)
    }

    private fun desenharCard(canvas: Canvas, paint: Paint, rect: RectF, corFundo: Int, corBorda: Int) {
        paint.style = Paint.Style.FILL
        paint.color = corFundo
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = corBorda
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun desenharQuadroFoto(
        context: Context,
        canvas: Canvas,
        paint: Paint,
        uriString: String,
        tituloTopo: String,
        subtituloRodape: String,
        x: Float,
        y: Float,
        largura: Float,
        altura: Float,
        corBorda: Int
    ) {
        val rectQuadro = RectF(x, y, x + largura, y + altura)
        paint.color = Color.rgb(22, 24, 30)
        canvas.drawRoundRect(rectQuadro, 8f, 8f, paint)

        val temRodape = subtituloRodape.isNotBlank()
        val alturaTopo = 20f
        val alturaRodape = if (temRodape) 18f else 4f

        try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val margem = 5f
                val dispLargura = largura - (margem * 2)
                val dispAltura = altura - alturaTopo - alturaRodape - 4f
                val escala = minOf(dispLargura / bitmap.width, dispAltura / bitmap.height)
                val wFinal = bitmap.width * escala
                val hFinal = bitmap.height * escala
                val xFoto = x + margem + (dispLargura - wFinal) / 2f
                val yFoto = y + alturaTopo + 2f + (dispAltura - hFinal) / 2f

                val paintFoto = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
                canvas.drawBitmap(bitmap, null, RectF(xFoto, yFoto, xFoto + wFinal, yFoto + hFinal), paintFoto)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        paint.color = Color.argb(225, 12, 14, 20)
        canvas.drawRoundRect(RectF(x, y, x + largura, y + alturaTopo), 8f, 8f, paint)
        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText(tituloTopo.take(40), x + 8f, y + 13.5f, paint)

        if (temRodape) {
            paint.color = Color.argb(215, 12, 14, 20)
            canvas.drawRoundRect(RectF(x, y + altura - alturaRodape, x + largura, y + altura), 8f, 8f, paint)
            paint.color = Color.LTGRAY
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText(subtituloRodape.take(46), x + 8f, y + altura - 5.5f, paint)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.3f
        paint.color = corBorda
        canvas.drawRoundRect(rectQuadro, 8f, 8f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun desenharQrCodeDecorativo(canvas: Canvas, paint: Paint, x: Float, y: Float, tamanho: Float) {
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(x, y, x + tamanho, y + tamanho), 4f, 4f, paint)
        paint.color = Color.BLACK
        val cel = tamanho / 7f
        for (i in 0..6) {
            for (j in 0..6) {
                if ((i in 0..1 && j in 0..1) || (i in 5..6 && j in 0..1) || (i in 0..1 && j in 5..6) || (i + j) % 2 == 0) {
                    canvas.drawRect(
                        x + 2f + i * cel * 0.85f,
                        y + 2f + j * cel * 0.85f,
                        x + 2f + (i + 0.8f) * cel * 0.85f,
                        y + 2f + (j + 0.8f) * cel * 0.85f,
                        paint
                    )
                }
            }
        }
    }
}
// FIM DO ARQUIVO