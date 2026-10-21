package ch.jacem.starclone.ui.screens.tasks.edit

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.jacem.starclone.Database.DatabaseHandler
import ch.jacem.starclone.Items.Task
import ch.jacem.starclone.Items.Trigger
import ch.jacem.starclone.R
import ch.jacem.starclone.Services.TriggerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TriggerEditViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application
    private val dbHandler = DatabaseHandler(context)

    var title by mutableStateOf("")
    var isEnabled by mutableStateOf(true)
    var type by mutableIntStateOf(Trigger.TRIGGER_TYPE_SCHEDULE)
    var weekdays by mutableIntStateOf(0b01111111) // all days enabled
    var timeHour by mutableIntStateOf(0)
    var timeMinute by mutableIntStateOf(0)
    var intervalMinutes by mutableIntStateOf(60)
    var targetTaskId by mutableLongStateOf(0L)

    var taskList by mutableStateOf<List<Task>>(emptyList())
        private set
    var isEditing by mutableStateOf(false)
        private set
    var validationError by mutableStateOf<String?>(null)
        private set

    private var existingTriggerId: Long = Trigger.TRIGGER_ID_DOESNTEXIST

    fun loadTrigger(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val allTasks = dbHandler.allTasks
            taskList = allTasks

            if (id != 0L) {
                val trigger = dbHandler.getTrigger(id)
                if (trigger != null) {
                    withContext(Dispatchers.Main) {
                        existingTriggerId = trigger.id
                        isEditing = true
                        title = trigger.title
                        isEnabled = trigger.isEnabled
                        type = trigger.type
                        weekdays = trigger.getWeekdays()

                        if (trigger.type == Trigger.TRIGGER_TYPE_SCHEDULE) {
                            timeHour = trigger.time / 60
                            timeMinute = trigger.time % 60
                        } else {
                            intervalMinutes = trigger.time
                        }
                        targetTaskId = trigger.triggerTarget
                    }
                }
            } else if (allTasks.isNotEmpty()) {
                targetTaskId = allTasks[0].id
            }
        }
    }

    fun updateWeekday(dayIndex: Int, enabled: Boolean) {
        weekdays = if (enabled) {
            weekdays or (1 shl dayIndex)
        } else {
            weekdays and (1 shl dayIndex).inv()
        }
    }

    fun isWeekdayEnabled(dayIndex: Int): Boolean {
        return weekdays shr dayIndex and 1 == 1
    }

    fun save(): Boolean {
        if (title.isBlank()) {
            validationError = context.getString(R.string.trigger_save_notitle)
            return false
        }
        if (taskList.isEmpty()) {
            validationError = context.getString(R.string.trigger_save_notasks)
            return false
        }

        val trigger = Trigger(existingTriggerId)
        trigger.title = title
        trigger.isEnabled = isEnabled
        trigger.type = type
        trigger.setWeekdays(weekdays.toByte())
        trigger.time = if (type == Trigger.TRIGGER_TYPE_SCHEDULE) {
            timeHour * 60 + timeMinute
        } else {
            intervalMinutes
        }
        trigger.triggerTarget = targetTaskId

        if (existingTriggerId == Trigger.TRIGGER_ID_DOESNTEXIST) {
            val created = dbHandler.createTrigger(trigger)
            TriggerService(context).queueSingleTrigger(created)
        } else {
            dbHandler.updateTrigger(trigger)
            TriggerService(context).queueSingleTrigger(trigger)
        }
        return true
    }
}
