package ch.jacem.starclone.Database

import ch.jacem.starclone.Items.Filter
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.Items.TransferRecord
import ch.jacem.starclone.Items.Trigger

class DatabaseInfo {

    companion object {

        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 9
        const val DATABASE_NAME = "rcloneExplorer.db"


        val SQL_CREATE_TABLES_TASKS = "CREATE TABLE " + Task.TABLE_NAME + " (" +
                Task.COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                Task.COLUMN_NAME_TITLE + " TEXT," +
                Task.COLUMN_NAME_REMOTE_ID + " TEXT," +
                Task.COLUMN_NAME_REMOTE_TYPE + " INTEGER," +
                Task.COLUMN_NAME_REMOTE_PATH + " TEXT," +
                Task.COLUMN_NAME_LOCAL_PATH + " TEXT," +
                Task.COLUMN_NAME_SYNC_DIRECTION + " INTEGER)"

        val SQL_CREATE_TABLE_TRIGGER = "CREATE TABLE " + Trigger.TABLE_NAME + " (" +
                Trigger.COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                Trigger.COLUMN_NAME_TITLE + " TEXT," +
                Trigger.COLUMN_NAME_ENABLED + " INTEGER," +
                Trigger.COLUMN_NAME_TIME + " INTEGER," +
                Trigger.COLUMN_NAME_WEEKDAY + " INTEGER," +
                Trigger.COLUMN_NAME_TARGET + " INTEGER)"

        val SQL_CREATE_TABLE_FILTERS = "CREATE TABLE " + Filter.TABLE_NAME + " (" +
                Filter.COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
                Filter.COLUMN_NAME_TITLE + " TEXT," +
                Filter.COLUMN_NAME_FILTERS + " TEXT)"


        val SQL_UPDATE_TASK_ADD_MD5 = "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_MD5SUM} INTEGER"
        val SQL_UPDATE_TASK_ADD_WIFI = "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_WIFI_ONLY} INTEGER"
        val SQL_UPDATE_TRIGGER_ADD_TYPE = "ALTER TABLE ${Trigger.TABLE_NAME} ADD COLUMN ${Trigger.COLUMN_NAME_TYPE} INTEGER DEFAULT ${Trigger.TRIGGER_TYPE_SCHEDULE}"
        val SQL_UPDATE_TASK_ADD_FILTER_ID = "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_FILTER_ID} INTEGER REFERENCES ${Filter.TABLE_NAME}(${Filter.COLUMN_NAME_ID}) ON DELETE SET NULL"
        val SQL_UPDATE_TASK_ADD_DELETE_EXCLUDED = "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_DELETE_EXCLUDED} INTEGER"
        val SQL_UPDATE_TASK_ADD_FOLLOWUPS_FAIL = "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_ONFAIL_FOLLOWUP} INTEGER"
        val SQL_UPDATE_TASK_ADD_FOLLOWUPS_SUCCESS = "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_ONSUCCESS_FOLLOWUP} INTEGER"

        val SQL_CREATE_TABLE_TRANSFER_HISTORY = "CREATE TABLE ${TransferRecord.TABLE_NAME} (" +
                "${TransferRecord.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                "${TransferRecord.COLUMN_WORKER_ID} TEXT," +
                "${TransferRecord.COLUMN_TYPE} TEXT," +
                "${TransferRecord.COLUMN_TITLE} TEXT," +
                "${TransferRecord.COLUMN_REMOTE_NAME} TEXT," +
                "${TransferRecord.COLUMN_SOURCE} TEXT," +
                "${TransferRecord.COLUMN_DESTINATION} TEXT," +
                "${TransferRecord.COLUMN_STATUS} TEXT," +
                "${TransferRecord.COLUMN_BYTES_TRANSFERRED} INTEGER," +
                "${TransferRecord.COLUMN_ERROR_MESSAGE} TEXT," +
                "${TransferRecord.COLUMN_STARTED_AT} INTEGER," +
                "${TransferRecord.COLUMN_FINISHED_AT} INTEGER)"

        val SQL_UPDATE_TRANSFER_HISTORY_ADD_SOURCE =
            "ALTER TABLE ${TransferRecord.TABLE_NAME} ADD COLUMN ${TransferRecord.COLUMN_SOURCE} TEXT"
        val SQL_UPDATE_TRANSFER_HISTORY_ADD_DEST =
            "ALTER TABLE ${TransferRecord.TABLE_NAME} ADD COLUMN ${TransferRecord.COLUMN_DESTINATION} TEXT"

        val SQL_UPDATE_TASK_ADD_REMOTE_ID_2 =
            "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_REMOTE_ID_2} TEXT DEFAULT ''"
        val SQL_UPDATE_TASK_ADD_REMOTE_TYPE_2 =
            "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_REMOTE_TYPE_2} INTEGER DEFAULT 0"
        val SQL_UPDATE_TASK_ADD_REMOTE_PATH_2 =
            "ALTER TABLE ${Task.TABLE_NAME} ADD COLUMN ${Task.COLUMN_NAME_REMOTE_PATH_2} TEXT DEFAULT ''"

    }
}