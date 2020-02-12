package net.mullvad.mullvadvpn.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.mullvad.mullvadvpn.R
import net.mullvad.mullvadvpn.model.Settings

class PreferencesFragment : ServiceDependentFragment(OnNoService.GoBack) {
    private lateinit var allowLanToggle: CellSwitch
    private lateinit var autoConnectToggle: CellSwitch
    private var subscriptionId: Int? = null

    private var updateUiJob: Job? = null

    override fun onSafelyCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.preferences, container, false)

        view.findViewById<View>(R.id.back).setOnClickListener {
            parentActivity.onBackPressed()
        }

        allowLanToggle = view.findViewById<CellSwitch>(R.id.allow_lan_toggle).apply {
            settingsListener.settings?.let { settings ->
                if (settings.allowLan) {
                    forcefullySetState(CellSwitch.State.ON)
                } else {
                    forcefullySetState(CellSwitch.State.OFF)
                }
            }

            listener = { state ->
                when (state) {
                    CellSwitch.State.ON -> daemon.setAllowLan(true)
                    CellSwitch.State.OFF -> daemon.setAllowLan(false)
                }
            }
        }

        autoConnectToggle = view.findViewById<CellSwitch>(R.id.auto_connect_toggle).apply {
            settingsListener.settings?.let { settings ->
                if (settings.autoConnect) {
                    forcefullySetState(CellSwitch.State.ON)
                } else {
                    forcefullySetState(CellSwitch.State.OFF)
                }
            }

            listener = { state ->
                when (state) {
                    CellSwitch.State.ON -> daemon.setAutoConnect(true)
                    CellSwitch.State.OFF -> daemon.setAutoConnect(false)
                }
            }
        }

        settingsListener.subscribe({ settings -> updateUi(settings) })

        return view
    }

    private fun updateUi(settings: Settings) {
        updateUiJob?.cancel()
        updateUiJob = GlobalScope.launch(Dispatchers.Main) {
            allowLanToggle.state = boolToSwitchState(settings.allowLan)
            autoConnectToggle.state = boolToSwitchState(settings.autoConnect)
        }
    }

    override fun onSafelyDestroyView() {
        subscriptionId?.let { id -> settingsListener.unsubscribe(id) }
    }

    private fun boolToSwitchState(pref: Boolean): CellSwitch.State {
        if (pref) {
            return CellSwitch.State.ON
        } else {
            return CellSwitch.State.OFF
        }
    }
}
