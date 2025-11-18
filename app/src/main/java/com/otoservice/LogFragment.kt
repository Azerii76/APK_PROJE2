package com.otoservice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class LogFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_logs, container, false)
        val prefs = PreferenceStore.get(requireContext())
        val rv = view.findViewById<RecyclerView>(R.id.rvLogs)
        val btnClear = view.findViewById<Button>(R.id.btnClearLogs)
        val adapter = LogAdapter()
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter.submit(loadLogs(prefs))
        btnClear.setOnClickListener {
            prefs.setString(PreferenceStore.KEY_LOGS, "[]")
            adapter.submit(emptyList())
        }
        return view
    }

    private fun loadLogs(prefs: PreferenceStore): List<AutoReplyLog> {
        val raw = prefs.getString(PreferenceStore.KEY_LOGS, "[]")
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { idx ->
            val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
            AutoReplyLog(
                appName = obj.optString("app"),
                target = obj.optString("target"),
                timestamp = obj.optLong("time"),
                preview = obj.optString("msg")
            )
        }.reversed()
    }
}

class LogAdapter : RecyclerView.Adapter<LogVH>() {
    private val items = mutableListOf<AutoReplyLog>()

    fun submit(list: List<AutoReplyLog>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogVH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LogVH, position: Int) {
        holder.bind(items[position])
    }
}

class LogVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: AutoReplyLog) {
        itemView.findViewById<android.widget.TextView>(R.id.tvLogApp).text = item.appName
        itemView.findViewById<android.widget.TextView>(R.id.tvLogTarget).text = "Hedef: ${item.target}"
        itemView.findViewById<android.widget.TextView>(R.id.tvLogTime).text =
            android.text.format.DateFormat.format("dd.MM.yyyy HH:mm", item.timestamp)
        itemView.findViewById<android.widget.TextView>(R.id.tvLogMessage).text = item.preview
    }
}
