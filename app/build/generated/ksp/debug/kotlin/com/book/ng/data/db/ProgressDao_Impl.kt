package com.book.ng.`data`.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ProgressDao_Impl(
  __db: RoomDatabase,
) : ProgressDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfProgressEntity: EntityUpsertAdapter<ProgressEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfProgressEntity = EntityUpsertAdapter<ProgressEntity>(object : EntityInsertAdapter<ProgressEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `progress` (`bookId`,`locator`,`mode`,`updatedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProgressEntity) {
        statement.bindLong(1, entity.bookId)
        statement.bindText(2, entity.locator)
        statement.bindText(3, entity.mode)
        statement.bindLong(4, entity.updatedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<ProgressEntity>() {
      protected override fun createQuery(): String = "UPDATE `progress` SET `bookId` = ?,`locator` = ?,`mode` = ?,`updatedAt` = ? WHERE `bookId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ProgressEntity) {
        statement.bindLong(1, entity.bookId)
        statement.bindText(2, entity.locator)
        statement.bindText(3, entity.mode)
        statement.bindLong(4, entity.updatedAt)
        statement.bindLong(5, entity.bookId)
      }
    })
  }

  public override suspend fun upsert(progress: ProgressEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfProgressEntity.upsert(_connection, progress)
  }

  public override fun getByBookId(bookId: Long): Flow<ProgressEntity?> {
    val _sql: String = "SELECT * FROM progress WHERE bookId = ?"
    return createFlow(__db, false, arrayOf("progress")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, bookId)
        val _columnIndexOfBookId: Int = getColumnIndexOrThrow(_stmt, "bookId")
        val _columnIndexOfLocator: Int = getColumnIndexOrThrow(_stmt, "locator")
        val _columnIndexOfMode: Int = getColumnIndexOrThrow(_stmt, "mode")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: ProgressEntity?
        if (_stmt.step()) {
          val _tmpBookId: Long
          _tmpBookId = _stmt.getLong(_columnIndexOfBookId)
          val _tmpLocator: String
          _tmpLocator = _stmt.getText(_columnIndexOfLocator)
          val _tmpMode: String
          _tmpMode = _stmt.getText(_columnIndexOfMode)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = ProgressEntity(_tmpBookId,_tmpLocator,_tmpMode,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM progress"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByBookId(bookId: Long) {
    val _sql: String = "DELETE FROM progress WHERE bookId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, bookId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
