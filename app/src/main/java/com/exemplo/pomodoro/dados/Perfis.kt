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
import androidx.room.Room
import androidx.room.RoomDatabase
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
 * Hoje guarda só o Tempo. Aparência (tickets 07 e 09), Alerta e notificação (ticket 05) entram como novos grupos `@Embedded`.
 */
@Entity(tableName = "perfil")
data class Perfil(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    @Embedded val tempo: ConfigCiclo = ConfigCiclo.PADRAO,
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

@Database(entities = [Perfil::class], version = 1, exportSchema = false)
abstract class Banco : RoomDatabase() {
    abstract fun perfis(): PerfilDao
}

private val Context.prefs by preferencesDataStore(name = "app")
private val PERFIL_ATIVO = longPreferencesKey("perfil_ativo")

/** Os Perfis e o Perfil ativo. Uma instância só, no [com.exemplo.pomodoro.App]. */
class Perfis(private val ctx: Context, escopo: CoroutineScope) {

    private val dao = Room.databaseBuilder(ctx, Banco::class.java, "pomodoro.db").build().perfis()

    // As gravações passam por uma fila: saem em ordem e sobrevivem ao fechar da tela que as pediu.
    private val fila = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        escopo.launch {
            // Primeira abertura: cria o Perfil padrão (25/5/15, N = 4).
            if (dao.contar() == 0) dao.inserir(Perfil(nome = ctx.getString(R.string.perfil_padrao)))
            for (gravacao in fila) gravacao()
        }
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

    fun salvar(p: Perfil) = gravar { dao.atualizar(p) }

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
        dao.apagar(id)
        if (id == ativoId) ctx.prefs.edit { it[PERFIL_ATIVO] = todos.first().first().id }
    }
}
