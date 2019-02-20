package com.ex.franklinsamboni.mobilemouse.models

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class EchoClient {
    private val socket: DatagramSocket
    private val address: InetAddress

    private var buf: ByteArray? = null

    init {
        socket = DatagramSocket()

        address = InetAddress.getByName("10.125.64.234")
        print("hola")
    }

    fun sendEcho(msg: String): String {
        buf = msg.toByteArray()
        var packet = DatagramPacket(buf, buf!!.size, address, 4445)
        socket.send(packet)
        return "ok"
        //packet = DatagramPacket(buf, buf!!.size)
        //socket.receive(packet)
        //return String(
        //    packet.getData(), 0, packet.getLength()
        //)
    }

    fun close() {
        socket.close()
    }
}