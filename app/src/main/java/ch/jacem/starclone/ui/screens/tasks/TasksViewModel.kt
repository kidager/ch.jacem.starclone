package ch.jacem.starclone.ui.screens.tasks

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.jacem.starclone.Database.DatabaseHandler
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.Items.Trigger
import ch.jacem.starclone.Services.TriggerService
import ch.jacem.starclone.workmanager.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    var tasks by mutableStateOf<List<Task>>(emptyList(), referentialEqualityPolicy())
        private set

    var triggers by mutableStateOf<List<Trigger>>(emptyList(), referentialEqualityPolicy())
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

            tasks = loadedTasks
            triggers = loadedTriggers
            isLoading = false
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
            tasks = dbHandler.allTasks
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbHandler = DatabaseHandler(context)
            dbHandler.deleteTask(task.id)
            tasks = dbHandler.allTasks
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
            val dbTrigger = Trigger(trigger.id).apply {
                title = trigger.title
                isEnabled = newEnabled
                setWeekdays(trigger.getWeekdays().toByte())
                time = trigger.time
                triggerTarget = trigger.triggerTarget
                type = trigger.type
            }
            dbHandler.updateTrigger(dbTrigger)

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
            triggers = dbHandler.allTrigger
        }
    }

    fun deleteTrigger(trigger: Trigger) {
        viewModelScope.launch(Dispatchers.IO) {
            val dbHandler = DatabaseHandler(context)
            val triggerService = TriggerService(context)
            triggerService.cancelTrigger(trigger.id)
            dbHandler.deleteTrigger(trigger.id)
            triggers = dbHandler.allTrigger
        }
    }
}
