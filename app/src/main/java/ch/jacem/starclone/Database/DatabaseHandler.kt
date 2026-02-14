package ch.jacem.starclone.Database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import ch.jacem.starclone.Database.DatabaseInfo.Companion.DATABASE_NAME
import ch.jacem.starclone.Database.DatabaseInfo.Companion.DATABASE_VERSION
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_CREATE_TABLES_TASKS
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_CREATE_TABLE_FILTERS
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_CREATE_TABLE_TRIGGER
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_UPDATE_TASK_ADD_DELETE_EXCLUDED
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_UPDATE_TASK_ADD_MD5
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_UPDATE_TASK_ADD_WIFI
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_UPDATE_TASK_ADD_FILTER_ID
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_UPDATE_TASK_ADD_FOLLOWUPS_FAIL
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_UPDATE_TASK_ADD_FOLLOWUPS_SUCCESS
import ch.jacem.starclone.Database.DatabaseInfo.Companion.SQL_UPDATE_TRIGGER_ADD_TYPE
import ch.jacem.starclone.Items.Filter
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.Items.TransferRecord
import ch.jacem.starclone.Items.Trigger
import java.util.ArrayList

class DatabaseHandler(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_TABLES_TASKS)
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_TRIGGER)
        sqLiteDatabase.execSQL(SQL_CREATE_TABLE_FILTERS)
        sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_MD5)
        sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_WIFI)
        sqLiteDatabase.execSQL(SQL_UPDATE_TRIGGER_ADD_TYPE)
        sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_FILTER_ID)
        sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_DELETE_EXCLUDED)
        sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_FOLLOWUPS_FAIL)
        sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_FOLLOWUPS_SUCCESS)
        sqLiteDatabase.execSQL(DatabaseInfo.SQL_CREATE_TABLE_TRANSFER_HISTORY)
        sqLiteDatabase.execSQL(DatabaseInfo.SQL_UPDATE_TASK_ADD_REMOTE_ID_2)
        sqLiteDatabase.execSQL(DatabaseInfo.SQL_UPDATE_TASK_ADD_REMOTE_TYPE_2)
        sqLiteDatabase.execSQL(DatabaseInfo.SQL_UPDATE_TASK_ADD_REMOTE_PATH_2)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            sqLiteDatabase.execSQL(SQL_CREATE_TABLE_TRIGGER)
        }
        if (oldVersion < 3) {
            sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_MD5)
            sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_WIFI)
        }
        if (oldVersion < 4) {
            sqLiteDatabase.execSQL(SQL_UPDATE_TRIGGER_ADD_TYPE)
        }
        if (oldVersion < 5) {
            sqLiteDatabase.execSQL(SQL_CREATE_TABLE_FILTERS)
            sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_FILTER_ID)
            sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_DELETE_EXCLUDED)
        }
        if (oldVersion < 6) {
            sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_FOLLOWUPS_FAIL)
            sqLiteDatabase.execSQL(SQL_UPDATE_TASK_ADD_FOLLOWUPS_SUCCESS)
        }
        if (oldVersion < 7) {
            sqLiteDatabase.execSQL(DatabaseInfo.SQL_CREATE_TABLE_TRANSFER_HISTORY)
        }
        if (oldVersion < 8) {
            try {
                sqLiteDatabase.execSQL(DatabaseInfo.SQL_UPDATE_TRANSFER_HISTORY_ADD_SOURCE)
                sqLiteDatabase.execSQL(DatabaseInfo.SQL_UPDATE_TRANSFER_HISTORY_ADD_DEST)
            } catch (_: Exception) {
                // Columns may already exist if table was created fresh in v7+
            }
        }
        if (oldVersion < 9) {
            sqLiteDatabase.execSQL(DatabaseInfo.SQL_UPDATE_TASK_ADD_REMOTE_ID_2)
            sqLiteDatabase.execSQL(DatabaseInfo.SQL_UPDATE_TASK_ADD_REMOTE_TYPE_2)
            sqLiteDatabase.execSQL(DatabaseInfo.SQL_UPDATE_TASK_ADD_REMOTE_PATH_2)
        }
    }

    val allTasks: List<Task>
        get() {
            val db = readableDatabase
            val selection = ""
            val selectionArgs = arrayOf<String>()
            val sortOrder = Task.COLUMN_NAME_ID + " ASC"
            val cursor = db.query(
                Task.TABLE_NAME,
                taskProjection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
            )
            val results: MutableList<Task> = ArrayList()
            while (cursor.moveToNext()) {
                results.add(taskFromCursor(cursor))
            }
            cursor.close()
            db.close()
            return results
        }

    fun getTask(id: Long): Task? {
        val db = readableDatabase
        val selection = Task.COLUMN_NAME_ID + " LIKE ?"
        val selectionArgs = arrayOf(id.toString())
        val sortOrder = Task.COLUMN_NAME_ID + " ASC"
        val cursor = db.query(
            Task.TABLE_NAME,
            taskProjection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        )
        val results: MutableList<Task> = ArrayList()
        while (cursor.moveToNext()) {
            results.add(taskFromCursor(cursor))
        }
        cursor.close()
        db.close()
        return if (results.size == 0) {
            null
        } else results[0]
    }

    fun createTask(taskToStore: Task, withId: Boolean = false): Task {
        val db = writableDatabase
        val newRowId = db.insert(Task.TABLE_NAME, null, if(withId) getTaskContentValuesWithID(taskToStore) else getTaskContentValues(taskToStore))
        db.close()
        taskToStore.id = newRowId
        return taskToStore
    }

    fun upsertTask(task: Task): Task {
        val db = writableDatabase
        val rowId = db.insertWithOnConflict(
            Task.TABLE_NAME, null,
            getTaskContentValuesWithID(task),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        db.close()
        task.id = rowId
        return task
    }

    fun updateTask(taskToUpdate: Task) {
        val db = writableDatabase
        db.update(
            Task.TABLE_NAME,
            getTaskContentValues(taskToUpdate),
            Task.COLUMN_NAME_ID + " = ?",
            arrayOf(taskToUpdate.id.toString())
        )
        db.close()
    }

    private val taskProjection: Array<String>
        get() = arrayOf(
            Task.COLUMN_NAME_ID,
            Task.COLUMN_NAME_TITLE,
            Task.COLUMN_NAME_REMOTE_ID,
            Task.COLUMN_NAME_REMOTE_TYPE,
            Task.COLUMN_NAME_REMOTE_PATH,
            Task.COLUMN_NAME_LOCAL_PATH,
            Task.COLUMN_NAME_SYNC_DIRECTION,
            Task.COLUMN_NAME_MD5SUM,
            Task.COLUMN_NAME_WIFI_ONLY,
            Task.COLUMN_NAME_FILTER_ID,
            Task.COLUMN_NAME_DELETE_EXCLUDED,
            Task.COLUMN_NAME_ONFAIL_FOLLOWUP,
            Task.COLUMN_NAME_ONSUCCESS_FOLLOWUP,
            Task.COLUMN_NAME_REMOTE_ID_2,
            Task.COLUMN_NAME_REMOTE_TYPE_2,
            Task.COLUMN_NAME_REMOTE_PATH_2
        )

    private fun taskFromCursor(cursor: Cursor): Task {
        val task = Task(cursor.getLong(0))
        task.title = cursor.getString(1)
        task.remoteId = cursor.getString(2)
        task.remoteType = cursor.getInt(3)
        task.remotePath = cursor.getString(4)
        task.localPath = cursor.getString(5)
        task.direction = cursor.getInt(6)
        task.md5sum = getBoolean(cursor, 7)
        task.wifionly = getBoolean(cursor, 8)
        task.filterId = cursor.getLong(9)
        task.deleteExcluded = getBoolean(cursor, 10)
        task.onFailFollowup = cursor.getLong(11)
        task.onSuccessFollowup = cursor.getLong(12)
        task.remoteId2 = cursor.getString(13) ?: ""
        task.remoteType2 = cursor.getInt(14)
        task.remotePath2 = cursor.getString(15) ?: ""
        return task
    }

    private fun getTaskContentValuesWithID(task: Task): ContentValues {
        val values = getTaskContentValues(task)
        values.put(Task.COLUMN_NAME_ID, task.id)
        return values
    }

    fun deleteTask(id: Long): Int {
        val db = writableDatabase
        val selection = Task.COLUMN_NAME_ID + " LIKE ?"
        val selectionArgs = arrayOf(id.toString())
        val retcode = db.delete(Task.TABLE_NAME, selection, selectionArgs)
        db.close()
        return retcode
    }

    private fun getTaskContentValues(task: Task): ContentValues {
        val values = ContentValues()
        values.put(Task.COLUMN_NAME_TITLE, task.title)
        values.put(Task.COLUMN_NAME_LOCAL_PATH, task.localPath)
        values.put(Task.COLUMN_NAME_REMOTE_ID, task.remoteId)
        values.put(Task.COLUMN_NAME_REMOTE_PATH, task.remotePath)
        values.put(Task.COLUMN_NAME_REMOTE_TYPE, task.remoteType)
        values.put(Task.COLUMN_NAME_SYNC_DIRECTION, task.direction)
        values.put(Task.COLUMN_NAME_MD5SUM, task.md5sum)
        values.put(Task.COLUMN_NAME_WIFI_ONLY, task.wifionly)
        values.put(Task.COLUMN_NAME_FILTER_ID, task.filterId)
        values.put(Task.COLUMN_NAME_DELETE_EXCLUDED, task.deleteExcluded)
        values.put(Task.COLUMN_NAME_ONFAIL_FOLLOWUP, task.onFailFollowup)
        values.put(Task.COLUMN_NAME_ONSUCCESS_FOLLOWUP, task.onSuccessFollowup)
        values.put(Task.COLUMN_NAME_REMOTE_ID_2, task.remoteId2)
        values.put(Task.COLUMN_NAME_REMOTE_TYPE_2, task.remoteType2)
        values.put(Task.COLUMN_NAME_REMOTE_PATH_2, task.remotePath2)
        return values
    }

    val allTrigger: List<Trigger>
        get() {
            val db = readableDatabase
            val projection = triggerProjection
            val selection = ""
            val selectionArgs = arrayOf<String>()
            val sortOrder = Trigger.COLUMN_NAME_ID + " ASC"
            val cursor = db.query(
                    Trigger.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            )
            val results: MutableList<Trigger> = ArrayList()
            while (cursor.moveToNext()) {
                results.add(triggerFromCursor(cursor))
            }
            cursor.close()
            db.close()
            return results
        }

    fun getTrigger(id: Long): Trigger? {
        val db = readableDatabase
        val projection = triggerProjection
        val selection = Trigger.COLUMN_NAME_ID + " LIKE ?"
        val selectionArgs = arrayOf(id.toString())
        val sortOrder = Trigger.COLUMN_NAME_ID + " ASC"
        val cursor = db.query(
                Trigger.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        )
        val results: MutableList<Trigger> = ArrayList()
        while (cursor.moveToNext()) {
            results.add(triggerFromCursor(cursor))
        }
        cursor.close()
        db.close()
        return if (results.size == 0) {
            null
        } else results[0]
    }

    fun createTrigger(triggerToStore: Trigger, withId: Boolean = false): Trigger {
        val db = writableDatabase
        val newRowId = db.insert(Trigger.TABLE_NAME, null, if(withId) getTriggerContentValuesWithID(triggerToStore) else getTriggerContentValues(triggerToStore))
        db.close()
        triggerToStore.id = newRowId
        return triggerToStore
    }

    fun upsertTrigger(trigger: Trigger): Trigger {
        val db = writableDatabase
        val rowId = db.insertWithOnConflict(
            Trigger.TABLE_NAME, null,
            getTriggerContentValuesWithID(trigger),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        db.close()
        trigger.id = rowId
        return trigger
    }

    fun updateTrigger(triggerToUpdate: Trigger) {
        val db = writableDatabase
        db.update(
                Trigger.TABLE_NAME,
                getTriggerContentValuesWithID(triggerToUpdate),
                Trigger.COLUMN_NAME_ID + " = ?",
                arrayOf(triggerToUpdate.id.toString())
        )
        db.close()
    }

    fun deleteTrigger(id: Long): Int {
        val db = writableDatabase
        val selection = Trigger.COLUMN_NAME_ID + " LIKE ?"
        val selectionArgs = arrayOf(id.toString())
        val retcode = db.delete(Trigger.TABLE_NAME, selection, selectionArgs)
        db.close()
        return retcode
    }

    private fun getTriggerContentValuesWithID(t: Trigger): ContentValues {
        val values = getTriggerContentValues(t)
        values.put(Trigger.COLUMN_NAME_ID, t.id)
        return values
    }

    private fun getTriggerContentValues(t: Trigger): ContentValues {
        val values = ContentValues()
        if(t.id != Trigger.TRIGGER_ID_DOESNTEXIST) {
            values.put(Trigger.COLUMN_NAME_ID, t.id)
        }
        values.put(Trigger.COLUMN_NAME_TITLE, t.title)
        values.put(Trigger.COLUMN_NAME_ENABLED, t.isEnabled)
        values.put(Trigger.COLUMN_NAME_TIME, t.time)
        values.put(Trigger.COLUMN_NAME_WEEKDAY, t.getWeekdays())
        values.put(Trigger.COLUMN_NAME_TARGET, t.triggerTarget)
        values.put(Trigger.COLUMN_NAME_TYPE, t.type)
        return values
    }

    private val triggerProjection: Array<String>
        private get() = arrayOf(
                Trigger.COLUMN_NAME_ID,
                Trigger.COLUMN_NAME_TITLE,
                Trigger.COLUMN_NAME_ENABLED,
                Trigger.COLUMN_NAME_TIME,
                Trigger.COLUMN_NAME_WEEKDAY,
                Trigger.COLUMN_NAME_TARGET,
                Trigger.COLUMN_NAME_TYPE
        )

    private fun triggerFromCursor(cursor: Cursor): Trigger {
        val trigger = Trigger(cursor.getLong(0))
        trigger.title = cursor.getString(1)
        trigger.isEnabled = cursor.getInt(2) == 1
        trigger.time = cursor.getInt(3)
        val weekdays = cursor.getInt(4)
        trigger.setWeekdays(weekdays.toByte())
        trigger.triggerTarget = cursor.getLong(5)
        trigger.type = cursor.getInt(6)
        return trigger
    }

    val allFilters: List<Filter>
        get() {
            val db = readableDatabase
            val projection = filterProjection
            val selection = ""
            val selectionArgs = arrayOf<String>()
            val sortOrder = Filter.COLUMN_NAME_ID + " ASC"
            val cursor = db.query(
                    Filter.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            )
            val results: MutableList<Filter> = ArrayList()
            while (cursor.moveToNext()) {
                results.add(filterFromCursor(cursor))
            }
            cursor.close()
            db.close()
            return results
        }

    fun getFilter(id: Long): Filter? {
        val db = readableDatabase
        val projection = filterProjection
        val selection = Filter.COLUMN_NAME_ID + " LIKE ?"
        val selectionArgs = arrayOf(id.toString())
        val sortOrder = Filter.COLUMN_NAME_ID + " ASC"
        val cursor = db.query(
                Filter.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        )
        val results: MutableList<Filter> = ArrayList()
        while (cursor.moveToNext()) {
            results.add(filterFromCursor(cursor))
        }
        cursor.close()
        db.close()
        return if (results.size == 0) {
            null
        } else results[0]
    }

    fun createFilter(filterToStore: Filter, withId: Boolean = false): Filter {
        val db = writableDatabase
        val newRowId = db.insert(Filter.TABLE_NAME, null, if(withId) getFilterContentValuesWithID(filterToStore) else getFilterContentValues(filterToStore))
        db.close()
        filterToStore.id = newRowId
        return filterToStore
    }

    fun upsertFilter(filter: Filter): Filter {
        val db = writableDatabase
        val rowId = db.insertWithOnConflict(
            Filter.TABLE_NAME, null,
            getFilterContentValuesWithID(filter),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        db.close()
        filter.id = rowId
        return filter
    }

    fun updateFilter(filterToUpdate: Filter) {
        val db = writableDatabase
        db.update(
                Filter.TABLE_NAME,
                getFilterContentValuesWithID(filterToUpdate),
                Filter.COLUMN_NAME_ID + " = ?",
                arrayOf(filterToUpdate.id.toString())
        )
        db.close()
    }

    fun deleteFilter(id: Long): Int {
        val db = writableDatabase
        val selection = Filter.COLUMN_NAME_ID + " LIKE ?"
        val selectionArgs = arrayOf(id.toString())
        val retcode = db.delete(Filter.TABLE_NAME, selection, selectionArgs)
        db.close()
        return retcode
    }

    private fun getFilterContentValuesWithID(t: Filter): ContentValues {
        val values = getFilterContentValues(t)
        values.put(Filter.COLUMN_NAME_ID, t.id)
        return values
    }

    private fun getFilterContentValues(t: Filter): ContentValues {
        val values = ContentValues()
        values.put(Filter.COLUMN_NAME_TITLE, t.title)
        values.put(Filter.COLUMN_NAME_FILTERS, t.getFiltersRaw())
        return values
    }

    private val filterProjection: Array<String>
        private get() = arrayOf(
                Filter.COLUMN_NAME_ID,
                Filter.COLUMN_NAME_TITLE,
                Filter.COLUMN_NAME_FILTERS,
        )

    private fun filterFromCursor(cursor: Cursor): Filter {
        val filter = Filter(cursor.getLong(0))
        filter.title = cursor.getString(1)
        filter.setFiltersRaw(cursor.getString(2))
        return filter
    }

    // Transfer History

    fun insertTransferRecord(record: TransferRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(TransferRecord.COLUMN_WORKER_ID, record.workerId)
            put(TransferRecord.COLUMN_TYPE, record.type)
            put(TransferRecord.COLUMN_TITLE, record.title)
            put(TransferRecord.COLUMN_REMOTE_NAME, record.remoteName)
            put(TransferRecord.COLUMN_SOURCE, record.source)
            put(TransferRecord.COLUMN_DESTINATION, record.destination)
            put(TransferRecord.COLUMN_STATUS, record.status)
            put(TransferRecord.COLUMN_BYTES_TRANSFERRED, record.bytesTransferred)
            put(TransferRecord.COLUMN_ERROR_MESSAGE, record.errorMessage)
            put(TransferRecord.COLUMN_STARTED_AT, record.startedAt)
            put(TransferRecord.COLUMN_FINISHED_AT, record.finishedAt)
        }
        val id = db.insert(TransferRecord.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getTransferHistory(limit: Int = 100): List<TransferRecord> {
        val db = readableDatabase
        val cursor = db.query(
            TransferRecord.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "${TransferRecord.COLUMN_FINISHED_AT} DESC",
            limit.toString(),
        )
        val results = mutableListOf<TransferRecord>()
        while (cursor.moveToNext()) {
            results.add(transferRecordFromCursor(cursor))
        }
        cursor.close()
        db.close()
        return results
    }

    fun clearTransferHistory() {
        val db = writableDatabase
        db.delete(TransferRecord.TABLE_NAME, null, null)
        db.close()
    }

    fun deleteTransferRecords(ids: List<Long>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        val placeholders = ids.joinToString(",") { "?" }
        db.delete(
            TransferRecord.TABLE_NAME,
            "${TransferRecord.COLUMN_ID} IN ($placeholders)",
            ids.map { it.toString() }.toTypedArray(),
        )
        db.close()
    }

    private fun transferRecordFromCursor(cursor: Cursor): TransferRecord {
        return TransferRecord(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_ID)),
            workerId = cursor.getString(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_WORKER_ID)) ?: "",
            type = cursor.getString(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_TYPE)) ?: "",
            title = cursor.getString(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_TITLE)) ?: "",
            remoteName = cursor.getString(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_REMOTE_NAME)) ?: "",
            source = cursor.getString(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_SOURCE)) ?: "",
            destination = cursor.getString(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_DESTINATION)) ?: "",
            status = cursor.getString(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_STATUS)) ?: "",
            bytesTransferred = cursor.getLong(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_BYTES_TRANSFERRED)),
            errorMessage = cursor.getString(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_ERROR_MESSAGE)),
            startedAt = cursor.getLong(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_STARTED_AT)),
            finishedAt = cursor.getLong(cursor.getColumnIndexOrThrow(TransferRecord.COLUMN_FINISHED_AT)),
        )
    }

    fun deleteEveryting() {
        for (trigger in allTrigger) {
            deleteTrigger(trigger.id)
        }
        for (task in allTasks) {
            deleteTask(task.id)
        }
        for (filter in allFilters) {
            deleteFilter(filter.id)
        }
    }

    private fun getBoolean(cursor: Cursor, cursorid: Int): Boolean {
        return when(cursor.getInt(cursorid)){
            0 -> false
            1 -> true
            else -> true
        }
    }
}