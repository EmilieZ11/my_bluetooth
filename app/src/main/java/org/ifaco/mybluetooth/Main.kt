package org.ifaco.mybluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.ifaco.mybluetooth.Fun.Companion.c
import org.ifaco.mybluetooth.Fun.Companion.vis
import org.ifaco.mybluetooth.adap.PairedAdap
import kotlin.collections.ArrayList

class Main : AppCompatActivity() {
    lateinit var body: ConstraintLayout
    lateinit var toolbar: Toolbar
    lateinit var inner: ConstraintLayout
    lateinit var mainSV: ScrollView
    lateinit var mainLL: LinearLayout
    lateinit var paired: RecyclerView
    lateinit var avail: RecyclerView
    lateinit var error: ConstraintLayout
    lateinit var errorIcon: ImageView
    lateinit var notSupported: TextView
    lateinit var youNeedIt: TextView

    var tapToExit = false
    //val reqBluetoothOn = 0
    var pairedAdap: PairedAdap? = null
    val menuTurnBT = 0
    val menuSearch = 1

    companion object {
        const val CHANNEL_ID = "org.ifaco.mybluetooth"
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        var stateHandler: Handler? = null
        var connectHandler: Handler? = null
        var pairedDevices: MutableList<BluetoothDevice> = mutableListOf()
        var bluetoothOn = false
        val notifId = 1072

        /*var bluetoothHeadset: BluetoothHeadset? = null
        private val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.HEADSET)
                    bluetoothHeadset = proxy as BluetoothHeadset
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HEADSET)
                    bluetoothHeadset = null
            }
        }*/

        private val discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Toast.makeText(c, "onReceive", Toast.LENGTH_SHORT).show()
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        Toast.makeText(c, "ACTION_FOUND", Toast.LENGTH_SHORT).show()
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null) {
                            Toast.makeText(c, device.name, Toast.LENGTH_LONG).show()
                            pairedDevices.add(device)
                        }
                    }
                }
            }
        }
        private val stateReceiver = object : BroadcastReceiver() {
            var newState: Int? = null
            var oldState: Int? = null

            override fun onReceive(c: Context, intent: Intent) {
                oldState = intent.extras?.getInt(BluetoothAdapter.EXTRA_PREVIOUS_STATE)
                newState = intent.extras?.getInt(BluetoothAdapter.EXTRA_STATE)

                if (newState != null)
                    stateHandler?.obtainMessage(newState!!)?.sendToTarget()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        body = findViewById(R.id.body)
        toolbar = findViewById(R.id.toolbar)
        inner = findViewById(R.id.inner)
        mainSV = findViewById(R.id.mainSV)
        mainLL = findViewById(R.id.mainLL)
        paired = findViewById(R.id.paired)
        avail = findViewById(R.id.avail)
        error = findViewById(R.id.error)
        errorIcon = findViewById(R.id.errorIcon)
        notSupported = findViewById(R.id.notSupported)
        youNeedIt = findViewById(R.id.youNeedIt)

        Fun.init(this)
        errorIcon.colorFilter = Fun.colour(c, R.color.errorIcon)
        setSupportActionBar(toolbar)


        // Handlers
        stateHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    BluetoothAdapter.STATE_OFF -> {// 10
                        bluetoothOn = false
                        innerState(2)
                        arrangePaired(setOf<BluetoothDevice>())
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {// 11
                    }
                    BluetoothAdapter.STATE_ON -> {// 12
                        bluetoothOn = true
                        check()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {// 13
                        removeNotif()
                    }
                    101 -> {// On by Default
                        bluetoothOn = true
                    }
                }
                toolbar.menu.getItem(menuTurnBT)?.apply {
                    icon = ContextCompat.getDrawable(
                        c,
                        if (bluetoothOn) R.drawable.bluetooth_on_1 else R.drawable.bluetooth_off_1
                    )
                    title = resources.getString(
                        if (bluetoothOn) R.string.mTurnBluetoothOff else R.string.mTurnBluetoothOn
                    )
                }
                toolbar.menu.getItem(menuSearch)?.isEnabled = bluetoothOn
            }
        }
        connectHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                Toast.makeText(c, "${msg.obj}", Toast.LENGTH_SHORT).show()
            }
        }

        // Receivers
        registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(stateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        // Load
        Fun.onLoad(toolbar, object : Fun.Function {
            override fun execute() {
                check()
            }
        })
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Receivers
        unregisterReceiver(discoveryReceiver)
        unregisterReceiver(stateReceiver)

        // Other
        removeNotif()
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putParcelableArrayList("pairedDevices", ArrayList(pairedDevices))
        super.onSaveInstanceState(state)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoration(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            /*reqBluetoothOn -> {
                if (resultCode == RESULT_OK) start()
                else innerState(2)
            }*/
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.mTurnBluetooth -> {
            if (!bluetoothOn) /*startActivityForResult(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                reqBluetoothOn
            )*/ bluetoothAdapter?.enable()
            else bluetoothAdapter?.disable()
            true
        }
        R.id.mMenuSearch -> {
            discovery(); true
        }
        else -> super.onOptionsItemSelected(item!!)
    }

    override fun onBackPressed() {
        if (!tapToExit) {
            Toast.makeText(c, R.string.tapToExit, Toast.LENGTH_LONG).show()
            tapToExit = true
            object : CountDownTimer(3000, 3000) {
                override fun onTick(p0: Long) {}
                override fun onFinish() {
                    tapToExit = false
                }
            }.start(); return
        }
        moveTaskToBack(true)
        Process.killProcess(Process.myPid())
        kotlin.system.exitProcess(1)
    }


    fun restoration(state: Bundle?) {
        if (state == null) return
        if (state.containsKey("pairedDevices"))
            state.getParcelableArrayList<BluetoothDevice>("pairedDevices")?.let {
                pairedDevices = it.toMutableList()
                pairedAdap = PairedAdap(c, pairedDevices.toList())
                paired.adapter = pairedAdap
            }
    }

    fun start() {
        innerState(0)
        if (bluetoothAdapter == null) return
        arrangePaired(bluetoothAdapter.bondedDevices)
        createNoitf()
        //bluetoothAdapter?.scanMode

        //bluetoothAdapter.getProfileProxy(c, profileListener, BluetoothProfile.HEADSET)
        //bluetoothHeadset
        /*object : CountDownTimer(30000, 30000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
                Toast.makeText(c, "CLOSED!", Toast.LENGTH_LONG).show()
            }
        }.start()*/
    }

    fun check() {
        if (bluetoothAdapter == null) {
            innerState(1)
            return
        }
        if (bluetoothAdapter.isEnabled) {
            start()
            if (!bluetoothOn) stateHandler?.obtainMessage(101)?.sendToTarget()
        } else innerState(2)
    }

    fun innerState(i: Int = 0) = when (i) {
        0 -> {
            vis(mainSV)
            vis(error, false)
        }
        1 -> {
            vis(mainSV, false)
            vis(error)
            vis(notSupported)
            vis(youNeedIt, false)
        }
        2 -> {
            vis(mainSV, false)
            vis(error)
            vis(notSupported, false)
            vis(youNeedIt)
        }
        else -> {
        }
    }

    fun arrangePaired(list: Set<BluetoothDevice>?) {
        if (list == null) return
        val newList = list.toMutableList()
        var changed = false
        if (pairedDevices.isNotEmpty()) {
            for (i in newList.indices) if (newList[i] != pairedDevices[i]) changed = true
        } else changed = true
        if (!changed) return
        pairedDevices = newList
        pairedAdap = PairedAdap(c, pairedDevices.toList())
        paired.adapter = pairedAdap
    }

    fun discovery() {
        if (bluetoothAdapter == null) return
        bluetoothAdapter.startDiscovery()
        object : CountDownTimer(12000, 12000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (bluetoothAdapter == null) return
                if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
                Toast.makeText(c, "onFinish", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    fun createNoitf() {
        //val openMain = PendingIntent.getActivity(c, 0, Intent(c, Main::class.java), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
                .apply { description = descriptionText }
            val ntfManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ntfManager.createNotificationChannel(channel)
        }
        var builder = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.bluetooth_on_1)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.notifOn))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            //.setContentIntent(openMain)
            .setColor(ContextCompat.getColor(c, R.color.notifBG))
            .setColorized(true)
            .setOngoing(true)
        with(NotificationManagerCompat.from(this)) { notify(notifId, builder.build()) }
    }

    fun removeNotif() {
        var ntfManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ntfManager.cancel(notifId)
    }
}