package ca.pkay.rcloneexplorer.ui.screens.tasks

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.pkay.rcloneexplorer.Database.DatabaseHandler
import ca.pkay.rcloneexplorer.Items.Task
import ca.pkay.rcloneexplorer.Items.Trigger
import ca.pkay.rcloneexplorer.Services.TriggerService
import ca.pkay.rcloneexplorer.workmanager.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application

    var tasks by mutableStateOf<List<Task>>(emptyList())
        private set
    var triggers by mutableStateOf<List<Trigger>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbHandler = DatabaseHandler(context)
            val loadedTasks = dbHandler.allTasks
            val loadedTriggers = dbHandler.allTrigger

            withContext(Dispatchers.Main) {
                tasks = loadedTasks
                triggers = loadedTriggers
                isLoading = false
            }
        }
    }

    fun startTask(task: Task) {
        SyncManager(context).queue(task)
    }

    fun copyTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbHandler = DatabaseHandler(context)
            val copy = Task(-1).apply {
                title = task.title + "_copy"
                remoteId = task.remoteId
                remoteType = task.remoteType
                remotePath = task.remotePath
                localPath = task.localPath
                direction = task.direction
                md5sum = task.md5sum
                wifionly = task.wifionly
                filterId = task.filterId
                deleteExcluded = task.deleteExcluded
                onFailFollowup = task.onFailFollowup
                onSuccessFollowup = task.onSuccessFollowup
            }
            dbHandler.createTask(copy)
            val updatedTasks = dbHandler.allTasks
            withContext(Dispatchers.Main) {
                tasks = updatedTasks
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbHandler = DatabaseHandler(context)
            dbHandler.deleteTask(task.id)
            val updatedTasks = dbHandler.allTasks
            withContext(Dispatchers.Main) {
                tasks = updatedTasks
            }
        }
    }

    fun toggleTrigger(trigger: Trigger) {
        val newEnabled = !trigger.isEnabled
        // Update UI immediately
        triggers = triggers.map {
            if (it.id == trigger.id) {
                Trigger(it.id).apply {
                    title = it.title
                    isEnabled = newEnabled
                    setWeekdays(it.getWeekdays().toByte())
                    time = it.time
                    triggerTarget = it.triggerTarget
                    type = it.type
                }
            } else it
        }
        // Persist in background
        viewModelScope.launch(Dispatchers.IO) {
            val dbHandler = DatabaseHandler(context)
            trigger.isEnabled = newEnabled
            dbHandler.updateTrigger(trigger)

            val triggerService = TriggerService(context)
            if (newEnabled) {
                triggerService.queueTrigger()
            } else {
                triggerService.cancelTrigger(trigger.id)
            }
        }
    }

    fun copyTrigger(trigger: Trigger) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbHandler = DatabaseHandler(context)
            val copy = Trigger(-1).apply {
                title = trigger.title + "_copy"
                isEnabled = trigger.isEnabled
                setWeekdays(trigger.getWeekdays().toByte())
                time = trigger.time
                triggerTarget = trigger.triggerTarget
                type = trigger.type
            }
            dbHandler.createTrigger(copy)
            val updatedTriggers = dbHandler.allTrigger
            withContext(Dispatchers.Main) {
                triggers = updatedTriggers
            }
        }
    }

    fun deleteTrigger(trigger: Trigger) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbHandler = DatabaseHandler(context)
            val triggerService = TriggerService(context)
            triggerService.cancelTrigger(trigger.id)
            dbHandler.deleteTrigger(trigger.id)
            val updatedTriggers = dbHandler.allTrigger
            withContext(Dispatchers.Main) {
                triggers = updatedTriggers
            }
        }
    }
}
