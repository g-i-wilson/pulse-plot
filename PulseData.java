import java.math.BigInteger;
import java.util.*;
import java.time.LocalDateTime;

public class PulseData {

	private LocalDateTime		timeReceived;	
	private byte[] 					data;
	private int							version;
	private int							captureId;
	private int							count;
	private int							actualCount;
	private long						timestamp;
	private List<Integer>		samples;

  private static int intLittleEndian( byte[] buf, int start, int end ) {
  	//System.out.println( start+", "+end );
  	int leftShift = 0;
  	int total = 0;
  	for (int i=start; i<end; i++) {
			total += ((int)buf[i]) << leftShift;
  		//System.out.println( "index:"+i+", value:"+buf[i]+" << "+leftShift+" --> total:"+total );
			leftShift += 8;
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
  
  public PulseData ( String hexStr ) {
  	this( strToBytes( hexStr ) );
  }
  
  public PulseData ( byte[] byteArray ) {
  	timeReceived	= LocalDateTime.now();
  	data 					=	byteArray;
  	//System.out.println( "length: "+data.length );
  	version 			=	intLittleEndian( data, 0, 2 );
  	captureId			=	intLittleEndian( data, 2, 4 );
  	count					=	intLittleEndian( data, 4, 8 );
  	timestamp			= ((long)intLittleEndian( data, 8, 12 )) + (((long)intLittleEndian( data, 12, 16 )) << 32);
  	samples				= new ArrayList<Integer>();
  	actualCount 	= 0;
  	
  	for (int i=16; i<data.length; i+=2) {
  		samples.add( intLittleEndian( data, i, i+2 ) );
  		actualCount++;
  	}
  	
	}
	
	public LocalDateTime timeReceived () {
		return timeReceived;
	}
	
	public byte[] data () {
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
	
		PulseData pd = new PulseData( "000001000400000001000000000000000100100000010010" );
		System.out.println( pd );
	}

}