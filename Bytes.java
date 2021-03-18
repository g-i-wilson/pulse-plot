import java.util.*;

public class Bytes {

	private byte[] bytes;
	
	
	// helper methods

  public static int charToNibble ( int charVal ) {
  	if 				(charVal >= 48 && charVal <= 57) 				return charVal-48;
  	else if 	(charVal >= 65 && charVal <= 70) 				return charVal-55;
  	else if 	(charVal >= 97 && charVal <= 102) 			return charVal-87;
  	else 																							return -1;
  }
  
  public static int nibbleToChar ( int nibbleVal ) {
  	if 				(nibbleVal >= 0   && nibbleVal <= 9) 		return nibbleVal+48;
  	else if		(nibbleVal >= 10  && nibbleVal <= 15) 	return nibbleVal+55;
  	else																							return -1;
  }
  
  public static String byteToStr ( byte byteVal ) {
		int lower = byteVal & 0x0f;
		int upper = (byteVal >> 4) & 0x0f;
		return new String( new byte[]{ (byte)nibbleToChar(upper), (byte)nibbleToChar(lower) } );
  }
  
  public static byte[] strToBytes ( String hexStr ) {
  	byte[] charArray = hexStr.getBytes();
  	List<Integer> charValues = new ArrayList<>();
  	for (byte b : charArray) {
  		if (charToNibble((int)b) != -1) charValues.add( (int)b );
  	}
  	byte[] byteArray = new byte[charValues.size()/2];
  	for (int i=0; i<byteArray.length; i++) {
  		int upper = charToNibble(charValues.get(i*2).intValue()) << 4;
  		int lower = charToNibble(charValues.get(i*2+1).intValue());
  		byteArray[i] = (byte)(upper + lower);
  	}
  	return byteArray;
  }
  
  
	// constructors  

	public Bytes ( byte[] bytes ) {
		this.bytes = bytes;
	}
	
	public Bytes ( String hexStr ) {
		this( strToBytes( hexStr ) );
	}
	
	
	// general
	
	public int size () {
		return bytes.length;
	}
	
	
	// little endian methods
	
	public Bytes writeLongLE ( long val, int start, int length ) {
		for (int i=start; i<Math.min(start+length,bytes.length); i++) {
			bytes[i] = (byte)( val & 0xff );
			val = val >> 8;
		}
		return this;
	}
	
	public Bytes writeIntLE ( int val, int start, int length ) {
		writeLongLE( (long)val, start, length );
		return this;
	}

	public Bytes writeShortLE ( short val, int start, int length ) {
		writeLongLE( (long)val, start, length );
		return this;
	}

	public long readLongLE ( int start, int length ) {
		long val;
		if ( (bytes[start+length-1] & 0x80) == 0x80 ) { // negative value
			val = -1; // all 1s
		} else {
			val = 0; // all 0s
		}
		for (int i=Math.min(start+length,bytes.length)-1; i>=start; i--) {
			val = val << 8; // create 8 0s, which may already be 0s
			val |= ((long)bytes[i] & 0xff); // when negative byte is cast to a long, it extends the sign bit all the way up... so must AND with 0x00000000000000ff
		}
		return val;
	}
	
	public int readIntLE ( int start, int length ) {
		return (int)readLongLE( start, length );
	}
	
	public short readShortLE ( int start, int length ) {
		return (short)readLongLE( start, length );
	}
	
	
	// hex String methods
	
	public String readHexBE ( int start, int length ) {
		String hex = "";
		for (int i=start; i<Math.min(start+length,bytes.length); i++) {
 			hex += byteToStr( bytes[i] );
		}
		return hex;
	}
	
	public String readHexLE ( int start, int length ) {
		String hex = "";
		for (int i=Math.min(start+length,bytes.length)-1; i>=start; i--) {
 			hex += byteToStr( bytes[i] );
		}
		return hex;
	}

	public Bytes writeHex ( String hexStr, int start ) {
		byte[] byteArray = strToBytes( hexStr );
		int j = 0;
		for (int i=start; i<Math.min(start+byteArray.length,bytes.length); i++) {
 			bytes[i] = byteArray[j++];
		}
		return this;
	}
	
	
	// toString
	
	public String toString () {
		return readHexBE( 0, bytes.length );
	}
	
	
	// test Bytes
	
	public static void main (String[] args) {
		Bytes b0 = new Bytes( "fe_ff | 00_01 | fe.FF.fF.Ff | 01,00,00,00" );
		
		short b0s0 = b0.readShortLE( 0, 2 );
		System.out.println( "short at 0: "+b0s0 );
		short b0s1 = b0.readShortLE( 2, 2 );
		System.out.println( "short at 2: "+b0s1 );
		
		System.out.println( "int at 4: "+b0.readIntLE( 4, 4 ) );
		System.out.println( "int at 8: "+b0.readIntLE( 8, 4 ) );
		System.out.println( b0 );

		System.out.println("writing int 0x01020304 at 4: " );
		b0.writeIntLE( 0x01020304, 4, 4 );
		System.out.println( b0.readIntLE(4, 4) );
		System.out.println( b0 );
		
		System.out.println( "writing long -1 at 4: ");
		b0.writeLongLE( -1, 4, 8 );
		System.out.println( b0.readLongLE(4, 8) );
		System.out.println( b0 );
		
		System.out.println( "writing hex '00 01 02 03 04 05 06 07' at 4: ");
		b0.writeHex( "00 01 02 03 04 05 06 07", 4 );
		System.out.println( b0.readHexBE(4, 4) );
		System.out.println( b0 );
		
		System.out.println( "writing short 496 (0x01F0) at 1: ");
		b0.writeShortLE( (short)496, 1, 2 );
		System.out.println( b0.readShortLE(1, 2) );
		System.out.println( b0 );
		
		System.out.println( "writing short 0x0102 at 10: ");
		b0.writeShortLE( (short)0x8001, 10, 2 );
		System.out.println( b0.readShortLE(10, 2) );
		System.out.println( b0 );
		
		short s = b0.readShortLE( 0, 2 );
		System.out.println( "short at 0: "+s );
		System.out.println( b0 );
		
		b0
			.writeShortLE( (short)0xEEFF, 3, 2 )
			.writeShortLE( (short)0xEEFF, 4, 2 )
			.writeShortLE( (short)0xEEFF, 5, 2 )
			.writeShortLE( (short)0xEEFF, 6, 2 )
		;
		System.out.println( b0 );

	}
	
}