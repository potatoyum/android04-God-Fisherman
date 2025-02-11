package com.android04.godfisherman.presentation.main

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android04.godfisherman.common.Event
import com.android04.godfisherman.common.Result
import com.android04.godfisherman.common.di.IoDispatcher
import com.android04.godfisherman.data.localdatabase.entity.TmpFishingRecord
import com.android04.godfisherman.domain.MainViewRepository
import com.android04.godfisherman.utils.StopwatchManager
import com.android04.godfisherman.utils.toTimeMilliSecond
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainViewRepository: MainViewRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val stopwatch = StopwatchManager({ time -> _displayTime.postValue(time.toTimeMilliSecond()) })
    val stopwatchOnFlag: MutableLiveData<Boolean> = MutableLiveData(false)
    var beforeMenuItemId: Int = 0
    var isOpened: Boolean = false
    var isServiceRequestWithOutCamera = true

    var lastBackTime = 0L

    private val _currentLocation: MutableLiveData<Location?> by lazy {
        MutableLiveData<Location?>(null)
    }
    val currentLocation: LiveData<Location?> = _currentLocation

    private val _isStopwatchStarted: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }
    val isStopwatchStarted: LiveData<Boolean> = _isStopwatchStarted

    private val _isAfterUpload: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    val isAfterUpload: LiveData<Boolean> = _isAfterUpload

    private val _tmpFishingList: MutableLiveData<List<TmpFishingRecord>> by lazy { MutableLiveData<List<TmpFishingRecord>>() }
    val tmpFishingList: LiveData<List<TmpFishingRecord>> = _tmpFishingList

    private val _displayTime: MutableLiveData<String> by lazy { MutableLiveData<String>("00:00:00.00") }
    val displayTime: LiveData<String> = _displayTime

    private val _isLoading: MutableLiveData<Boolean?> by lazy { MutableLiveData<Boolean?>(null) }
    val isLoading: MutableLiveData<Boolean?> = _isLoading

    private val _successOrFail: MutableLiveData<Event<String>> by lazy { MutableLiveData<Event<String>>() }
    val successOrFail: LiveData<Event<String>> = _successOrFail

    fun startOrStopTimer(): Boolean {
        return if (isStopwatchStarted.value == true) {
            endStopwatch()
            true
        } else {
            startStopwatch()
            false
        }
    }

    fun passedTimeFromService(passedTime: Double) {
        stopwatch.start(10, passedTime)
        _isStopwatchStarted.value = true
    }

    private fun startStopwatch() {
        isTimeLine = true
        stopwatch.start(10)
        _isStopwatchStarted.value = true
    }

    fun endStopwatch() {
        isTimeLine = false
        stopwatch.end()
        _isStopwatchStarted.value = false
    }

    fun resumeStopwatch() {
        isTimeLine = true
        stopwatch.resumeStopwatch(10)
        _isStopwatchStarted.value = true
    }

    fun saveTimeLineRecord() {
        if (!_tmpFishingList.value.isNullOrEmpty()) {
            _isLoading.value = true

            viewModelScope.launch(ioDispatcher) {
                val result = mainViewRepository.saveTimeLineRecord(stopwatch.getSaveTime())
                _isLoading.postValue(false)

                when (result) {
                    is Result.Success -> {
                        _successOrFail.postValue(Event("업로드를 완료했습니다."))
                    }
                    is Result.Fail -> {
                        _successOrFail.postValue(Event(result.description))
                    }
                }
            }
        }
        _isAfterUpload.value = true
    }

    fun loadTmpTimeLineRecord() {
        viewModelScope.launch(ioDispatcher) {
            _tmpFishingList.postValue(mainViewRepository.loadTmpTimeLineRecord())
        }
    }

    fun passStopwatchToService() {
        _isStopwatchStarted.value = false
        stopwatch.end()
    }

    fun setIsAfterUploadFalse() {
        _isAfterUpload.value = false
    }

    fun updateLocation(location: Location?) {
        viewModelScope.launch(ioDispatcher) {
            mainViewRepository.saveLocation(location)
            _currentLocation.postValue(location)
        }
    }

    companion object {
        var isTimeLine = false
        var isFromService = false
    }
}
