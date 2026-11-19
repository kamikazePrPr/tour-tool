package com.kamikazeprpr.tourtool.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kamikazeprpr.tourtool.data.TourRepository
import com.kamikazeprpr.tourtool.data.db.Team
import com.kamikazeprpr.tourtool.data.db.TourDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TourRepository(TourDatabase.get(app))
    private val dao = TourDatabase.get(app).teamDao()

    val teams: StateFlow<List<Team>> = repo.teams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _newTeamId = MutableStateFlow<Long?>(null)
    val newTeamId: StateFlow<Long?> = _newTeamId

    fun createTeam(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _newTeamId.value = repo.createTeam(trimmed)
        }
    }

    fun memberCount(teamId: Long) = repo.memberCount(teamId)

    fun consumeNewTeam() {
        _newTeamId.value = null
    }

    fun deleteTeam(team: Team) {
        viewModelScope.launch { repo.deleteTeam(team) }
    }
}
