package org.ifaco.mybluetooth.adap

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import org.ifaco.mybluetooth.Connect
import org.ifaco.mybluetooth.Fun.Companion.vis
import org.ifaco.mybluetooth.Main.Companion.bluetoothAdapter
import org.ifaco.mybluetooth.R

class PairedAdap(val c: Context, val list: List<BluetoothDevice>) :
    RecyclerView.Adapter<PairedAdap.MyViewHolder>() {

    class MyViewHolder(val v: ConstraintLayout) : RecyclerView.ViewHolder(v)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        var v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paired, parent, false) as ConstraintLayout
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(h: MyViewHolder, i: Int) {
        val main = h.v[mainPos] as LinearLayout
        val name = main[namePos] as TextView
        val address = main[addressPos] as TextView
        val borderBottom = h.v[borderBottomPos]

        // Texts
        name.text = list[i].name
        address.text = list[h.layoutPosition].address

        // Border Bottom
        vis(borderBottom, i != list.size - 1)

        // Clicks
        h.v.setOnClickListener {
            //Toast.makeText(c, bondState(list[i].bondState), Toast.LENGTH_LONG).show()
            var conn = Connect(bluetoothAdapter, list[h.layoutPosition])
            conn.start()
            object : CountDownTimer(10000, 10000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    conn.cancel()
                }
            }.start()
        }
    }

    override fun getItemCount() = list.size


    companion object {
        const val mainPos = 0
        const val namePos = 0
        const val addressPos = 1
        const val borderBottomPos = 1

        fun bondState(state: Int): String = when (state) {
            12 -> "BONDED"
            11 -> "BONDING"
            10 -> "NONE"
            else -> "UNKNOWN"
        }
    }
}