import java.math.BigInteger;
import java.util.*;
import java.time.LocalDateTime;
import bluegill.*;

public class PulseData {

	private LocalDateTime		timeReceived;	
	private byte[] 					data;
	private int							version;
	private int							captureId;
	private int							count;
	private int							actualCount;
	private long						timestamp;
	private List<Integer>		samples;
	private List<Double>		amplitude;
	private List<Double>		phase;
	private int 						duration;
	
	public static byte[] pack ( int version, int captureId, long timestamp, List<Integer> samples ) {
		byte[] bytes = new byte[ 2+2+4+8+2*samples.size() ];
		bytesLittleEndian( version, 				bytes, 0, 2 );
		bytesLittleEndian( captureId, 			bytes, 2, 4 );
		bytesLittleEndian( samples.size(), 	bytes, 4, 8 );
		bytesLittleEndian( timestamp,				bytes, 8, 16 );
		for (int i=0; i<samples.size(); i++) {
			bytesLittleEndian( samples.get(i).intValue(), bytes, i*2+16, i*2+18 );
		}
		return bytes;
	}
	
	private static void bytesLittleEndian( long l, byte[] buf, int start, int end ) {
		for (int i=start; i<end; i++) {
			buf[i] = (byte)(l & 0xff);
			l = l >> 8;
		}
	}
	
  private static int intLittleEndian( byte[] buf, int start, int end ) {
  	//System.out.println( start+", "+end );
  	int leftShift = 0;
  	int total = 0;
  	for (int i=start; i<end; i++) {
			total |= ((int)buf[i]) << leftShift;
  		//System.out.println( "index:"+i+", value:"+buf[i]+" << "+leftShift+" --> total:"+total );
			leftShift += 8;
		}
		if ((buf[end-1] & 0x80) == 0x80) { // negative value
			for (int i=end; i<start+4; i++) {
				total |= 0xff << leftShift;
				leftShift += 8;
			}
		}
		return total;
  }
  
  private static int charToNibble( int charVal ) {
  	if 				(charVal >= 48 && charVal <= 57) 		return charVal-48;
  	else if 	(charVal >= 65 && charVal <= 70) 		return charVal-55;
  	else if 	(charVal >= 97 && charVal <= 102) 	return charVal-87;
  	else 			return 0;
  }
  
  private static byte[] strToBytes( String hexStr ) {
  	byte[] charArray = hexStr.getBytes();
  	byte[] byteArray = new byte[charArray.length/2];
  	for (int i=0; i<byteArray.length; i++) {
  		byteArray[i] = (byte)( (charToNibble(charArray[i*2]) << 4) + charToNibble(charArray[i*2+1]) );
  		//System.out.println( "upper: "+charToNibble(charArray[i*2])+" << 4, lower:"+charToNibble(charArray[i*2+1])+", total:"+byteArray[i] );
  	}
  	//System.out.println();
  	return byteArray;
  }
  
  public PulseData ( String hexStr, double samplesPerCycle ) {
  	this( strToBytes( hexStr), samplesPerCycle );
  }
  
  public PulseData ( byte[] byteArray, double samplesPerCycle ) {
  	timeReceived	= LocalDateTime.now();
  	data 					=	byteArray;
  	//System.out.println( "length: "+data.length );
  	version 			=	intLittleEndian( data, 0, 2 );
  	captureId			=	intLittleEndian( data, 2, 4 );
  	count					=	intLittleEndian( data, 4, 8 );
  	timestamp			= ((long)intLittleEndian( data, 8, 12 )) + (((long)intLittleEndian( data, 12, 16 )) << 32);
  	samples				= new ArrayList<Integer>();
  	amplitude			= new ArrayList<Double>();
  	phase					= new ArrayList<Double>();
  	duration			= 0;
  	actualCount 	= 0;
  	
		QuadratureDemodulator qd = new QuadratureDemodulator( samplesPerCycle );
		Comparitor comp = new Comparitor( 100.0, 500.0 );
		
		int tempDuration = 0;
  	
  	for (int i=16; i<data.length; i+=2) {
  		int sample = intLittleEndian( data, i, i+2 );
  		samples.add( sample );
  		
  		qd.input( sample );
  		amplitude.add( qd.amplitude() );
  		phase.add( qd.phase() );
  		
  		comp.sample( qd.amplitude() );
  		if (comp.state() == 1) {
  			tempDuration += 1;
  		} else {
  			tempDuration = 0;
  		}
  		// duration will contain number samples of longest pulse in samples
  		if (tempDuration > duration) duration = tempDuration;
  		
  		actualCount++;
  	}
  	
	}
	
	public LocalDateTime timeReceived () {
		return timeReceived;
	}
	
	public byte[] packet () {
		return data;
	}
	
	public int version () {
		return version;
	}
	
	public int captureId () {
		return captureId;
	}
	
	public int count () {
		return count;
	}
	
	public int actualCount () {
		return actualCount;
	}
	
	public long timestamp () {
		return timestamp;
	}
	
	public List<Integer> samples () {
		return samples;
	}
	
	public List<Double> amplitude () {
		return amplitude;
	}
	
	public int duration () {
		return duration;
	}
	
	public String toString () {
		return
			"*** PulseData ***"+
			"\ntimeReceived: "+timeReceived+
			"\nversion:      "+version+
			"\ncaptureId:    "+captureId+
			"\ncount:        "+count+
			"\nactualCount:  "+actualCount+
			"\ntimestamp:    "+timestamp+
			"\nsamples:\n"+samples
		;
	}
	
	public static void main (String[] args) {
		byte[] testBytes = new byte[]{0x01, 0x00, 0x04, 0x00};
		System.out.println( "testing intLittleEndian: "+(intLittleEndian( testBytes, 0, 2 )+intLittleEndian( testBytes, 2, 4 )) );
	
		PulseData pd = new PulseData( "000001000400000001000000000000000100100000010010", 4 );
		System.out.println( pd );
		
		PulseData pd2 = new PulseData(
			PulseData.pack(
				0, 10, 100, new ArrayList<Integer>(Arrays.asList( -100,100,-100,100 ))
			),
			4
		);
		System.out.println( pd2 );
	}

}