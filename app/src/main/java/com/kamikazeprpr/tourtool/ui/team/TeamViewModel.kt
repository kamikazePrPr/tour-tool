package com.kamikazeprpr.tourtool.ui.team

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kamikazeprpr.tourtool.data.TourRepository
import com.kamikazeprpr.tourtool.data.db.Member
import com.kamikazeprpr.tourtool.data.db.Team
import com.kamikazeprpr.tourtool.data.db.TourDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TeamViewModel(app: Application, teamId: Long) : AndroidViewModel(app) {

    private val repo = TourRepository(TourDatabase.get(app))

    val team: StateFlow<Team?> = repo.team(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val members: StateFlow<List<Member>> = repo.members(teamId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(teamId: Long) = viewModelFactory {
            initializer {
                TeamViewModel(this[APPLICATION_KEY] as Application, teamId)
            }
        }
    }
}
