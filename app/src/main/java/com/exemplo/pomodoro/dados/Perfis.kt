package com.exemplo.pomodoro.dados

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import com.exemplo.pomodoro.R
import com.exemplo.pomodoro.motor.ConfigCiclo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

const val NOME_MAX = 40

/**
 * Perfil: um conjunto nomeado com toda a configuração do usuário.
 * Cada parte é um grupo `@Embedded`: Tempo, Aparência (tickets 07 e 09), Alerta e notificação (ticket 05).
 */
@Entity(tableName = "perfil")
data class Perfil(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    @Embedded val tempo: ConfigCiclo = ConfigCiclo.PADRAO,
    @Embedded(prefix = "alerta_") val alerta: AlertaNotificacao = AlertaNotificacao(),
    @Embedded val aparencia: Aparencia = Aparencia(),
)

@Dao
interface PerfilDao {
    @Query("SELECT * FROM perfil ORDER BY id") fun todos(): Flow<List<Perfil>>
    @Query("SELECT * FROM perfil WHERE id = :id") suspend fun ler(id: Long): Perfil?
    @Query("SELECT COUNT(*) FROM perfil") suspend fun contar(): Int
    @Insert suspend fun inserir(p: Perfil): Long
    @Update suspend fun atualizar(p: Perfil)
    @Query("DELETE FROM perfil WHERE id = :id") suspend fun apagar(id: Long)
}

@Database(entities = [Perfil::class, FaseRegistrada::class], version = 1, exportSchema = false)
@TypeConverters(Conversores::class)
abstract class Banco : RoomDatabase() {
    abstract fun perfis(): PerfilDao
    abstract fun historico(): HistoricoDao
}

private val Context.prefs by preferencesDataStore(name = "perfis")
private val PERFIL_ATIVO = longPreferencesKey("perfil_ativo")

/** Os Perfis e o Perfil ativo. Uma instância só, no [com.exemplo.pomodoro.AppPomodoro]. */
class Perfis(private val ctx: Context, private val dao: PerfilDao, escopo: CoroutineScope) {

    // As gravações passam por uma fila: saem em ordem e sobrevivem ao fechar da tela que as pediu.
    private val fila = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        escopo.launch { for (gravacao in fila) gravacao() }
        // Na abertura, nenhuma importação está em curso: apaga o que sobrou de uma importação interrompida.
        gravar { apagarOrfas(fotosGuardadas(ctx)) }
    }

    /** Apaga as fotos [candidatas] que nenhum Perfil usa mais. Roda na fila, depois da gravação que as soltou. */
    private suspend fun apagarOrfas(candidatas: List<String>) {
        if (candidatas.isEmpty()) return
        apagarFotos(ctx, fotosOrfas(candidatas, todos.first().map { it.aparencia.fundo }))
    }

    private fun fotoDe(p: Perfil?) = listOfNotNull((p?.aparencia?.fundo as? Fundo.Foto)?.caminho)

    /** Primeira abertura: cria o Perfil padrão (25/5/15, N = 4). Roda uma vez, na criação do app. */
    suspend fun garantirPadrao() {
        if (dao.contar() == 0) dao.inserir(Perfil(nome = ctx.getString(R.string.perfil_padrao)))
    }

    private fun gravar(acao: suspend () -> Unit) {
        fila.trySend(acao)
    }

    val todos: Flow<List<Perfil>> = dao.todos()

    /** O Perfil ativo. Se o id guardado não existe (primeira abertura), vale o primeiro da lista. */
    val ativo: Flow<Perfil> = combine(todos, ctx.prefs.data.map { it[PERFIL_ATIVO] }) { lista, id ->
        lista.find { it.id == id } ?: lista.firstOrNull()
    }.filterNotNull()

    suspend fun ler(id: Long) = dao.ler(id)

    fun ativar(id: Long) = gravar { ctx.prefs.edit { it[PERFIL_ATIVO] = id } }

    /** Cria um Perfil com o Tempo padrão e devolve o id. */
    suspend fun criar(): Long = dao.inserir(Perfil(nome = ctx.getString(R.string.perfil_novo)))

    fun salvar(p: Perfil) = gravar {
        val antes = dao.ler(p.id)
        dao.atualizar(p)
        apagarOrfas(fotoDe(antes) - fotoDe(p).toSet())
    }

    fun duplicar(id: Long) = gravar {
        val p = dao.ler(id) ?: return@gravar
        dao.inserir(p.copy(id = 0, nome = ctx.getString(R.string.perfil_copia, p.nome).take(NOME_MAX)))
    }

    fun renomear(id: Long, nome: String) = gravar {
        val p = dao.ler(id) ?: return@gravar
        dao.atualizar(p.copy(nome = nome))
    }

    /** O último Perfil não pode ser excluído. Se o excluído era o ativo, o primeiro que sobrar fica ativo. */
    fun excluir(id: Long) = gravar {
        if (dao.contar() <= 1) return@gravar
        val ativoId = ativo.first().id
        val foto = fotoDe(dao.ler(id))
        dao.apagar(id)
        apagarOrfas(foto)
        if (id == ativoId) ctx.prefs.edit { it[PERFIL_ATIVO] = todos.first().first().id }
    }
}
