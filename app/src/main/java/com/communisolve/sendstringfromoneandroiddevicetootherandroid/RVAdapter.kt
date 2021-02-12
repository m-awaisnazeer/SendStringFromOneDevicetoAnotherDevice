package com.communisolve.p2pwifidirectkotlin

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.communisolve.sendstringfromoneandroiddevicetootherandroid.IRecyclerViewItemClickListner
import com.communisolve.sendstringfromoneandroiddevicetootherandroid.R

class RVAdapter(
    var peers: List<WifiP2pDevice>,
    var iRecyclerViewItemClickListner: IRecyclerViewItemClickListner
) : RecyclerView.Adapter<RVAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        lateinit var device_name: TextView
        lateinit var device_details:TextView

        init {
            device_name = itemView.findViewById(R.id.device_name)
            device_details = itemView.findViewById(R.id.device_details)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.row_devices, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return peers.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.device_name.setText(peers.get(position).deviceName)
        holder.device_details.setText(peers.get(position).deviceAddress)

        holder.itemView.setOnClickListener {
            iRecyclerViewItemClickListner.onItemClick(position)
        }
    }
}