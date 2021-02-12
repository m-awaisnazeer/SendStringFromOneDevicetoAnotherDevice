package com.communisolve.sendstringfromoneandroiddevicetootherandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.communisolve.p2pwifidirectkotlin.RVAdapter
import com.communisolve.p2pwifidirectkotlin.WiFiDirectBroadcastReceiver
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class MainActivity : AppCompatActivity(), WifiP2pManager.PeerListListener,
        WifiP2pManager.ConnectionInfoListener,
        IRecyclerViewItemClickListner {

    companion object {
        const val TAG = "MainActivity"
        private const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
        protected const val CHOOSE_IMAGE_RESULT_CODE = 20
        protected const val CHOOSE_Video_RESULT_CODE = 30
        protected const val CHOOSE_PDF_RESULT_CODE = 40
        public var currentdeviceip: String?=null


    }


    private lateinit var currentDevice: WifiP2pDevice
    private lateinit var alertDialog: AlertDialog
    private var manager: WifiP2pManager? = null
    private var isWifiP2pEnabled = false
    private val retryChannel = false
    var progress_rl: RelativeLayout? = null
    lateinit var dialogBuilder: AlertDialog.Builder
    lateinit var txt_availablepeers: TextView
    lateinit var available_devices_list_RV: RecyclerView
    lateinit var connection_ll: LinearLayout
    private val intentFilter = IntentFilter()
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null
    lateinit var mInfo: WifiP2pInfo
    lateinit var message_edit_text:EditText


    var mPeers: MutableList<WifiP2pDevice> = ArrayList()
    var progressDialog: ProgressDialog? = null
    var mContentView: View? = null
    private var device: WifiP2pDevice? = null
    lateinit var txt_no_availablepeers: TextView


    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }

    private fun getDeviceStatus(deviceStatus: Int): String? {
        Log.d(TAG, "Peer status :$deviceStatus")
        return when (deviceStatus) {
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Fine LOcation Is not granted")
                finish()
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun initP2p(): Boolean {
        // Device capability definition check
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.")
            return false
        }

        // Hardware capability check
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.")
            return false
        }
        if (!wifiManager.isP2pSupported) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.")
            return false
        }
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.")
            return false
        }
        channel = manager!!.initialize(this, mainLooper, null)
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.")
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        connection_ll = findViewById(R.id.connection_ll)
        message_edit_text = findViewById(R.id.message_edit_text)

        // pb = findViewById(R.id.pb)

        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        if (!initP2p()) {
            finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION
            )
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }



    }

    fun onOffP2P(view: View) {
        if (manager != null && channel != null) {

            // Since this is the system wireless settings activity, it's
            // not going to send us a result. We will be notified by
            // WiFiDeviceBroadcastReceiver instead.
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        } else {
            Log.e(TAG, "channel or manager is null")
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverP2P(view: View) {
        if (!isWifiP2pEnabled) {
            Toast.makeText(
                    this@MainActivity, "First Enable P2P ",
                    Toast.LENGTH_SHORT
            ).show()
            return
        }

        dialogBuilder = AlertDialog.Builder(this)

        val dialog: View =
                LayoutInflater.from(this)
                        .inflate(R.layout.peers_list_dialog, null)
        dialogBuilder.setView(dialog)
        progress_rl = dialog.findViewById<RelativeLayout>(R.id.progress_rl) as RelativeLayout
        txt_availablepeers = dialog.findViewById<TextView>(R.id.txt_availablepeers) as TextView
        txt_no_availablepeers =
                dialog.findViewById<TextView>(R.id.txt_no_availablepeers) as TextView
        available_devices_list_RV =
                dialog.findViewById(R.id.available_devices_list_RV) as RecyclerView
        available_devices_list_RV.setHasFixedSize(true)
        available_devices_list_RV.layoutManager = LinearLayoutManager(this)


        alertDialog = dialogBuilder.create()

        alertDialog.show()

        manager!!.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(
                        this@MainActivity, "Discovery Initiated",
                        Toast.LENGTH_SHORT
                ).show()
            }

            override fun onFailure(reasonCode: Int) {
                Toast.makeText(
                        this@MainActivity, "Discovery Failed : $reasonCode",
                        Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
        mPeers.clear()
        mPeers.addAll(peers!!.deviceList)

        available_devices_list_RV.adapter = RVAdapter(mPeers, this)
        progress_rl!!.visibility = View.GONE
        txt_availablepeers.visibility = View.VISIBLE
        if (mPeers.size == 0) {
            progress_rl!!.visibility = View.GONE
            Log.d(TAG, "No devices found")
            Toast.makeText(this, "No Devices Found", Toast.LENGTH_SHORT).show()
            txt_no_availablepeers.visibility = View.VISIBLE
            return
        }


    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {

        mInfo = info!!
        // this.getView().setVisibility(View.VISIBLE)

        // The owner IP is now known.

        // The owner IP is now known.
        var group_owner =
                findViewById<View>(R.id.group_owner) as TextView
        group_owner.text =
                resources.getString(R.string.group_owner_text) + if (info.isGroupOwner == true) resources.getString(
                        R.string.yes
                ) else resources.getString(
                        R.string.no
                )

        // InetAddress from WifiP2pInfo struct.

        // InetAddress from WifiP2pInfo struct.
        var device_info = findViewById<View>(R.id.device_info) as TextView
        device_info.text = "Group Owner IP - " + mInfo.groupOwnerAddress.hostAddress

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
             findViewById<RelativeLayout>(R.id.transfer_data_layout).visibility = View.GONE
             AppListener().execute()
             EventBus.getDefault().post(SendMessageOrCloseDialog(closeDialog = true,sendMessage = false,receivedMessage = false,
                 message = ""
             ))
             (findViewById<View>(R.id.status_text) as TextView).text ="This is Server Device and will receive Message"
         } else   if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            findViewById<RelativeLayout>(R.id.transfer_data_layout).visibility = View.VISIBLE

            (findViewById<View>(R.id.status_text) as TextView).text = resources
                .getString(R.string.client_text)

            EventBus.getDefault().post(SendMessageOrCloseDialog(closeDialog = true,sendMessage = false,receivedMessage = false,
                message = ""
            ))
        }

        // hide the connect button

        // hide the connect button
        findViewById<View>(R.id.btn_connect).visibility = View.GONE
        findViewById<View>(R.id.transfer_data_layout).visibility = View.VISIBLE



        findViewById<View>(R.id.btn_disconnect).setOnClickListener {
            disconnect()
        }




    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    override fun onResume() {
        super.onResume()
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    @SuppressLint("MissingPermission")
    override fun onItemClick(position: Int) {
        alertDialog.dismiss()
        currentDevice = mPeers.get(position)
        connection_ll.visibility = View.VISIBLE

        val config = WifiP2pConfig()
        config.deviceAddress = currentDevice.deviceAddress
        config.wps.setup = WpsInfo.PBC

        manager!!.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Toast.makeText(this@MainActivity, "Connected Successfully", Toast.LENGTH_SHORT)
                        .show()

                EventBus.getDefault().post(SendMessageOrCloseDialog(closeDialog = true,sendMessage = false,receivedMessage = false,
                    message = ""
                ))
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(
                        this@MainActivity, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT
                ).show()
            }
        })
    }



    fun disconnect() {
        manager!!.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(
                        TAG,
                        "Disconnect failed. Reason :$reasonCode"
                )
            }

            override fun onSuccess() {
                connection_ll.visibility = View.GONE
            }
        })
    }




    fun updateThisDevice(wifiP2pDevice: WifiP2pDevice?) {

        device = wifiP2pDevice
        var view = findViewById<View>(R.id.my_name) as TextView
        view.text = device!!.deviceName
        view = findViewById<View>(R.id.my_status) as TextView
        view.setText(getDeviceStatus(device!!.status))
    }


    class AppListener : AsyncTask<String?, String?, String?>() {
        override fun doInBackground(vararg params: String?): String? {
            var msg_received: String? = null
            println("LISTENING FOR LAST INSTALLED APP")
            Log.d("tag_name", "Entered Server AsyncTask")

            try {
                println("TRY")
                val socket = ServerSocket(1755)
                println("Connect to Socket and listening")
                Log.d("tag_name", "Connect to Socket and listening")

                val clientSocket: Socket =
                        socket.accept() //This is blocking. It will wait.
                println("This should print after connection")
                val DIS = DataInputStream(clientSocket.getInputStream())
                msg_received = DIS.readUTF()

                EventBus.getDefault().post(SendMessageOrCloseDialog(closeDialog = false,sendMessage = false,receivedMessage = true,
                    message = msg_received.toString()
                ))

                println("Message from server$msg_received")
                Log.d("tag_name", "Message from server $msg_received")

                clientSocket.close()
                socket.close()
            } catch (e: Exception) {
                println("Did not connect to SendString")
            }
            println("Return Statement is Reached")
            return msg_received
        }
    }

    class SendString : AsyncTask<String?, String?, String?>() {
        override fun doInBackground(vararg params: String?): String? {
            Log.d("tag_name", "Entered AsyncTask")
//            val applicationName = "Testt......."

            val applicationName = params[0]


            // SEND APPLICATION NAME AND ICON TO OTHER APP
            try {
                Log.d("tag_name", "TRY TO SEND STRING")
                val socket = Socket(currentdeviceip, 1755)
                val DOS = DataOutputStream(socket.getOutputStream())
                DOS.writeUTF(applicationName)
                Log.d("tag_name", "Application Name Sent!")
                socket.close()
            } catch (e: java.lang.Exception) {
                Log.d("tag_name", "Did not send string")
            }
            return null
        }
    }

    fun SendTextMessagetotherDevice(view: View) {

        val string = message_edit_text.text.toString()
        if (!TextUtils.isEmpty(string)){
           EventBus.getDefault().post(SendMessageOrCloseDialog(false,true,false,string))
        }
        message_edit_text.setText("")
    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun sendMessageorCloseDialog(event:SendMessageOrCloseDialog){
        if (event.closeDialog){
            if (alertDialog.isShowing){
                alertDialog.dismiss()
            }
        }
        if(event.sendMessage){
            currentdeviceip=mInfo.groupOwnerAddress.hostAddress
            SendString().execute(event.message)
        }

        if (event.receivedMessage){
            findViewById<TextView>(R.id.received_message).text = event.message
        }
    }
}