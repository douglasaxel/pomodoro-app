package com.exemplo.pomodoro.dados

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.exemplo.pomodoro.motor.TipoFase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Uma Fase no Histórico, concluída ou incompleta. Início e fim em tempo de parede (epoch ms).
 * Excluir o Perfil anula o [perfilId] e mantém o registro, com o [perfilNome] copiado.
 */
@Entity(
    tableName = "fase_registrada",
    foreignKeys = [
        ForeignKey(entity = Perfil::class, parentColumns = ["id"], childColumns = ["perfilId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("perfilId"), Index("inicioEpochMs")],
)
data class FaseRegistrada(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: TipoFase, // o Room grava o name: não renomeie as constantes
    val inicioEpochMs: Long,
    val fimEpochMs: Long,
    val minutosEfetivos: Int,
    val concluida: Boolean,
    val perfilId: Long?,
    val perfilNome: String,
)

@Dao
interface HistoricoDao {
    @Query("SELECT * FROM fase_registrada ORDER BY inicioEpochMs DESC") fun todas(): Flow<List<FaseRegistrada>>
    @Query("SELECT EXISTS(SELECT 1 FROM perfil WHERE id = :id)") suspend fun perfilExiste(id: Long): Boolean
    @Insert suspend fun inserir(f: FaseRegistrada): Long
    @Query("DELETE FROM fase_registrada WHERE id = :id") suspend fun apagar(id: Long)

    /** O Perfil pode ter sido excluído no meio tempo (excluir o Perfil ativo encerra a Fase dele): aí o id vai nulo. */
    @Transaction
    suspend fun registrar(f: FaseRegistrada) {
        inserir(if (f.perfilId != null && perfilExiste(f.perfilId)) f else f.copy(perfilId = null))
    }
}

/** O Histórico do aparelho. Uma instância só, no [com.exemplo.pomodoro.AppPomodoro]. */
class Historico(private val dao: HistoricoDao, private val escopo: CoroutineScope) {

    /** Da mais recente para a mais antiga. */
    val todas: Flow<List<FaseRegistrada>> = dao.todas()

    /** Também serve para o Desfazer: a Fase apagada volta com o mesmo id. */
    fun registrar(f: FaseRegistrada) {
        escopo.launch { dao.registrar(f) }
    }

    fun apagar(id: Long) {
        escopo.launch { dao.apagar(id) }
    }
}
