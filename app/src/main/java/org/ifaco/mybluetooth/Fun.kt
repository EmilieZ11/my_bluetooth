package org.ifaco.mybluetooth

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class Fun {
    companion object {
        lateinit var c: Context
        lateinit var sp: SharedPreferences

        var dm = DisplayMetrics()


        fun init(that: AppCompatActivity) {
            c = that.applicationContext// if (!::c.isInitialized)
            if (!::sp.isInitialized) sp =
                that.getSharedPreferences("${that.packageName}_preferences", Context.MODE_PRIVATE)
            sp = that.getPreferences(Context.MODE_PRIVATE)
            dm = that.resources.displayMetrics
        }

        fun dp(px: Int = 0) = (dm.density * px.toFloat()).toInt()

        fun vis(v: View, b: Boolean = true) {
            v.visibility = if (b) View.VISIBLE else View.GONE
        }

        fun colour(
            c: Context, res: Int = R.color.CA, method: PorterDuff.Mode = PorterDuff.Mode.SRC_IN
        ) = PorterDuffColorFilter(ContextCompat.getColor(c, res), method)

        fun onLoad(view: View, func: Function): CountDownTimer =
            object : CountDownTimer(10000, 50) {
                override fun onFinish() {}
                override fun onTick(millisUntilFinished: Long) {
                    if (view.width <= 0) return
                    func.execute()
                    this.cancel()
                }
            }.start()
    }


    interface Function {
        fun execute()
    }
}