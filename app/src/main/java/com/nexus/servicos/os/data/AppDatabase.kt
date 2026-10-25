package com.nexus.servicos.os.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Entity(tableName = "itens_servico")
data class ItemServico(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val osId: Int,
    val equipamentoNumeroSeq: Int = 1,
    val tipoItem: String = "SERVICO",
    val descricao: String,
    val quantidade: Int = 1,
    val valorUnitarioCentavos: Long = 0L,
    val valor: Double
)

@Entity(tableName = "fotos_os")
data class FotoOs(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val osId: Int,
    val equipamentoNumeroSeq: Int = 1,
    val uriPath: String,
    val tipoFoto: String,
    val legenda: String = "",
    val dataRegistro: Long = System.currentTimeMillis()
)

@Dao
interface NexusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirCliente(cliente: Cliente): Long

    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun buscarTodosClientes(): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes ORDER BY id ASC")
    suspend fun listarClientesSincrono(): List<Cliente>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirOs(os: OrdemServico): Long

    @Update
    suspend fun atualizarOs(os: OrdemServico)

    @Delete
    suspend fun deletarOs(os: OrdemServico)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirEquipamento(equipamento: EquipamentoOs): Long

    @Query("SELECT * FROM equipamentos_os WHERE osId = :osId ORDER BY numeroSequencial ASC")
    suspend fun buscarEquipamentosDaOs(osId: Int): List<EquipamentoOs>

    @Query("SELECT * FROM equipamentos_os ORDER BY osId ASC, numeroSequencial ASC")
    suspend fun listarTodosEquipamentosSincrono(): List<EquipamentoOs>

    @Query("DELETE FROM equipamentos_os WHERE osId = :osId")
    suspend fun deletarEquipamentosDaOs(osId: Int)

    @Insert
    suspend fun inserirItem(item: ItemServico)

    @Insert
    suspend fun inserirFoto(foto: FotoOs)

    @Delete
    suspend fun deletarFoto(foto: FotoOs)

    @Query("DELETE FROM itens_servico WHERE osId = :osId")
    suspend fun deletarItensDaOs(osId: Int)

    @Query("DELETE FROM fotos_os WHERE osId = :osId")
    suspend fun deletarFotosDaOs(osId: Int)

    @Query("SELECT * FROM ordens_servico ORDER BY id DESC")
    fun buscarTodasOs(): Flow<List<OrdemServico>>

    @Query("SELECT * FROM ordens_servico ORDER BY id ASC")
    suspend fun listarOrdensSincrono(): List<OrdemServico>

    @Query("SELECT * FROM ordens_servico WHERE clienteId = :clienteId ORDER BY id DESC")
    suspend fun buscarOrdensDoCliente(clienteId: Int): List<OrdemServico>

    @Query("SELECT COUNT(*) FROM ordens_servico")
    suspend fun contarOrdens(): Int

    @Query("SELECT * FROM itens_servico WHERE osId = :osId ORDER BY equipamentoNumeroSeq ASC, id ASC")
    suspend fun buscarItensDaOs(osId: Int): List<ItemServico>

    @Query("SELECT * FROM itens_servico ORDER BY osId ASC, id ASC")
    suspend fun listarTodosItensSincrono(): List<ItemServico>

    @Query("SELECT * FROM fotos_os WHERE osId = :osId ORDER BY equipamentoNumeroSeq ASC, id ASC")
    suspend fun buscarFotosDaOs(osId: Int): List<FotoOs>

    @Query("SELECT * FROM fotos_os ORDER BY osId ASC, id ASC")
    suspend fun listarTodasFotosSincrono(): List<FotoOs>

    @Insert
    suspend fun inserirHistoricoStatus(historico: HistoricoStatus)

    @Query("SELECT * FROM historico_status WHERE osId = :osId ORDER BY dataHora DESC")
    suspend fun buscarHistoricoDaOs(osId: Int): List<HistoricoStatus>

    @Query("SELECT * FROM configuracoes WHERE id = 1")
    fun buscarConfiguracao(): Flow<ConfiguracaoOficina?>

    @Query("SELECT * FROM configuracoes WHERE id = 1")
    suspend fun buscarConfiguracaoSincrona(): ConfiguracaoOficina?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarConfiguracao(config: ConfiguracaoOficina)

    @Query("DELETE FROM ordens_servico")
    suspend fun limparTodasOs()

    @Query("DELETE FROM equipamentos_os")
    suspend fun limparTodosEquipamentos()

    @Query("DELETE FROM itens_servico")
    suspend fun limparTodosItens()

    @Query("DELETE FROM fotos_os")
    suspend fun limparTodasFotos()

    @Query("DELETE FROM historico_status")
    suspend fun limparTodoHistorico()

    @Query("DELETE FROM clientes")
    suspend fun limparTodosClientes()
}

@Database(
    entities = [
        Cliente::class,
        OrdemServico::class,
        EquipamentoOs::class,
        ItemServico::class,
        FotoOs::class,
        HistoricoStatus::class,
        ConfiguracaoOficina::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nexusDao(): NexusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `equipamentos_os` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `osId` INTEGER NOT NULL,
                        `numeroSequencial` INTEGER NOT NULL,
                        `tipoEquipamento` TEXT NOT NULL,
                        `marcaModelo` TEXT NOT NULL,
                        `numeroSerie` TEXT NOT NULL,
                        `defeitoRelatado` TEXT NOT NULL,
                        `estadoFisico` TEXT NOT NULL,
                        `acessorios` TEXT NOT NULL,
                        `diagnostico` TEXT NOT NULL,
                        `observacoes` TEXT NOT NULL,
                        `garantia` TEXT NOT NULL,
                        `statusEquipamento` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `equipamentos_os` (
                        `osId`, `numeroSequencial`, `tipoEquipamento`, `marcaModelo`,
                        `numeroSerie`, `defeitoRelatado`, `estadoFisico`, `acessorios`,
                        `diagnostico`, `observacoes`, `garantia`, `statusEquipamento`
                    )
                    SELECT 
                        `id`, 1, `categoriaEquipamento`, `equipamento`,
                        `numeroSerie`, `defeitoRelatado`, '', `acessorios`,
                        '', `observacoes`, `garantia`, `status`
                    FROM `ordens_servico`
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `historico_status` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `osId` INTEGER NOT NULL,
                        `statusAnterior` TEXT NOT NULL,
                        `statusNovo` TEXT NOT NULL,
                        `descricao` TEXT NOT NULL,
                        `dataHora` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL("ALTER TABLE `itens_servico` ADD COLUMN `equipamentoNumeroSeq` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `itens_servico` ADD COLUMN `tipoItem` TEXT NOT NULL DEFAULT 'SERVICO'")
                db.execSQL("ALTER TABLE `itens_servico` ADD COLUMN `quantidade` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `itens_servico` ADD COLUMN `valorUnitarioCentavos` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `itens_servico` SET `valorUnitarioCentavos` = CAST(ROUND(`valor` * 100) AS INTEGER)")

                db.execSQL("ALTER TABLE `fotos_os` ADD COLUMN `equipamentoNumeroSeq` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `fotos_os` ADD COLUMN `legenda` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `fotos_os` ADD COLUMN `dataRegistro` INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE `configuracoes` ADD COLUMN `validadeOrcamentoDias` INTEGER NOT NULL DEFAULT 7")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexus_os_db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- GERENCIADOR DE BACKUP COM PROGRESSO REAL (0% A 100%) ---
object GerenciadorBackup {

    private const val NOME_CSV = "Planilha_Oficina_Nexus.csv"
    private const val NOME_ZIP = "Backup_Completo_Nexus.zip"

    /**
     * Executa o Backup reportando o progresso real de 0.05f (5%) até 1.0f (100%)
     * conforme lê o banco, monta a planilha Excel e compacta cada foto dentro do .ZIP.
     */
    suspend fun executarAutoBackup(
        context: Context,
        dao: NexusDao,
        onProgresso: (Float) -> Unit = {}
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        onProgresso(0.08f)
        val dirInterno = File(context.filesDir, "pdfs").apply { mkdirs() }
        val arquivoCsv = File(dirInterno, NOME_CSV)
        val arquivoZip = File(dirInterno, NOME_ZIP)

        val clientes = dao.listarClientesSincrono()
        val ordens = dao.listarOrdensSincrono()
        val equipamentos = dao.listarTodosEquipamentosSincrono()
        val itens = dao.listarTodosItensSincrono()
        val fotos = dao.listarTodasFotosSincrono()
        val config = dao.buscarConfiguracaoSincrona() ?: ConfiguracaoOficina()
        onProgresso(0.20f)

        // 1. Gera a Planilha CSV (Excel)
        val conteudoCsv = construirConteudoPlanilhaExcel(clientes, ordens, equipamentos, itens)
        FileOutputStream(arquivoCsv).use { out ->
            out.write(conteudoCsv.toByteArray(Charsets.UTF_8))
        }
        onProgresso(0.35f)

        // 2. Monta a árvore JSON estruturada
        val jsonRaiz = JSONObject().apply {
            put("versaoBackup", 6)
            put("dataGeracao", System.currentTimeMillis())

            val arrClientes = JSONArray()
            clientes.forEach { c ->
                arrClientes.put(JSONObject().apply {
                    put("id", c.id)
                    put("nome", c.nome)
                    put("telefone", c.telefone)
                    put("telefoneSecundario", c.telefoneSecundario)
                    put("endereco", c.endereco)
                    put("observacoes", c.observacoes)
                })
            }
            put("clientes", arrClientes)

            val arrOrdens = JSONArray()
            ordens.forEach { o ->
                arrOrdens.put(JSONObject().apply {
                    put("id", o.id)
                    put("numeroOs", o.numeroOs)
                    put("clienteId", o.clienteId)
                    put("nomeCliente", o.nomeCliente)
                    put("telefoneCliente", o.telefoneCliente)
                    put("enderecoCliente", o.enderecoCliente)
                    put("tipo", o.tipo.name)
                    put("categoriaEquipamento", o.categoriaEquipamento)
                    put("equipamento", o.equipamento)
                    put("numeroSerie", o.numeroSerie)
                    put("defeitoRelatado", o.defeitoRelatado)
                    put("acessorios", o.acessorios)
                    put("garantia", o.garantia)
                    put("observacoes", o.observacoes)
                    put("status", o.status)
                    put("dataCriacao", o.dataCriacao)
                    put("dataOrcamento", o.dataOrcamento)
                    put("dataSaida", o.dataSaida)
                    put("desconto", o.desconto)
                    put("valorTotal", o.valorTotal)
                })
            }
            put("ordens", arrOrdens)

            val arrEquips = JSONArray()
            equipamentos.forEach { eq ->
                arrEquips.put(JSONObject().apply {
                    put("id", eq.id)
                    put("osId", eq.osId)
                    put("numeroSequencial", eq.numeroSequencial)
                    put("tipoEquipamento", eq.tipoEquipamento)
                    put("marcaModelo", eq.marcaModelo)
                    put("numeroSerie", eq.numeroSerie)
                    put("defeitoRelatado", eq.defeitoRelatado)
                    put("estadoFisico", eq.estadoFisico)
                    put("acessorios", eq.acessorios)
                    put("diagnostico", eq.diagnostico)
                    put("observacoes", eq.observacoes)
                    put("garantia", eq.garantia)
                    put("statusEquipamento", eq.statusEquipamento)
                })
            }
            put("equipamentos", arrEquips)

            val arrItens = JSONArray()
            itens.forEach { i ->
                arrItens.put(JSONObject().apply {
                    put("id", i.id)
                    put("osId", i.osId)
                    put("equipamentoNumeroSeq", i.equipamentoNumeroSeq)
                    put("tipoItem", i.tipoItem)
                    put("descricao", i.descricao)
                    put("quantidade", i.quantidade)
                    put("valorUnitarioCentavos", i.valorUnitarioCentavos)
                    put("valor", i.valor)
                })
            }
            put("itens", arrItens)

            val arrFotos = JSONArray()
            fotos.forEach { f ->
                val nomeArquivoFoto = Uri.parse(f.uriPath).lastPathSegment ?: "foto_${f.id}.jpg"
                arrFotos.put(JSONObject().apply {
                    put("id", f.id)
                    put("osId", f.osId)
                    put("equipamentoNumeroSeq", f.equipamentoNumeroSeq)
                    put("nomeArquivo", nomeArquivoFoto)
                    put("tipoFoto", f.tipoFoto)
                    put("legenda", f.legenda)
                    put("dataRegistro", f.dataRegistro)
                })
            }
            put("fotos", arrFotos)

            put("configuracao", JSONObject().apply {
                put("nomeOficina", config.nomeOficina)
                put("whatsapp", config.whatsapp)
                put("cidadeUf", config.cidadeUf)
                put("garantiaPadrao", config.garantiaPadrao)
                put("validadeOrcamentoDias", config.validadeOrcamentoDias)
                put("observacoesPadrao", config.observacoesPadrao)
            })
        }
        onProgresso(0.45f)

        // 3. Compacta o JSON, a Planilha e cada Foto física aumentando o nível do tanque proporcionalmente!
        ZipOutputStream(BufferedOutputStream(FileOutputStream(arquivoZip))).use { zos ->
            zos.putNextEntry(ZipEntry("nexus_backup_dados.json"))
            zos.write(jsonRaiz.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(NOME_CSV))
            zos.write(conteudoCsv.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            onProgresso(0.55f)

            val dirFotos = File(context.filesDir, "fotos")
            val arquivosFotos = dirFotos.listFiles()?.filter { it.isFile } ?: emptyList()
            val totalFotos = arquivosFotos.size.coerceAtLeast(1)

            arquivosFotos.forEachIndexed { idx, fotoFile ->
                zos.putNextEntry(ZipEntry("fotos/${fotoFile.name}"))
                FileInputStream(fotoFile).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
                // Sobe gradualmente de 55% até 90% conforme cada foto é compactada
                val progFoto = 0.55f + (0.35f * ((idx + 1).toFloat() / totalFotos.toFloat()))
                onProgresso(progFoto)
            }
        }
        onProgresso(0.92f)

        // 4. Salva cópia automática na pasta pública Downloads/NexusServicos_Backup do celular
        copiarParaPastaDownloadsPublica(context, arquivoCsv, NOME_CSV, "text/csv")
        copiarParaPastaDownloadsPublica(context, arquivoZip, NOME_ZIP, "application/zip")
        onProgresso(1.0f)

        Pair(arquivoCsv, arquivoZip)
    }

    private fun construirConteudoPlanilhaExcel(
        clientes: List<Cliente>,
        ordens: List<OrdemServico>,
        equipamentos: List<EquipamentoOs>,
        itens: List<ItemServico>
    ): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("\uFEFF")

        sb.appendLine("TABELA 1 - ORDENS DE SERVICO E ORCAMENTOS (NEXUS SERVICOS)")
        sb.appendLine("Nº da OS;Data Entrada;Data Orcamento;Data Saida;Categoria;Status Geral;Cliente;Telefone / WhatsApp;Endereco;Qtd Maquinas;Equipamentos e Diagnosticos;Servicos e Pecas;Desconto (R$);Valor Total Aprovado (R$)")

        ordens.sortedByDescending { it.id }.forEach { os ->
            val eqsDaOs = equipamentos.filter { it.osId == os.id }
            val itensDaOs = itens.filter { it.osId == os.id }

            val dtEnt = if (os.dataCriacao > 0L) sdf.format(Date(os.dataCriacao)) else "-"
            val dtOrc = if (os.dataOrcamento > 0L) sdf.format(Date(os.dataOrcamento)) else "-"
            val dtSai = if (os.dataSaida > 0L) sdf.format(Date(os.dataSaida)) else "-"

            val resumoEqs = if (eqsDaOs.isNotEmpty()) {
                eqsDaOs.joinToString(" | ") { eq ->
                    val diag = if (eq.diagnostico.isNotBlank()) " [Diag/Motivo: ${eq.diagnostico}]" else ""
                    "Eq.${eq.numeroSequencial.toString().padStart(2, '0')}: ${eq.marcaModelo} (${eq.statusEquipamento} - Defeito: ${eq.defeitoRelatado}$diag)"
                }
            } else {
                "${os.equipamento} (Defeito: ${os.defeitoRelatado})"
            }

            val resumoItens = if (itensDaOs.isNotEmpty()) {
                itensDaOs.joinToString(" + ") { i ->
                    "[Eq.${i.equipamentoNumeroSeq}] ${i.quantidade}x ${i.descricao} (R$ ${String.format(Locale.getDefault(), "%.2f", i.valor)})"
                }
            } else "Em analise / Sem itens"

            sb.appendLine(
                listOf(
                    "OS-${os.numeroOs.padStart(6, '0')}",
                    dtEnt,
                    dtOrc,
                    dtSai,
                    os.tipo.name,
                    limparCelulaCsv(os.status),
                    limparCelulaCsv(os.nomeCliente),
                    limparCelulaCsv(os.telefoneCliente),
                    limparCelulaCsv(os.enderecoCliente),
                    eqsDaOs.size.coerceAtLeast(1).toString(),
                    limparCelulaCsv(resumoEqs),
                    limparCelulaCsv(resumoItens),
                    String.format(Locale.getDefault(), "%.2f", os.desconto),
                    String.format(Locale.getDefault(), "%.2f", os.valorTotal)
                ).joinToString(";")
            )
        }

        sb.appendLine()
        sb.appendLine("TABELA 2 - CLIENTES CADASTRADOS")
        sb.appendLine("ID;Nome do Cliente;Telefone Principal;Segundo Telefone;Endereco;Total de OS do Cliente")
        clientes.forEach { c ->
            val qtdOs = ordens.count { (it.clienteId > 0 && it.clienteId == c.id) || it.nomeCliente.equals(c.nome, true) }
            sb.appendLine(
                listOf(
                    c.id.toString(),
                    limparCelulaCsv(c.nome),
                    limparCelulaCsv(formatarTelefoneBr(c.telefone)),
                    limparCelulaCsv(formatarTelefoneBr(c.telefoneSecundario)),
                    limparCelulaCsv(c.endereco),
                    qtdOs.toString()
                ).joinToString(";")
            )
        }

        return sb.toString()
    }

    private fun limparCelulaCsv(texto: String): String {
        return texto.replace(";", ",").replace("\n", " ").replace("\r", " ").trim()
    }

    private fun copiarParaPastaDownloadsPublica(context: Context, arquivoOrigem: File, nomeArquivo: String, mimeType: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val caminhoRelativo = Environment.DIRECTORY_DOWNLOADS + "/NexusServicos_Backup"
                val selecao = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, selecao, arrayOf(nomeArquivo))

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nomeArquivo)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, caminhoRelativo)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(arquivoOrigem).use { input -> input.copyTo(out) }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun compartilharArquivoBackup(context: Context, arquivo: File, mimeType: String, titulo: String) {
        if (!arquivo.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", arquivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, titulo))
    }

    suspend fun restaurarBackupDeUri(
        context: Context,
        uriZip: Uri,
        dao: NexusDao,
        onProgresso: (Float) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        onProgresso(0.10f)
        var jsonTexto = ""
        val dirFotos = File(context.filesDir, "fotos").apply { mkdirs() }

        context.contentResolver.openInputStream(uriZip)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val nomeEntry = entry.name
                    if (nomeEntry == "nexus_backup_dados.json") {
                        val buffer = ByteArrayOutputStream()
                        zis.copyTo(buffer)
                        jsonTexto = buffer.toString("UTF-8")
                        onProgresso(0.35f)
                    } else if (nomeEntry.startsWith("fotos/") && !entry.isDirectory) {
                        val nomeFoto = File(nomeEntry).name
                        val destinoFoto = File(dirFotos, nomeFoto)
                        FileOutputStream(destinoFoto).use { fos -> zis.copyTo(fos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (jsonTexto.isBlank()) return@withContext 0
        onProgresso(0.55f)

        val raiz = JSONObject(jsonTexto)
        dao.limparTodasFotos()
        dao.limparTodosItens()
        dao.limparTodosEquipamentos()
        dao.limparTodasOs()
        dao.limparTodosClientes()

        val arrClientes = raiz.optJSONArray("clientes") ?: JSONArray()
        for (i in 0 until arrClientes.length()) {
            val obj = arrClientes.getJSONObject(i)
            dao.inserirCliente(
                Cliente(
                    id = obj.optInt("id", 0),
                    nome = obj.optString("nome", ""),
                    telefone = obj.optString("telefone", ""),
                    telefoneSecundario = obj.optString("telefoneSecundario", ""),
                    endereco = obj.optString("endereco", ""),
                    observacoes = obj.optString("observacoes", "")
                )
            )
        }
        onProgresso(0.70f)

        val arrOrdens = raiz.optJSONArray("ordens") ?: JSONArray()
        for (i in 0 until arrOrdens.length()) {
            val obj = arrOrdens.getJSONObject(i)
            val tipoEnum = try {
                TipoAtendimento.valueOf(obj.optString("tipo", "MAQUINAS"))
            } catch (_: Exception) { TipoAtendimento.MAQUINAS }

            dao.inserirOs(
                OrdemServico(
                    id = obj.optInt("id", 0),
                    numeroOs = obj.optString("numeroOs", "000001"),
                    clienteId = obj.optInt("clienteId", 0),
                    nomeCliente = obj.optString("nomeCliente", ""),
                    telefoneCliente = obj.optString("telefoneCliente", ""),
                    enderecoCliente = obj.optString("enderecoCliente", ""),
                    tipo = tipoEnum,
                    categoriaEquipamento = obj.optString("categoriaEquipamento", ""),
                    equipamento = obj.optString("equipamento", ""),
                    numeroSerie = obj.optString("numeroSerie", ""),
                    defeitoRelatado = obj.optString("defeitoRelatado", ""),
                    acessorios = obj.optString("acessorios", ""),
                    garantia = obj.optString("garantia", ""),
                    observacoes = obj.optString("observacoes", ""),
                    status = obj.optString("status", StatusOrdem.EM_ANALISE),
                    dataCriacao = obj.optLong("dataCriacao", System.currentTimeMillis()),
                    dataOrcamento = obj.optLong("dataOrcamento", 0L),
                    dataSaida = obj.optLong("dataSaida", 0L),
                    desconto = obj.optDouble("desconto", 0.0),
                    valorTotal = obj.optDouble("valorTotal", 0.0)
                )
            )
        }
        onProgresso(0.85f)

        val arrEquips = raiz.optJSONArray("equipamentos") ?: JSONArray()
        for (i in 0 until arrEquips.length()) {
            val obj = arrEquips.getJSONObject(i)
            dao.inserirEquipamento(
                EquipamentoOs(
                    id = obj.optInt("id", 0),
                    osId = obj.optInt("osId", 0),
                    numeroSequencial = obj.optInt("numeroSequencial", 1),
                    tipoEquipamento = obj.optString("tipoEquipamento", ""),
                    marcaModelo = obj.optString("marcaModelo", ""),
                    numeroSerie = obj.optString("numeroSerie", ""),
                    defeitoRelatado = obj.optString("defeitoRelatado", ""),
                    estadoFisico = obj.optString("estadoFisico", ""),
                    acessorios = obj.optString("acessorios", ""),
                    diagnostico = obj.optString("diagnostico", ""),
                    observacoes = obj.optString("observacoes", ""),
                    garantia = obj.optString("garantia", ""),
                    statusEquipamento = obj.optString("statusEquipamento", StatusOrdem.EM_ANALISE)
                )
            )
        }

        val arrItens = raiz.optJSONArray("itens") ?: JSONArray()
        for (i in 0 until arrItens.length()) {
            val obj = arrItens.getJSONObject(i)
            dao.inserirItem(
                ItemServico(
                    id = obj.optInt("id", 0),
                    osId = obj.optInt("osId", 0),
                    equipamentoNumeroSeq = obj.optInt("equipamentoNumeroSeq", 1),
                    tipoItem = obj.optString("tipoItem", "SERVICO"),
                    descricao = obj.optString("descricao", ""),
                    quantidade = obj.optInt("quantidade", 1),
                    valorUnitarioCentavos = obj.optLong("valorUnitarioCentavos", 0L),
                    valor = obj.optDouble("valor", 0.0)
                )
            )
        }

        val arrFotos = raiz.optJSONArray("fotos") ?: JSONArray()
        for (i in 0 until arrFotos.length()) {
            val obj = arrFotos.getJSONObject(i)
            val nomeArq = obj.optString("nomeArquivo", "")
            val arquivoFoto = File(dirFotos, nomeArq)
            if (arquivoFoto.exists()) {
                val novaUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", arquivoFoto)
                dao.inserirFoto(
                    FotoOs(
                        osId = obj.optInt("osId", 0),
                        equipamentoNumeroSeq = obj.optInt("equipamentoNumeroSeq", 1),
                        uriPath = novaUri.toString(),
                        tipoFoto = obj.optString("tipoFoto", "ENTRADA"),
                        legenda = obj.optString("legenda", ""),
                        dataRegistro = obj.optLong("dataRegistro", System.currentTimeMillis())
                    )
                )
            }
        }
        onProgresso(1.0f)

        arrOrdens.length()
    }
}
// FIM DO ARQUIVO