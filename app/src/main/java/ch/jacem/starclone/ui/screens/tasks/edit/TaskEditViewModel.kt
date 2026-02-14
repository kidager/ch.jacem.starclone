package ch.jacem.starclone.ui.screens.tasks.edit

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.jacem.starclone.Database.DatabaseHandler
import ch.jacem.starclone.Items.Filter
import ch.jacem.starclone.Items.SyncDirectionObject
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.R
import ch.jacem.starclone.Rclone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskEditViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val dbHandler = DatabaseHandler(context)

    var title by mutableStateOf("")
    var direction by mutableStateOf(SyncDirectionObject.SYNC_LOCAL_TO_REMOTE)
    var localPath by mutableStateOf("")
    var remoteId by mutableStateOf("")
    var remotePath by mutableStateOf("")
    var remoteId2 by mutableStateOf("")
    var remotePath2 by mutableStateOf("")
    var wifiOnly by mutableStateOf(false)
    var md5sum by mutableStateOf(false)
    var selectedFilterId by mutableStateOf<Long?>(null)
    var deleteExcluded by mutableStateOf(false)
    var onFailTaskId by mutableStateOf<Long?>(null)
    var onSuccessTaskId by mutableStateOf<Long?>(null)

    var remoteNames by mutableStateOf<List<String>>(emptyList())
        private set
    var filters by mutableStateOf<List<Filter>>(emptyList())
        private set
    var taskList by mutableStateOf<List<Task>>(emptyList())
        private set
    var directionOptions by mutableStateOf<List<DirectionOption>>(emptyList())
        private set

    var isEditing by mutableStateOf(false)
        private set
    var validationError by mutableStateOf<String?>(null)
        private set

    private var existingTaskId: Long = 0

    fun loadTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            loadDropdownData()

            if (id != 0L) {
                val task = dbHandler.getTask(id)
                if (task != null) {
                    withContext(Dispatchers.Main) {
                        existingTaskId = task.id
                        isEditing = true
                        title = task.title
                        direction = task.direction
                        localPath = task.localPath
                        remoteId = task.remoteId
                        remotePath = task.remotePath
                        wifiOnly = task.wifionly
                        md5sum = task.md5sum
                        selectedFilterId = task.filterId?.takeIf { it > 0 }
                        deleteExcluded = task.deleteExcluded
                        onFailTaskId = task.onFailFollowup?.takeIf { it > 0 }
                        onSuccessTaskId = task.onSuccessFollowup?.takeIf { it > 0 }
                        remoteId2 = task.remoteId2
                        remotePath2 = task.remotePath2
                    }
                }
            }
        }
    }

    private fun loadDropdownData() {
        val rclone = Rclone(context)
        val remotes = rclone.remotes?.map { it.name } ?: emptyList()
        val allFilters = dbHandler.allFilters
        val allTasks = dbHandler.allTasks

        val resources = context.resources
        val options = listOf(
            DirectionOption(SyncDirectionObject.SYNC_LOCAL_TO_REMOTE, resources.getString(R.string.sync_direction_local_remote)),
            DirectionOption(SyncDirectionObject.SYNC_REMOTE_TO_LOCAL, resources.getString(R.string.sync_direction_remote_local)),
            DirectionOption(SyncDirectionObject.COPY_LOCAL_TO_REMOTE, resources.getString(R.string.sync_direction_copy_local_remote)),
            DirectionOption(SyncDirectionObject.COPY_REMOTE_TO_LOCAL, resources.getString(R.string.sync_direction_copy_remote_local)),
            DirectionOption(SyncDirectionObject.SYNC_REMOTE_TO_REMOTE, resources.getString(R.string.sync_direction_sync_remote_to_remote)),
            DirectionOption(SyncDirectionObject.COPY_REMOTE_TO_REMOTE, resources.getString(R.string.sync_direction_copy_remote_to_remote)),
        )

        remoteNames = remotes
        filters = allFilters
        taskList = allTasks
        directionOptions = options

        if (remoteId.isEmpty() && remotes.isNotEmpty()) {
            remoteId = remotes[0]
        }
        if (remoteId2.isEmpty() && remotes.isNotEmpty()) {
            remoteId2 = remotes[0]
        }
    }

    fun updateLocalPath(path: String) {
        localPath = path
        validationError = null
    }

    fun updateRemotePath(path: String) {
        remotePath = path
        validationError = null
    }

    fun updateRemotePath2(path: String) {
        remotePath2 = path
        validationError = null
    }

    fun onFilterCreated(filterId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            filters = dbHandler.allFilters
            withContext(Dispatchers.Main) {
                selectedFilterId = filterId
            }
        }
    }

    fun deleteFilter(filterId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHandler.deleteFilter(filterId)
            filters = dbHandler.allFilters
            withContext(Dispatchers.Main) {
                if (selectedFilterId == filterId) {
                    selectedFilterId = null
                }
            }
        }
    }

    fun save(): Boolean {
        val isR2R = SyncDirectionObject.isRemoteToRemote(direction)

        if (!isR2R && localPath.isBlank()) {
            validationError = context.getString(R.string.task_data_validation_error_no_local_path)
            return false
        }
        if (remotePath.isBlank()) {
            validationError = context.getString(R.string.task_data_validation_error_no_remote_path)
            return false
        }
        if (isR2R && remotePath2.isBlank()) {
            validationError = context.getString(R.string.task_data_validation_error_no_remote_path_2)
            return false
        }

        val task = Task(if (isEditing) existingTaskId else 0)
        task.title = title
        task.direction = direction
        task.localPath = localPath
        task.remoteId = remoteId
        task.remotePath = remotePath
        task.wifionly = wifiOnly
        task.md5sum = md5sum
        task.filterId = selectedFilterId
        task.deleteExcluded = deleteExcluded
        task.onFailFollowup = onFailTaskId
        task.onSuccessFollowup = onSuccessTaskId
        task.remoteId2 = remoteId2
        task.remotePath2 = remotePath2

        val rclone = Rclone(context)
        rclone.remotes?.find { it.name == task.remoteId }?.let {
            task.remoteType = it.type
        }
        if (isR2R) {
            rclone.remotes?.find { it.name == task.remoteId2 }?.let {
                task.remoteType2 = it.type
            }
        }

        if (isEditing) {
            dbHandler.updateTask(task)
        } else {
            dbHandler.createTask(task)
        }
        return true
    }
}

data class DirectionOption(val value: Int, val label: String) {
    override fun toString(): String = label
}
