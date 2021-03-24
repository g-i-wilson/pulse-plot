import java.util.*;
import bluegill.*;

public class DummyPulseData extends PulseData {

	private int captureId;
	private long timestamp;

	private static double randomRange ( double min, double max ) {
		return (Math.random()*(max-min))+min;
	}

	private static List<Integer> dummyData ( int samplesPerCycle ) {
		List<Integer> data = new ArrayList<>();
		SignalPath wf = new WindowFilter( samplesPerCycle );
		QuadratureModulator qm = new QuadratureModulator( (double)samplesPerCycle );
		double phaseAngle = randomRange(Math.PI/2, Math.PI*3/2);
		for (int i=0; i<(int)randomRange(20,300); i++) {
			data.add( (int)wf.sample( 100 * qm.phase( phaseAngle ) * 0.1 ) );
		}
		for (int i=0; i<(int)randomRange(800,1000); i++) {
			data.add( (int)wf.sample( 100 * qm.phase( phaseAngle ) ) );
		}
		for (int i=0; i<(int)randomRange(100,100); i++) {
			data.add( (int)wf.sample( 100 * qm.phase( phaseAngle ) * 0.1 ) );
		}
		return data;
	}
	
	public DummyPulseData () {
		this( 0, 0, (int)randomRange(12.0,20.0) );
	}

	public DummyPulseData ( int captureId, long timestamp, int samplesPerCycle ) {
		super( 0, captureId, timestamp, dummyData( samplesPerCycle ), samplesPerCycle );
		this.captureId = captureId;
		this.timestamp = timestamp;
	}
	
	public DummyPulseData spawn () {
		return spawn( 1 );
	}
	
	public DummyPulseData spawn ( long timeDelta ) {
		return new DummyPulseData( ++captureId, timestamp+timeDelta, (int)randomRange(12.0,20.0) );
	}
	
	public static void main (String[] args) {
		DummyPulseData dpd = null;
		for (int i=0; i<3; i++) {
			if (dpd == null) dpd = new DummyPulseData();
			else dpd = dpd.spawn();
			System.out.println( dpd );
		}
	}
	
}