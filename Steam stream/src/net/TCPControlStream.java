package net;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.crypto.tls.AlertLevel;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.PSKTlsClient;
import org.bouncycastle.crypto.tls.ServerOnlyTlsAuthentication;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.bouncycastle.crypto.tls.TlsPSKIdentity;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;


public class TCPControlStream implements Runnable{
	
	private DataOutputStream output = null;
	private DataInputStream input = null;
	
	private static byte[] magicBytes = "VT01".getBytes(Charset.forName("UTF-8"));
	
	static String convertStreamToString(InputStream is) {
		Scanner s = new Scanner(is).useDelimiter("\\A");
		if(s.hasNext()){
			return s.next();
		} else{
			return "";
		}
	}
	
	@Override
	public void run() {
		try{
			Z_PSKIdentity pskIdentity = new Z_PSKIdentity();
	
	        Security.addProvider(new BouncyCastleProvider());
	        
	        Socket socket = new Socket(InetAddress.getByName("192.168.1.66"), 27036);
	
	        SecureRandom secureRandom = new SecureRandom();
	        TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream(), secureRandom);
	
	        MyPSKTlsClient client = new MyPSKTlsClient(pskIdentity);
	        protocol.connect(client);
	
	        output = new DataOutputStream(protocol.getOutputStream());
	        //output.write("GET / HTTP/1.1\r\n\r\n".getBytes("UTF-8"));
	        
	        input = new DataInputStream(protocol.getInputStream());
	        boolean running = true;
	        while(running){
	        	readPacket(input);
	        }
	        //System.out.println(convertStreamToString(input));
	
	        protocol.close();
	        socket.close();
		} catch(IOException e){
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readPacket(InputStream input) throws Exception{
		readMessage(new DataInputStream(input));
	}
	
	public GeneratedMessage readMessage(DataInputStream in) throws IOException{
		int length = 0;
		int emsg = 0;
		try {
			length = Integer.reverseBytes(in.readInt());
		int magic = Integer.reverseBytes(in.readInt());
		emsg = in.readInt();
		//System.out.println("emsg: " + emsg);
		emsg = Integer.reverseBytes(emsg);
		int empty = Integer.reverseBytes(in.readInt()); // according to SteamKit this is the length of legacy header. Always 0.
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Class<? extends GeneratedMessage> clazz = EMsgRemoteClient.getById(emsg & 0x7fffffff);
		byte[] messageBytes = new byte[length - 8];
		try {
			in.read(messageBytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		GeneratedMessage message = null;
		//fSystem.out.println(emsg & 0x7fffffff);
		try{
			switch(emsg & 0x7fffffff){
			case 9500:
				message = SteammessagesRemoteclient.CMsgRemoteClientAuth.parseFrom(messageBytes);
				System.out.println("Sending auth response");
				sendAuth((SteammessagesRemoteclient.CMsgRemoteClientAuth) message);
				break;
			case 9501:
				message = SteammessagesRemoteclient.CMsgRemoteClientAuthResponse.parseFrom(messageBytes);
				sendAuthResponse();
				break;
			case 9502:
				message = SteammessagesRemoteclient.CMsgRemoteClientAppStatus.parseFrom(messageBytes);
				//Main.printBody(message);
				break;
			case 9503:
				message = SteammessagesRemoteclient.CMsgRemoteClientStartStream.parseFrom(messageBytes);
				break;
			case 9504:
				message = SteammessagesRemoteclient.CMsgRemoteClientStartStreamResponse.parseFrom(messageBytes);
				//Main.printBody(message);
				if(((SteammessagesRemoteclient.CMsgRemoteClientStartStreamResponse) message).getELaunchResult() == 1){
					Main.printBody(message);
					new Thread(new TCPGameStream(new InetSocketAddress("192.168.1.66", ((SteammessagesRemoteclient.CMsgRemoteClientStartStreamResponse) message).getStreamPort()), ((SteammessagesRemoteclient.CMsgRemoteClientStartStreamResponse) message).getAuthToken())).start();
				}
				break;
			case 9505:
				message = SteammessagesRemoteclient.CMsgRemoteClientPing.parseFrom(messageBytes);
				sendPingResponse();
				break;
			case 9506:
				message = SteammessagesRemoteclient.CMsgRemoteClientPingResponse.parseFrom(messageBytes);
				break;
				
			}
		} catch (InvalidProtocolBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//return (GeneratedMessage) clazz.getMethod("parseFrom", byte[].class).invoke(null, messageBytes);
		return null;
	}
	
	static class MyPSKTlsClient extends PSKTlsClient{
		public MyPSKTlsClient(TlsPSKIdentity id){
			super(id);
		}
		
		@Override
		public int[] getCipherSuites(){
			return new int[]{
					CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA
			};
		}

	    public void notifyAlertRaised(short alertLevel, short alertDescription, String message, Exception cause){
            PrintStream out = (alertLevel == AlertLevel.fatal) ? System.err : System.out;
            out.println("TLS client raised alert (AlertLevel." + alertLevel + ", AlertDescription." + alertDescription + ")");
            if (message != null) {
                out.println(message);
            }
            if (cause != null) {
                cause.printStackTrace(out);
            }
        }

        public void notifyAlertReceived(short alertLevel, short alertDescription){
            PrintStream out = (alertLevel == AlertLevel.fatal) ? System.err : System.out;
            out.println("TLS client received alert (AlertLevel." + alertLevel + ", AlertDescription." + alertDescription + ")");
        }

        public TlsAuthentication getAuthentication() throws IOException{
            return new ServerOnlyTlsAuthentication(){
                public void notifyServerCertificate(org.bouncycastle.crypto.tls.Certificate serverCertificate) throws IOException{
                    System.out.println("in getAuthentication");
                }
            };
	   }
	}
	
	private void sendAuth(SteammessagesRemoteclient.CMsgRemoteClientAuth message) throws IOException{
		message = message.newBuilder(message).setStatus(message.getStatus().newBuilder(message.getStatus()).setHostname("3rd Party Client").build()).build();
		byte[] message_bytes = message.toByteArray();
		int length = message_bytes.length + 8;
		output.writeInt(Integer.reverseBytes(length));
		output.write(magicBytes);
		int emsg = Integer.reverseBytes(9500 | 0x80000000);
		output.writeInt(emsg);
		output.writeInt(0);
		output.write(message_bytes);
	}
	
	private void sendAuthResponse() throws IOException{
		SteammessagesRemoteclient.CMsgRemoteClientAuthResponse message = SteammessagesRemoteclient.CMsgRemoteClientAuthResponse.newBuilder().setEresult(1).build();
		byte[] message_bytes = message.toByteArray();
		int length = message_bytes.length + 8;
		output.writeInt(Integer.reverseBytes(length));
		output.write(magicBytes);
		int emsg = Integer.reverseBytes(9501 | 0x80000000);
		output.writeInt(emsg);
		output.writeInt(0);
		output.write(message_bytes);
	}
	
	private void sendPingResponse() throws IOException{
		SteammessagesRemoteclient.CMsgRemoteClientPingResponse message = SteammessagesRemoteclient.CMsgRemoteClientPingResponse.newBuilder().build();
		byte[] message_bytes = message.toByteArray();
		int length = message_bytes.length + 8;
		output.writeInt(Integer.reverseBytes(length));
		output.write(magicBytes);
		int emsg = Integer.reverseBytes(9506 | 0x80000000);
		output.writeInt(emsg);
		output.writeInt(0);
		output.write(message_bytes);
	}
	
	public void startStream(int app_id) throws IOException{
		SteammessagesRemoteclient.CMsgRemoteClientStartStream message = SteammessagesRemoteclient.CMsgRemoteClientStartStream.newBuilder().setAppId(app_id).setGamepadCount(0).setLaunchOption(1).setLockParentalLock(false).setMaximumResolutionX(320).setMaximumResolutionY(240).build();
		byte[] message_bytes = message.toByteArray();
		int length = message_bytes.length + 8;
		output.writeInt(Integer.reverseBytes(length));
		output.write(magicBytes);
		int emsg = Integer.reverseBytes(9503 | 0x80000000);
		output.writeInt(emsg);
		output.writeInt(0);
		output.write(message_bytes);
	}
	
	static class Z_PSKIdentity implements TlsPSKIdentity {

		  public Z_PSKIdentity(){};
		  @Override
		  public void skipIdentityHint(){
		         System.out.println("skipIdentityHint called\n");
		  }
		  @Override
		  public void notifyIdentityHint(byte[] PSK_identity_hint){
		         System.out.println("notifyIdentityHint called");
		  }
		  @Override
		  public byte[] getPSKIdentity(){
			  return "steam".getBytes();
		  }
		  @Override
		  public byte[] getPSK(){
		   return DatatypeConverter.parseHexBinary("9a57f5736f62971a6731e9bf815f8eedaf4cfb6f2fcb369babc2a0e8d4fe4bef");
		  }

		 }


}
