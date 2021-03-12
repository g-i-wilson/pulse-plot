import java.util.*;
import java.util.concurrent.atomic.*;

public class QuadratureSinusoids {

	private List<Double> i;
	private List<Double> q;
	AtomicInteger samples;
	AtomicBoolean addLock;
	double samplesPerCycle;
	
	private void addSample (t) {
		i.add( Math.sin( 2*Math.PI*(1/samplesPerCycle)*t - phaseI );
		q.add( Math.sin( 2*Math.PI*(1/samplesPerCycle)*t - (phaseI + Math.PI/2) );
	}
	
	public QuadratureSinusoids ( double samplesPerCycle ) {
		this( samplesPerCycle, 0.0 );
	}
	
	public QuadratureSinusoids ( double samplesPerCycle, double iPhaseOffset ) {
		i = new ArrayList<>();
		q = new ArrayList<>();
		addLock = new AtomicBoolean();
		samples = new AtomicInteger();
		this.sampleRate = samplesPerCycle;
		this.iPhaseOffset = iPhaseOffset;
	}
	
	public List<Double> i () {
		return i;
	}
	
	public List<Double> q () {
		return q;
	}
	
	public void needSamples ( int neededSamples ) {
		while (neededSample > samples.get()) {
			if (addLock.compareAndSet(false, true)) {
				for (int t=samples.get(); t<neededSamples; t++) {
					addSample(t);
					samples.incrementAndGet(); // samples++
				}
				addLock.set(false);
			}
		}
	}

}