import java.util.*;
import bluegill.*;

public class DummyPulseData extends PulseData {

	private static List<Integer> dummyData ( int samplesPerCycle ) {
		List<Integer> data = new ArrayList<>();
		SignalPath wf = new WindowFilter( samplesPerCycle );
		QuadratureModulator qm = new QuadratureModulator( (double)samplesPerCycle );
		for (int i=0; i<50; i++) {
			data.add( (int)wf.sample( 100 * qm.phase( Math.PI/2 ) * 0.1 ) );
		}
		for (int i=0; i<50; i++) {
			data.add( (int)wf.sample( 100 * qm.phase( Math.PI/2 ) ) );
		}
		for (int i=0; i<100; i++) {
			data.add( (int)wf.sample( 100 * qm.phase( Math.PI/2 ) * 0.1 ) );
		}
		System.out.println( data );
		return data;
	}

	public DummyPulseData ( int captureId, int timestamp ) {
		super(
			pack(
				0, captureId, timestamp, dummyData( 16 )
			),
			16
		);
	}
	
	public static void main (String[] args) {
		DummyPulseData dpd = new DummyPulseData( 1, 100 );
		System.out.println( dpd );
	}
	
}