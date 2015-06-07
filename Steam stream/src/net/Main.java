package net;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;

public class Main implements Runnable{
	
	public static final byte[] PACKET_PREHEADER = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x21, 0x4c, 0x5f, (byte) 0xa0};
	DatagramSocket socket;
	
	public static void main(String args[]){
		//new Thread(new TCPStream()).start();
		new Thread(new Main()).start();
	}
	
	@Override
	public void run() {
		try {
			socket = new DatagramSocket(27036);
			byte[] buf = new byte[8192];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			sendDiscoveryPacket();
			while(true) {
				socket.receive(packet);
				SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastHeader header = handlePacket(packet);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private boolean called = false;
	private SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastHeader handlePacket(DatagramPacket packet) throws IOException {
			ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
			DataInputStream dis = new DataInputStream(bis);
			byte[] preheaderBytes = new byte[PACKET_PREHEADER.length];
			dis.read(preheaderBytes);
			if (!Arrays.equals(PACKET_PREHEADER, preheaderBytes)) {
				return null;
			}
			int headerLength = Integer.reverseBytes(dis.readInt());
			byte[] headerBytes = new byte[headerLength];
			dis.read(headerBytes);
			
			SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastHeader header = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastHeader.parseFrom(headerBytes);
			System.out.println(packet.getAddress().toString() + ": " + header.getMsgType());
			
			int bodyLength = Integer.reverseBytes(dis.readInt());
			byte[] bodyBytes = new byte[bodyLength];
			dis.read(bodyBytes);
			
			GeneratedMessage body = null;
			
			Thread tcp = null;
			
			switch(header.getMsgType()){
				case k_ERemoteClientBroadcastMsgDiscovery:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastDiscovery.parseFrom(bodyBytes);
					if(!(packet.getSocketAddress().equals(new InetSocketAddress("192.168.1.35", 27036)))){
						sendDiscoveryPacket();
						sendStatusPacket(packet.getAddress());
					}
					break;
				case k_ERemoteClientBroadcastMsgStatus:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastStatus.parseFrom(bodyBytes);
					if(!packet.getAddress().equals(new InetSocketAddress("192.168.1.35", 27036).getAddress()) && !called){
						called = true;
						TCPControlStream stream = new TCPControlStream();
						new Thread(stream).start();
						new Thread(new GameLaunch(stream)).start();
					}
					break;
				case k_ERemoteClientBroadcastMsgOffline:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastDiscovery.parseFrom(bodyBytes);
					break;
				case k_ERemoteDeviceAuthorizationRequest:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteDeviceAuthorizationRequest.parseFrom(bodyBytes);
					break;
				case k_ERemoteDeviceAuthorizationResponse:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastDiscovery.parseFrom(bodyBytes);
					break;
				case k_ERemoteDeviceStreamingRequest:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteDeviceStreamingRequest.parseFrom(bodyBytes);
					break;
				case k_ERemoteDeviceStreamingResponse:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteDeviceStreamingResponse.parseFrom(bodyBytes);
					break;
				case k_ERemoteDeviceProofRequest:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteDeviceProofRequest.parseFrom(bodyBytes);
					break;
				case k_ERemoteDeviceProofResponse:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteDeviceProofResponse.parseFrom(bodyBytes);
					break;
				case k_ERemoteDeviceAuthorizationCancelRequest:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteDeviceAuthorizationCancelRequest.parseFrom(bodyBytes);
					break;
				case k_ERemoteDeviceStreamingCancelRequest:
					body = SteammessagesRemoteclientDiscovery.CMsgRemoteDeviceStreamingCancelRequest.parseFrom(bodyBytes);
					break;
			}
			
			//printBody(body);
			return header;
			
			/*if (header.getMsgType() != SteammessagesRemoteclientDiscovery.ERemoteClientBroadcastMsg.k_ERemoteClientBroadcastMsgStatus) return;
			int messageLength = Integer.reverseBytes(dis.readInt());
			byte[] messageBytes = new byte[messageLength];
			dis.read(messageBytes);
			SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastStatus message = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastStatus.parseFrom(messageBytes);
			listener.onResponse(packet.getSocketAddress(), header, message);*/
	}
	
	private void sendDiscoveryPacket() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.write(PACKET_PREHEADER);
		SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastHeader header = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastHeader.newBuilder().
			setClientId(12345678).
			setMsgType(SteammessagesRemoteclientDiscovery.ERemoteClientBroadcastMsg.k_ERemoteClientBroadcastMsgDiscovery).
			build();
		byte[] headerBytes = header.toByteArray();
		dos.writeInt(Integer.reverseBytes(headerBytes.length));
		dos.write(headerBytes);
		SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastDiscovery message = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastDiscovery.newBuilder().
			setSeqNum(0).
			build();
		byte[] messageBytes = message.toByteArray();
		dos.writeInt(Integer.reverseBytes(messageBytes.length));
		dos.write(messageBytes);
		byte[] buf = bos.toByteArray();

		DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress("255.255.255.255", 27036));
		socket.send(packet);
	}
	
	private void sendStatusPacket(InetAddress address) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.write(PACKET_PREHEADER);
		SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastHeader header = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastHeader.newBuilder().
			setClientId(12345678).
			setMsgType(SteammessagesRemoteclientDiscovery.ERemoteClientBroadcastMsg.k_ERemoteClientBroadcastMsgStatus).
			build();
		byte[] headerBytes = header.toByteArray();
		dos.writeInt(Integer.reverseBytes(headerBytes.length));
		dos.write(headerBytes);
		SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastStatus.User user = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastStatus.User.newBuilder().setSteamid(76561198033602994L).setAuthKeyId(10513310).build();
		SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastStatus message = SteammessagesRemoteclientDiscovery.CMsgRemoteClientBroadcastStatus.newBuilder().
			addUsers(user).setVersion(6).setMinVersion(6).setConnectPort(27036).setHostname(InetAddress.getLocalHost().getHostName()).setEnabledServices(2).setOstype(10).setIs64Bit(true).setEuniverse(1).setTimestamp((int) (System.currentTimeMillis() / 1000)).
			build();
		//printBody(message);
		byte[] messageBytes = message.toByteArray();
		dos.writeInt(Integer.reverseBytes(messageBytes.length));
		dos.write(messageBytes);
		byte[] buf = bos.toByteArray();
		
		InetSocketAddress isa = new InetSocketAddress(address, 27036);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, isa);
		socket.send(packet);
	}
	
	public static void printBody(GeneratedMessage body){
		Map<FieldDescriptor,Object> map = body.getAllFields();
		for(FieldDescriptor fd : map.keySet()){
			System.out.println(fd.getFullName() + ": " + map.get(fd));
		}
	}
		
	
}
