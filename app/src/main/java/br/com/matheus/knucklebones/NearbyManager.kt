package br.com.matheus.knucklebones

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyManager(private val context: Context) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val STRATEGY = Strategy.P2P_POINT_TO_POINT
    private val SERVICE_ID = "br.com.matheus.knucklebones.SERVICE_ID"

    private val _connectionState = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionState = _connectionState.asStateFlow()

    private val _opponentEndpointId = MutableStateFlow<String?>(null)
    val opponentEndpointId = _opponentEndpointId.asStateFlow()

    private val _receivedMessage = MutableStateFlow<String?>(null)
    val receivedMessage = _receivedMessage.asStateFlow()

    private var isHostDevice = false
    fun isHost() = isHostDevice

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _connectionState.value = ConnectionStatus.EndpointFound(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            if (_connectionState.value is ConnectionStatus.EndpointFound && 
                (_connectionState.value as ConnectionStatus.EndpointFound).endpointId == endpointId) {
                _connectionState.value = ConnectionStatus.Discovering
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            _connectionState.value = ConnectionStatus.Connecting(endpointId, info.endpointName)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                _opponentEndpointId.value = endpointId
                _connectionState.value = ConnectionStatus.Connected(endpointId)
                connectionsClient.stopAdvertising()
                connectionsClient.stopDiscovery()
            } else {
                _connectionState.value = ConnectionStatus.Error("Connection failed: ${result.status.statusMessage}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectionState.value = ConnectionStatus.Idle
            _opponentEndpointId.value = null
            isHostDevice = false
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let {
                _receivedMessage.value = String(it)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    fun startAdvertising(username: String) {
        isHostDevice = true
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(username, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnSuccessListener { _connectionState.value = ConnectionStatus.Advertising }
            .addOnFailureListener { 
                isHostDevice = false
                _connectionState.value = ConnectionStatus.Error(it.message ?: "Advertising failed") 
            }
    }

    fun startDiscovery() {
        isHostDevice = false
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { _connectionState.value = ConnectionStatus.Discovering }
            .addOnFailureListener { _connectionState.value = ConnectionStatus.Error(it.message ?: "Discovery failed") }
    }

    fun connectToEndpoint(endpointId: String, username: String) {
        connectionsClient.requestConnection(username, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { _connectionState.value = ConnectionStatus.Error(it.message ?: "Request connection failed") }
    }

    fun sendMessage(message: String) {
        val endpointId = _opponentEndpointId.value ?: return
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(message.toByteArray()))
    }

    fun disconnect() {
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        _connectionState.value = ConnectionStatus.Idle
        _opponentEndpointId.value = null
        isHostDevice = false
    }
    
    fun clearReceivedMessage() {
        _receivedMessage.value = null
    }
}

sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object Advertising : ConnectionStatus()
    object Discovering : ConnectionStatus()
    data class EndpointFound(val endpointId: String, val endpointName: String) : ConnectionStatus()
    data class Connecting(val endpointId: String, val endpointName: String) : ConnectionStatus()
    data class Connected(val endpointId: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
