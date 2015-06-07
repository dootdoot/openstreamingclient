package net;
import java.awt.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import net.Stream.CVideoEncoderInfoMsg;

import org.bouncycastle.util.encoders.Hex;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;


public class TCPGameStream implements Runnable {
	private static final int PACKET_TYPE_CONNECT = 1;
	private static final int PACKET_TYPE_CONNECT_RESPONSE = 2;
	private static final int PACKET_TYPE_DATA = 3;
	private static final int PACKET_TYPE_CONTROL = 5;
	private static final int PACKET_TYPE_CONTROL_CONTINUED = 6;
	private static final int PACKET_TYPE_CONTROL_ACKNOWLEDGE = 7;
	private static final int PACKET_TYPE_DISCONNECT = 9;

	private InetSocketAddress isa = null;
	private ByteString authToken = null;
	private boolean connected = false;
	private DatagramSocket socket = null;
	private int receiverId = 0;
	private int senderId = 0x80;
	private short[] packetSequenceIds = new short[10];
	private ByteArrayOutputStream splitPacketBuffer = new ByteArrayOutputStream();
	private int splitPacketRemaining = 0;
	private int[] channelAssignments = new int[128];
	private OutputStream audioOut;
	private OutputStream loggingOut;
	private OutputStream loggingFull;
	
	private static final byte[] bad_bytes = new byte[]{
			(byte) 0x00,  (byte) 0x00,  (byte) 0x00,  (byte) 0x00,  (byte) 0x00,  (byte) 0x00,  (byte) 0x01,  (byte) 0x67,  (byte) 0x4d,  (byte) 0x40,  (byte) 0x0c,  (byte) 0xda,  (byte) 0x05,  (byte) 0x07,  (byte) 0xe8,  (byte) 0x40,  
			(byte) 0x00,  (byte) 0x00,  (byte) 0x03,  (byte) 0x00,  (byte) 0x40,  (byte) 0x00,  (byte) 0x00,  (byte) 0x03,  (byte) 0x00,  (byte) 0xa3,  (byte) 0xc5,  (byte) 0x0a,  (byte) 0xa8,  (byte) 0x00,  (byte) 0x00,  (byte) 0x00,  
			(byte) 0x01,  (byte) 0x68,  (byte) 0xef,  (byte) 0x0f,  (byte) 0xc8,  (byte) 0x00,  (byte) 0x00,  (byte) 0x00,  (byte) 0x01,  (byte) 0x06,  (byte) 0x05,  (byte) 0xff,  (byte) 0xff,  (byte) 0xb3,  (byte) 0xdc,  (byte) 0x45,  
			(byte) 0xe9,  (byte) 0xbd,  (byte) 0xe6,  (byte) 0xd9,  (byte) 0x48,  (byte) 0xb7,  (byte) 0x96,  (byte) 0x2c,  (byte) 0xd8,  (byte) 0x20,  (byte) 0xd9,  (byte) 0x23,  (byte) 0xee,  (byte) 0xef,  (byte) 0x78,  (byte) 0x32,  
			(byte) 0x36,  (byte) 0x34,  (byte) 0x20,  (byte) 0x2d,  (byte) 0x20,  (byte) 0x63,  (byte) 0x6f,  (byte) 0x72,  (byte) 0x65,  (byte) 0x20,  (byte) 0x31,  (byte) 0x34,  (byte) 0x32,  (byte) 0x20,  (byte) 0x2d,  (byte) 0x20,  
			(byte) 0x48,  (byte) 0x2e,  (byte) 0x32,  (byte) 0x36,  (byte) 0x34,  (byte) 0x2f,  (byte) 0x4d,  (byte) 0x50,  (byte) 0x45,  (byte) 0x47,  (byte) 0x2d,  (byte) 0x34,  (byte) 0x20,  (byte) 0x41,  (byte) 0x56,  (byte) 0x43,  
			(byte) 0x20,  (byte) 0x63,  (byte) 0x6f,  (byte) 0x64,  (byte) 0x65,  (byte) 0x63,  (byte) 0x20,  (byte) 0x2d,  (byte) 0x20,  (byte) 0x43,  (byte) 0x6f,  (byte) 0x70,  (byte) 0x79,  (byte) 0x72,  (byte) 0x69,  (byte) 0x67,  
			(byte) 0x68,  (byte) 0x74,  (byte) 0x20,  (byte) 0x32,  (byte) 0x30,  (byte) 0x30,  (byte) 0x33,  (byte) 0x2d,  (byte) 0x32,  (byte) 0x30,  (byte) 0x31,  (byte) 0x34,  (byte) 0x20,  (byte) 0x2d,  (byte) 0x20,  (byte) 0x68,  
			(byte) 0x74,  (byte) 0x74,  (byte) 0x70,  (byte) 0x3a,  (byte) 0x2f,  (byte) 0x2f,  (byte) 0x77,  (byte) 0x77,  (byte) 0x77,  (byte) 0x2e,  (byte) 0x76,  (byte) 0x69,  (byte) 0x64,  (byte) 0x65,  (byte) 0x6f,  (byte) 0x6c,  
			(byte) 0x61,  (byte) 0x6e,  (byte) 0x2e,  (byte) 0x6f,  (byte) 0x72,  (byte) 0x67,  (byte) 0x2f,  (byte) 0x78,  (byte) 0x32,  (byte) 0x36,  (byte) 0x34,  (byte) 0x2e,  (byte) 0x68,  (byte) 0x74,  (byte) 0x6d,  (byte) 0x6c,  
			(byte) 0x20,  (byte) 0x2d,  (byte) 0x20,  (byte) 0x6f,  (byte) 0x70,  (byte) 0x74,  (byte) 0x69,  (byte) 0x6f,  (byte) 0x6e,  (byte) 0x73,  (byte) 0x3a,  (byte) 0x20,  (byte) 0x63,  (byte) 0x61,  (byte) 0x62,  (byte) 0x61,  
			(byte) 0x63,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x20,  (byte) 0x72,  (byte) 0x65,  (byte) 0x66,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x20,  (byte) 0x64,  (byte) 0x65,  (byte) 0x62,  (byte) 0x6c,  (byte) 0x6f,  (byte) 0x63,  
			(byte) 0x6b,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x3a,  (byte) 0x30,  (byte) 0x3a,  (byte) 0x30,  (byte) 0x20,  (byte) 0x61,  (byte) 0x6e,  (byte) 0x61,  (byte) 0x6c,  (byte) 0x79,  (byte) 0x73,  (byte) 0x65,  (byte) 0x3d,  
			(byte) 0x30,  (byte) 0x78,  (byte) 0x31,  (byte) 0x3a,  (byte) 0x30,  (byte) 0x78,  (byte) 0x31,  (byte) 0x20,  (byte) 0x6d,  (byte) 0x65,  (byte) 0x3d,  (byte) 0x64,  (byte) 0x69,  (byte) 0x61,  (byte) 0x20,  (byte) 0x73,  
			(byte) 0x75,  (byte) 0x62,  (byte) 0x6d,  (byte) 0x65,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x20,  (byte) 0x70,  (byte) 0x73,  (byte) 0x79,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x20,  (byte) 0x70,  (byte) 0x73,  (byte) 0x79,  
			(byte) 0x5f,  (byte) 0x72,  (byte) 0x64,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x2e,  (byte) 0x30,  (byte) 0x30,  (byte) 0x3a,  (byte) 0x30,  (byte) 0x2e,  (byte) 0x30,  (byte) 0x30,  (byte) 0x20,  (byte) 0x6d,  (byte) 0x69,  
			(byte) 0x78,  (byte) 0x65,  (byte) 0x64,  (byte) 0x5f,  (byte) 0x72,  (byte) 0x65,  (byte) 0x66,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x6d,  (byte) 0x65,  (byte) 0x5f,  (byte) 0x72,  (byte) 0x61,  (byte) 0x6e,  
			(byte) 0x67,  (byte) 0x65,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x36,  (byte) 0x20,  (byte) 0x63,  (byte) 0x68,  (byte) 0x72,  (byte) 0x6f,  (byte) 0x6d,  (byte) 0x61,  (byte) 0x5f,  (byte) 0x6d,  (byte) 0x65,  (byte) 0x3d,  
			(byte) 0x31,  (byte) 0x20,  (byte) 0x74,  (byte) 0x72,  (byte) 0x65,  (byte) 0x6c,  (byte) 0x6c,  (byte) 0x69,  (byte) 0x73,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x38,  (byte) 0x78,  (byte) 0x38,  (byte) 0x64,  
			(byte) 0x63,  (byte) 0x74,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x63,  (byte) 0x71,  (byte) 0x6d,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x64,  (byte) 0x65,  (byte) 0x61,  (byte) 0x64,  (byte) 0x7a,  
			(byte) 0x6f,  (byte) 0x6e,  (byte) 0x65,  (byte) 0x3d,  (byte) 0x32,  (byte) 0x31,  (byte) 0x2c,  (byte) 0x31,  (byte) 0x31,  (byte) 0x20,  (byte) 0x66,  (byte) 0x61,  (byte) 0x73,  (byte) 0x74,  (byte) 0x5f,  (byte) 0x70,  
			(byte) 0x73,  (byte) 0x6b,  (byte) 0x69,  (byte) 0x70,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x20,  (byte) 0x63,  (byte) 0x68,  (byte) 0x72,  (byte) 0x6f,  (byte) 0x6d,  (byte) 0x61,  (byte) 0x5f,  (byte) 0x71,  (byte) 0x70,  
			(byte) 0x5f,  (byte) 0x6f,  (byte) 0x66,  (byte) 0x66,  (byte) 0x73,  (byte) 0x65,  (byte) 0x74,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x74,  (byte) 0x68,  (byte) 0x72,  (byte) 0x65,  (byte) 0x61,  (byte) 0x64,  
			(byte) 0x73,  (byte) 0x3d,  (byte) 0x32,  (byte) 0x20,  (byte) 0x6c,  (byte) 0x6f,  (byte) 0x6f,  (byte) 0x6b,  (byte) 0x61,  (byte) 0x68,  (byte) 0x65,  (byte) 0x61,  (byte) 0x64,  (byte) 0x5f,  (byte) 0x74,  (byte) 0x68,  
			(byte) 0x72,  (byte) 0x65,  (byte) 0x61,  (byte) 0x64,  (byte) 0x73,  (byte) 0x3d,  (byte) 0x32,  (byte) 0x20,  (byte) 0x73,  (byte) 0x6c,  (byte) 0x69,  (byte) 0x63,  (byte) 0x65,  (byte) 0x64,  (byte) 0x5f,  (byte) 0x74,  
			(byte) 0x68,  (byte) 0x72,  (byte) 0x65,  (byte) 0x61,  (byte) 0x64,  (byte) 0x73,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x20,  (byte) 0x73,  (byte) 0x6c,  (byte) 0x69,  (byte) 0x63,  (byte) 0x65,  (byte) 0x73,  (byte) 0x3d,  
			(byte) 0x32,  (byte) 0x20,  (byte) 0x6e,  (byte) 0x72,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x64,  (byte) 0x65,  (byte) 0x63,  (byte) 0x69,  (byte) 0x6d,  (byte) 0x61,  (byte) 0x74,  (byte) 0x65,  (byte) 0x3d,  
			(byte) 0x31,  (byte) 0x20,  (byte) 0x69,  (byte) 0x6e,  (byte) 0x74,  (byte) 0x65,  (byte) 0x72,  (byte) 0x6c,  (byte) 0x61,  (byte) 0x63,  (byte) 0x65,  (byte) 0x64,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x62,  
			(byte) 0x6c,  (byte) 0x75,  (byte) 0x72,  (byte) 0x61,  (byte) 0x79,  (byte) 0x5f,  (byte) 0x63,  (byte) 0x6f,  (byte) 0x6d,  (byte) 0x70,  (byte) 0x61,  (byte) 0x74,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x63,  
			(byte) 0x6f,  (byte) 0x6e,  (byte) 0x73,  (byte) 0x74,  (byte) 0x72,  (byte) 0x61,  (byte) 0x69,  (byte) 0x6e,  (byte) 0x65,  (byte) 0x64,  (byte) 0x5f,  (byte) 0x69,  (byte) 0x6e,  (byte) 0x74,  (byte) 0x72,  (byte) 0x61,  
			(byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x62,  (byte) 0x66,  (byte) 0x72,  (byte) 0x61,  (byte) 0x6d,  (byte) 0x65,  (byte) 0x73,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x77,  (byte) 0x65,  (byte) 0x69,  
			(byte) 0x67,  (byte) 0x68,  (byte) 0x74,  (byte) 0x70,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x20,  (byte) 0x6b,  (byte) 0x65,  (byte) 0x79,  (byte) 0x69,  (byte) 0x6e,  (byte) 0x74,  (byte) 0x3d,  (byte) 0x69,  (byte) 0x6e,  
			(byte) 0x66,  (byte) 0x69,  (byte) 0x6e,  (byte) 0x69,  (byte) 0x74,  (byte) 0x65,  (byte) 0x20,  (byte) 0x6b,  (byte) 0x65,  (byte) 0x79,  (byte) 0x69,  (byte) 0x6e,  (byte) 0x74,  (byte) 0x5f,  (byte) 0x6d,  (byte) 0x69,  
			(byte) 0x6e,  (byte) 0x3d,  (byte) 0x35,  (byte) 0x33,  (byte) 0x36,  (byte) 0x38,  (byte) 0x37,  (byte) 0x30,  (byte) 0x39,  (byte) 0x31,  (byte) 0x33,  (byte) 0x20,  (byte) 0x73,  (byte) 0x63,  (byte) 0x65,  (byte) 0x6e,  
			(byte) 0x65,  (byte) 0x63,  (byte) 0x75,  (byte) 0x74,  (byte) 0x3d,  (byte) 0x34,  (byte) 0x30,  (byte) 0x20,  (byte) 0x69,  (byte) 0x6e,  (byte) 0x74,  (byte) 0x72,  (byte) 0x61,  (byte) 0x5f,  (byte) 0x72,  (byte) 0x65,  
			(byte) 0x66,  (byte) 0x72,  (byte) 0x65,  (byte) 0x73,  (byte) 0x68,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x72,  (byte) 0x63,  (byte) 0x5f,  (byte) 0x6c,  (byte) 0x6f,  (byte) 0x6f,  (byte) 0x6b,  (byte) 0x61,  
			(byte) 0x68,  (byte) 0x65,  (byte) 0x61,  (byte) 0x64,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x72,  (byte) 0x63,  (byte) 0x3d,  (byte) 0x63,  (byte) 0x72,  (byte) 0x66,  (byte) 0x20,  (byte) 0x6d,  (byte) 0x62,  
			(byte) 0x74,  (byte) 0x72,  (byte) 0x65,  (byte) 0x65,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x63,  (byte) 0x72,  (byte) 0x66,  (byte) 0x3d,  (byte) 0x32,  (byte) 0x33,  (byte) 0x2e,  (byte) 0x30,  (byte) 0x20,  
			(byte) 0x71,  (byte) 0x63,  (byte) 0x6f,  (byte) 0x6d,  (byte) 0x70,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x2e,  (byte) 0x36,  (byte) 0x30,  (byte) 0x20,  (byte) 0x71,  (byte) 0x70,  (byte) 0x6d,  (byte) 0x69,  (byte) 0x6e,  
			(byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x71,  (byte) 0x70,  (byte) 0x6d,  (byte) 0x61,  (byte) 0x78,  (byte) 0x3d,  (byte) 0x36,  (byte) 0x39,  (byte) 0x20,  (byte) 0x71,  (byte) 0x70,  (byte) 0x73,  (byte) 0x74,  
			(byte) 0x65,  (byte) 0x70,  (byte) 0x3d,  (byte) 0x34,  (byte) 0x20,  (byte) 0x76,  (byte) 0x62,  (byte) 0x76,  (byte) 0x5f,  (byte) 0x6d,  (byte) 0x61,  (byte) 0x78,  (byte) 0x72,  (byte) 0x61,  (byte) 0x74,  (byte) 0x65,  
			(byte) 0x3d,  (byte) 0x32,  (byte) 0x35,  (byte) 0x30,  (byte) 0x20,  (byte) 0x76,  (byte) 0x62,  (byte) 0x76,  (byte) 0x5f,  (byte) 0x62,  (byte) 0x75,  (byte) 0x66,  (byte) 0x73,  (byte) 0x69,  (byte) 0x7a,  (byte) 0x65,  
			(byte) 0x3d,  (byte) 0x32,  (byte) 0x37,  (byte) 0x32,  (byte) 0x20,  (byte) 0x63,  (byte) 0x72,  (byte) 0x66,  (byte) 0x5f,  (byte) 0x6d,  (byte) 0x61,  (byte) 0x78,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x2e,  (byte) 0x30,  
			(byte) 0x20,  (byte) 0x6e,  (byte) 0x61,  (byte) 0x6c,  (byte) 0x5f,  (byte) 0x68,  (byte) 0x72,  (byte) 0x64,  (byte) 0x3d,  (byte) 0x6e,  (byte) 0x6f,  (byte) 0x6e,  (byte) 0x65,  (byte) 0x20,  (byte) 0x66,  (byte) 0x69,  
			(byte) 0x6c,  (byte) 0x6c,  (byte) 0x65,  (byte) 0x72,  (byte) 0x3d,  (byte) 0x30,  (byte) 0x20,  (byte) 0x69,  (byte) 0x70,  (byte) 0x5f,  (byte) 0x72,  (byte) 0x61,  (byte) 0x74,  (byte) 0x69,  (byte) 0x6f,  (byte) 0x3d,  
			(byte) 0x31,  (byte) 0x2e,  (byte) 0x34,  (byte) 0x30,  (byte) 0x20,  (byte) 0x61,  (byte) 0x71,  (byte) 0x3d,  (byte) 0x31,  (byte) 0x3a,  (byte) 0x31,  (byte) 0x2e,  (byte) 0x30,  (byte) 0x30,  (byte) 0x00,  (byte) 0x80

	};
	private static final byte[] start_code = new byte[]{
		0x00, 0x00, 0x01
	};
	private static final byte[] fourx_zero_bytes = new byte[]{
		0x00, 0x00, 0x00, 0x00
	};
	
	public TCPGameStream(InetSocketAddress isa, ByteString byteString){
		this.isa = isa;
		this.authToken = byteString;
	}
	
	@Override
	public void run() {
		try {
			socket = new DatagramSocket();
			socket.connect(isa);
			System.out.println(socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
			connected = true;
			Arrays.fill(channelAssignments, -1);
			writeOpenConnectionMessage();
			setupLogging();
			processLoop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	private void processLoop() throws IOException {
		byte[] byteBuf = new byte[8192];
		DatagramPacket packet = new DatagramPacket(byteBuf, byteBuf.length);
		while(connected) {
			socket.receive(packet);
			//loggingOut.write("START".getBytes(), 0, "START".getBytes().length);
			//loggingOut.write(byteBuf, 0, packet.getLength());
			//loggingOut.write("STOP".getBytes(), 0, "STOP".getBytes().length);
			loggingOut.flush();
			//System.out.println(Hex.toHexString(byteBuf, 0, packet.getLength()));
			int packetType = byteBuf[0];
			switch(packetType) {
				case PACKET_TYPE_CONNECT_RESPONSE:
					//System.out.println("Connect response");
					receiverId = byteBuf[2] & 0xff;
					sendAuthMessage();
					break;
				case PACKET_TYPE_CONTROL:
					//System.out.println("Control");
					if (byteBuf[5] != 0) { //split packet
						//System.out.println(byteBuf[7] + ": " + Hex.toHexString(byteBuf));
						splitPacketBuffer.reset();
						splitPacketBuffer.write(byteBuf, 0, packet.getLength());
						splitPacketRemaining = byteBuf[5];
						break;
					}
					//System.out.println(byteBuf[7] + ": " + Hex.toHexString(byteBuf));
					processControlMessage(byteBuf, 0, packet.getLength());
					break;
				case PACKET_TYPE_CONTROL_CONTINUED:
					//System.out.println("Control cont.");
					//System.out.println(Hex.toHexString(byteBuf));
					int channel = byteBuf[4];
					if (channel >= Stream.EStreamChannel.k_EStreamChannelDataChannelStart_VALUE) {
						splitPacketBuffer.write(byteBuf, 0, packet.getLength());
						System.out.println("not cutting");
					} else{
						splitPacketBuffer.write(byteBuf, 13, packet.getLength() - 13);
					}
					splitPacketRemaining--;
					if (splitPacketRemaining == 0) {
						byte[] fullBuf = splitPacketBuffer.toByteArray();
						splitPacketBuffer.reset();
						processControlMessage(fullBuf, 0, fullBuf.length);
					}
					break;
				case PACKET_TYPE_DATA:
					System.out.println("Data!");
					break;
				case PACKET_TYPE_DISCONNECT:
					System.out.println("disconnected");
					connected = false;
					break;
				default:
					break;
				}
			}
		}
		
		private void sendAuthMessage() throws IOException {
			Stream.CAuthenticationRequestMsg authMsg = Stream.CAuthenticationRequestMsg.newBuilder().
				setVersion(Stream.EStreamVersion.k_EStreamVersionCurrent).
				setToken(authToken).
				build();
			writeControlMessage(authMsg);
		}
		
		private void writeControlMessage(GeneratedMessage msg) throws IOException {
			int escmsg = EStreamControlMessageMap.getByClass(msg.getClass());
			byte[] serialized = msg.toByteArray();
			byte[] withId = new byte[serialized.length + 1];
			withId[0] = (byte) escmsg;
			System.arraycopy(serialized, 0, withId, 1, serialized.length);
			writeRawMessage(PACKET_TYPE_CONTROL, Stream.EStreamChannel.k_EStreamChannelControl_VALUE, withId);
		}
		
		private void writeRawMessage(int packetType, int dataChannel, byte[] payload) throws IOException {
			// todo: splitting oversized packets.
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			dos.write(packetType);
			dos.write(0); //repeat count: todo
			dos.write(senderId);
			dos.write(receiverId);
			dos.write(dataChannel); //dunno
			dos.writeShort(0); //dunno
			dos.writeShort(Short.reverseBytes(packetSequenceIds[packetType]));
			dos.writeInt(Integer.reverseBytes(getTimestamp()));
			dos.write(payload);
			byte[] output = bos.toByteArray();
			DatagramPacket packet = new DatagramPacket(output, output.length);
			socket.send(packet);
			packetSequenceIds[packetType]++;
		}
		
		private int getTimestamp() {
			return (int) System.currentTimeMillis();
		}
		
		private void processControlMessage(byte[] buffer, int begin, int length) throws IOException {
			//System.out.println("Control message");
			sendControlAckMessage(buffer, begin);
			int channel = buffer[begin+4];
			if (channel >= Stream.EStreamChannel.k_EStreamChannelDataChannelStart_VALUE) {
				int channelAssignment = channelAssignments[channel];
				byte[] outmsg = new byte[4];
				System.arraycopy(buffer, begin + 16, outmsg, 0, 4);
				//System.out.println("Acking " + Hex.toHexString(outmsg));
				writeRawMessage(PACKET_TYPE_CONTROL_ACKNOWLEDGE, Stream.EStreamChannel.k_EStreamChannelControl_VALUE, outmsg);
				switch (channelAssignment) {
					case Stream.EStreamingDataType.k_EStreamingAudioData_VALUE:
						processAudioControlMessage(buffer, begin, length);
						break;
					case Stream.EStreamingDataType.k_EStreamingVideoData_VALUE:
						processVideoControlMessage(buffer, begin, length);
						break;
					default:
						break;
				}
				return;
			}
			GeneratedMessage msg = readControlMessage(buffer, begin, length);
			if(msg != null){
				Main.printBody(msg);
			}
			if (msg instanceof Stream.CNegotiationInitMsg) {
				Stream.CNegotiationInitMsg initMsg = (Stream.CNegotiationInitMsg) msg;
				Stream.CNegotiationSetConfigMsg setconfigMsg = Stream.CNegotiationSetConfigMsg.newBuilder().
					setConfig(Stream.CNegotiatedConfig.newBuilder().
						setReliableData(false).
						setSelectedAudioCodec(Stream.EStreamAudioCodec.k_EStreamAudioCodecVorbis).
						setSelectedVideoCodec(Stream.EStreamVideoCodec.k_EStreamVideoCodecH264).
						addAvailableVideoModes(Stream.CStreamVideoMode.newBuilder().
							setWidth(320).
							setHeight(240)
						)
						// todo: un-hardcode
					).
					build();
				writeControlMessage(setconfigMsg);
			} else if (msg instanceof Stream.CNegotiationSetConfigMsg) {
				writeControlMessage(Stream.CNegotiationCompleteMsg.getDefaultInstance());
			} else if (msg instanceof Stream.CStartAudioDataMsg) {
				Stream.CStartAudioDataMsg startMsg = (Stream.CStartAudioDataMsg) msg;
				channelAssignments[startMsg.getChannel()] = Stream.EStreamingDataType.k_EStreamingAudioData_VALUE;
			} else if (msg instanceof Stream.CStartVideoDataMsg) {
				Stream.CStartVideoDataMsg startMsg = (Stream.CStartVideoDataMsg) msg;
				Main.printBody(startMsg);
				channelAssignments[startMsg.getChannel()] = Stream.EStreamingDataType.k_EStreamingVideoData_VALUE;
			} else if (msg instanceof Stream.CVideoEncoderInfoMsg){
				Stream.CVideoEncoderInfoMsg startMsg = (CVideoEncoderInfoMsg) msg;
				//Main.printBody(startMsg);
			}
		}
		
		private void sendControlAckMessage(byte[] originalMessage, int offset) throws IOException {
			byte[] outmsg = new byte[4];
			System.arraycopy(originalMessage, offset + 9, outmsg, 0, 4);
			byte channel = originalMessage[offset+4];
			writeRawMessage(PACKET_TYPE_CONTROL_ACKNOWLEDGE, channel, outmsg);
		}
		
		private void processAudioControlMessage(byte[] msg, int offset, int length) throws IOException {
			if (msg[offset+13] != 0x1) return;
			System.out.println("Writing audio control message");
			audioOut.write(msg, offset+30, length-30);
		}
		
		private void processVideoControlMessage(byte[] msg, int offset, int length) throws IOException {
			String message_string = Hex.toHexString(msg);
			int start_pos = message_string.indexOf("000000");//Bytes.indexOf(msg, start_code);
			boolean found = false;
			int check_pos = 0;
			boolean notfound = false;
			if(start_pos != -1){
				while (!found){
					check_pos = message_string.indexOf("000000", check_pos + 1);
					//System.out.println(check_pos);
					if(check_pos/2+5 >= length){
						//System.out.println("start_pos: " + start_pos);
						//System.out.println("Pos: " + check_pos/2+5);
						//System.out.println("Length: " + length);
						found = true;
						notfound = true;
					}
					if(msg[check_pos/2+1 + 4] == 0x01){
						//System.out.println("found");
						//System.out.println(check_pos/2+1);
						check_pos = check_pos / 2 + 1;
						found = true;
					} else{
						//System.out.println("Not found yet: " + check_pos);
					}
				}
			} else{
				//return;
			}
			//System.out.println(message_string);
			//System.out.println(length);
			
			//System.out.println("Check_pos: " + check_pos);
			//System.out.println("Start: " + (offset +check_pos));
			//System.out.println("Stop: " + (length - check_pos));
			
			//loggingOut.write("PACKET_START".getBytes());
			//loggingFull.write("PACKET_START".getBytes());
			if(notfound){
				//System.out.println("Not found");
				//loggingOut.write("START_NOT_FOUND".getBytes());
				//loggingFull.write("START_NOT_FOUND".getBytes());
				//System.out.println(start_pos);
				//loggingOut.write("NO_NAL_START".getBytes());
				//loggingFull.write("NO_NAL_START".getBytes());
				loggingOut.write(msg, offset + 33, length - 33);	//USE 33
				loggingFull.write(msg, 33, length - 33);
				//loggingOut.write("NO_NAL_STOP".getBytes());
				//loggingFull.write("NO_NAL_STOP".getBytes());
			} else{
				System.out.println("Check_pos: " + check_pos);
				loggingOut.write(msg, offset + check_pos, length - check_pos);
				loggingFull.write(msg, check_pos, length + check_pos);
			}
			//loggingOut.write("PACKET_STOP".getBytes());
			//loggingFull.write("PACKET_STOP".getBytes());
			if(notfound){
				//loggingOut.write("STOP_NOT_FOUND".getBytes());
				//loggingFull.write("STOP_NOT_FOUND".getBytes());

			}
			
			//loggingOut.write(msg, offset + 33, length - 33);
			//loggingFull.write(msg, 33, length - 33);
			

		}
		
		private GeneratedMessage readControlMessage(byte[] buffer, int beginOfBuffer, int lengthOfBuffer) {
			int begin = beginOfBuffer + 13;
			int length = lengthOfBuffer - 13;
			int escmsg = buffer[begin] & 0xff;
			Class<? extends GeneratedMessage> clazz = EStreamControlMessageMap.getById(escmsg);
			//System.out.println(Integer.toString(escmsg, 16) + ":" + clazz);
			byte[] messageBytes = new byte[length - 1];
			System.arraycopy(buffer, begin + 1, messageBytes, 0, length - 1);
			try {
				return (GeneratedMessage) clazz.getMethod("parseFrom", byte[].class).invoke(null, messageBytes);
			} catch (Exception e) {
				System.out.println(escmsg);
				e.printStackTrace();
				return null;
			}

		}
		
		private void setupLogging() throws IOException {
			loggingOut = new FileOutputStream(new File("streaminglog4.dat"));
			loggingFull = new FileOutputStream(new File("streaminglog4full.dat"));
			audioOut = new FileOutputStream(new File("audiolog.dat"));
		}
		
		private void writeOpenConnectionMessage() throws IOException {
			writeRawMessage(PACKET_TYPE_CONNECT, Stream.EStreamChannel.k_EStreamChannelDiscovery_VALUE, new byte[]{});
		}

}