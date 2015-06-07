package net;

import java.nio.ByteBuffer;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

public class PlayStream {

	public static void main(String[] args) {
		ByteBuffer bb = ByteBuffer.allocate(0); // Your frame data is stored in this buffer
		H264Decoder decoder = new H264Decoder();
		Picture out = Picture.create(1920, 1088, ColorSpace.YUV420); // Allocate output frame of max size
		//Picture real = decoder.decodeFrame(grab., out.getData());
		//BufferedImage bi = JCodecUtil.toBufferedImage(real); // If you prefere AWT image
		
	}

}
