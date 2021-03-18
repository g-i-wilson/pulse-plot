import java.util.*;
import java.time.LocalDateTime;
import bluegill.*;

public class PulseData {

	private Bytes						packet;
	private LocalDateTime		timeReceived;	
	private int							version;
	private int							captureId;
	private int							count;
	private int							actualCount;
	private long						timestamp;
	private List<Integer>		samples;
	private List<Double>		amplitude;
	private List<Double>		phase;
	private int 						duration;
	
	private static Bytes pack (  int version, int captureId, long timestamp, List<Integer> samples ) {
  	Bytes packet = new Bytes( new byte[2+2+4+8+samples.size()*2] );
  	packet
			.writeShortLE( (short)version, 0, 2 )
			.writeShortLE( (short)captureId, 2, 2 )
			.writeIntLE( samples.size(), 4, 4 )
			.writeLongLE( timestamp, 8, 8 );
		for (int i=0; i<samples.size(); i++) {
			packet.writeShortLE( (short)samples.get(i).intValue(), i*2+16, 2 );
		}
		return packet;
	}
  
  public PulseData ( int version, int captureId, long timestamp, List<Integer> samples, double samplesPerCycle ) {
  	this( pack(version, captureId, timestamp, samples), samplesPerCycle );
  }

  public PulseData ( String hexStr, double samplesPerCycle ) {
  	this( new Bytes( hexStr), samplesPerCycle );
  }
  
  public PulseData ( byte[] bytes, double samplesPerCycle ) {
  	this( new Bytes( bytes ), samplesPerCycle );
  }
  
  public PulseData ( Bytes packet, double samplesPerCycle ) {
  	timeReceived	= LocalDateTime.now();
  	this.packet 	=	packet;
  	version 			=	packet.readShortLE( 0, 2 );
  	captureId			=	packet.readShortLE( 2, 2 );
  	count					=	packet.readIntLE( 4, 4 );
  	timestamp			= packet.readLongLE( 8, 8 );
  	samples				= new ArrayList<Integer>();
  	amplitude			= new ArrayList<Double>();
  	phase					= new ArrayList<Double>();
  	duration			= 0;
  	actualCount 	= 0;
  	
		QuadratureDemodulator qd = new QuadratureDemodulator( samplesPerCycle );
		Comparitor comp = new Comparitor( 100.0, 500.0 );
		
		int tempDuration = 0;
  	
  	for (int i=16; i<packet.size(); i+=2) {
  		int sample = packet.readShortLE( i, 2 );
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
	
	public Bytes packet () {
		return packet;
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
		PulseData pd = new PulseData( "00 00 | 01 00 | 04 00 00 00 | 01 00 00 00 00 00 00 00 | 01 00 | 10 00 | 00 01 | 00 10", 4 );
		System.out.println( pd );
		
		PulseData pd2 = new PulseData( 0, 10, 100, new ArrayList<Integer>(Arrays.asList( -100,100,-100,100 )), 4 );
		System.out.println( pd2 );
	}

}