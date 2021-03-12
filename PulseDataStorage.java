import java.util.*;
import java.time.LocalDateTime;

public class PulseDataStorage {

	private List<PulseData> 											pulses; // all pulses
	private SortedMap<Long, PulseData> 						timeFPGAMap; // sorted by FPGA timestamp
	private SortedMap<LocalDateTime, PulseData> 	timeReceivedMap; // sorted by received time
	private SortedMap<Integer, List<PulseData>>		pulseLengthMap; // sorted by length [samples]
	
	
	public PulseDataStorage () {
		pulses 						= new ArrayList<>();
		timeFPGAMap 			= new TreeMap<>();
		timeReceivedMap 	= new TreeMap<>();
		pulseLengthMap		= new TreeMap<>();
	}
	
	public addPulse ( PulseData p ) {
		pulses.add( p );
		timeFPGAMap.put( p.timestamp(), p );
		timeReceivedMap.put( p.timeReceived, p );
		if (! pulseLengthMap.containsKey(p.samples().size())) {
			pulseLengthMap.put( p.samples().size(), new ArrayList<
	}

}