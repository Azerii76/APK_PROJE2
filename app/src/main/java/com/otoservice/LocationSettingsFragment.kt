package com.otoservice

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class LocationSettingsFragment : Fragment() {

    private lateinit var prefs: PreferenceStore

    private val istanbulDistricts = listOf(
        "Adalar", "Arnavutköy", "Ataşehir", "Avcılar", "Bağcılar", "Bahçelievler",
        "Bakırköy", "Başakşehir", "Bayrampaşa", "Beşiktaş", "Beykoz", "Beylikdüzü",
        "Beyoğlu", "Büyükçekmece", "Çatalca", "Çekmeköy", "Esenler", "Esenyurt",
        "Eyüpsultan", "Fatih", "Gaziosmanpaşa", "Güngören", "Kadıköy", "Kağıthane",
        "Kartal", "Küçükçekmece", "Maltepe", "Pendik", "Sancaktepe", "Sarıyer",
        "Silivri", "Sultanbeyli", "Sultangazi", "Şile", "Şişli", "Tuzla",
        "Ümraniye", "Üsküdar", "Zeytinburnu"
    )
    private val locationIntervals = listOf(10, 15, 20, 30, 45, 60, 90, 120)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_location_settings, container, false)
        prefs = PreferenceStore.get(requireContext())

        val switchLocation = view.findViewById<SwitchMaterial>(R.id.switchLocationEnabled)
        val spinnerDistrict = view.findViewById<Spinner>(R.id.spinnerDistrict)
        val spinnerInterval = view.findViewById<Spinner>(R.id.spinnerLocationInterval)

        val districtAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, istanbulDistricts)
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDistrict.adapter = districtAdapter

        val intervalAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locationIntervals.map { "$it Dakika" })
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = intervalAdapter

        switchLocation.isChecked = prefs.getBoolean(PreferenceStore.KEY_LOCATION_ENABLED, false)
        val savedDistrict = prefs.getString(PreferenceStore.KEY_LOCATION_DISTRICT, "Kadıköy")
        spinnerDistrict.setSelection(istanbulDistricts.indexOf(savedDistrict).coerceAtLeast(0))

        // HATA DÜZELTMESİ: Ayarı Int yerine Long olarak oku
        val savedInterval = prefs.getLong(PreferenceStore.KEY_LOCATION_INTERVAL_MIN, 10L).toInt()
        spinnerInterval.setSelection(locationIntervals.indexOf(savedInterval).coerceAtLeast(0))

        switchLocation.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBoolean(PreferenceStore.KEY_LOCATION_ENABLED, isChecked)
            toggleService(isChecked)
        }

        spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = istanbulDistricts[position]
                prefs.setString(PreferenceStore.KEY_LOCATION_DISTRICT, selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = locationIntervals[position]
                // HATA DÜZELTMESİ: Ayarı Int yerine Long olarak kaydet
                prefs.setLong(PreferenceStore.KEY_LOCATION_INTERVAL_MIN, selected.toLong())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return view
    }

    private fun toggleService(start: Boolean) {
        val intent = Intent(requireContext(), LocationAutomationService::class.java)
        if (start) {
            requireContext().startService(intent)
        } else {
            requireContext().stopService(intent)
        }
    }
}
