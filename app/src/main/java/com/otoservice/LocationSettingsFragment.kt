package com.otoservice

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.otoservice.LocationDefaults.RANDOM_DISTRICT
import com.otoservice.LocationDefaults.districts

class LocationSettingsFragment : Fragment() {

    private lateinit var prefs: PreferenceStore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_location_settings, container, false)
        prefs = PreferenceStore.get(requireContext())

        val switchLocation = view.findViewById<SwitchMaterial>(R.id.switchLocationEnabled)
        val spinnerDistrict = view.findViewById<Spinner>(R.id.spinnerDistrict)
        val spinnerInterval = view.findViewById<Spinner>(R.id.spinnerLocationInterval)
        val btnDeveloper = view.findViewById<Button>(R.id.btnOpenDeveloper)
        val tvStatus = view.findViewById<TextView>(R.id.tvLocationStatus)

        val districtsWithRandom = listOf(RANDOM_DISTRICT) + districts
        val districtAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, districtsWithRandom)
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDistrict.adapter = districtAdapter

        val intervalAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            FrequencyDefaults.locationMinutes.map { "$it Dakika" }
        )
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = intervalAdapter

        switchLocation.isChecked = prefs.getBoolean(PreferenceStore.KEY_LOCATION_ENABLED, false)
        val savedDistrict = prefs.getString(PreferenceStore.KEY_LOCATION_DISTRICT, RANDOM_DISTRICT)
        spinnerDistrict.setSelection(districtsWithRandom.indexOf(savedDistrict).coerceAtLeast(0))

        // HATA DÜZELTMESİ: Ayarı Int yerine Long olarak oku
        val savedInterval = prefs.getLong(PreferenceStore.KEY_LOCATION_INTERVAL_MIN, 15L).toInt()
        spinnerInterval.setSelection(FrequencyDefaults.locationMinutes.indexOf(savedInterval).coerceAtLeast(0))

        switchLocation.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBoolean(PreferenceStore.KEY_LOCATION_ENABLED, isChecked)
            toggleService(isChecked)
            updateStatus(tvStatus, isChecked)
        }

        spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = districtsWithRandom[position]
                prefs.setString(PreferenceStore.KEY_LOCATION_DISTRICT, selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = FrequencyDefaults.locationMinutes[position]
                // HATA DÜZELTMESİ: Ayarı Int yerine Long olarak kaydet
                prefs.setLong(PreferenceStore.KEY_LOCATION_INTERVAL_MIN, selected.toLong())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnDeveloper.setOnClickListener {
            Toast.makeText(requireContext(), R.string.mock_location_instruction, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }

        if (switchLocation.isChecked) {
            toggleService(true)
        }
        updateStatus(tvStatus, switchLocation.isChecked)

        return view
    }

    private fun toggleService(start: Boolean) {
        val intent = Intent(requireContext(), LocationAutomationService::class.java)
        if (start) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
        } else {
            requireContext().stopService(intent)
        }
    }

    private fun updateStatus(tv: TextView, enabled: Boolean) {
        tv.text = if (enabled) {
            getString(R.string.location_status_on)
        } else {
            getString(R.string.location_status_off)
        }
    }
}
