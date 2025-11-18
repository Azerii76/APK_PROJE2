package com.otoservice

import android.view.View
import android.widget.AdapterView

class SimpleItemSelectedListener(private val onSelect: (Int) -> Unit) : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onSelect(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
