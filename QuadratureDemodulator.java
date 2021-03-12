public class QuadratureDemodulator {

	private List<Double> amplitude;
	private List<Double> phase;
	private List<Double> frequency;
	private QuadratureSinusoids qs;
	
	
	public static double multReal (double aR, double aI, double bR, double bI) {
		return aR*bR - aI*bI;
	}

	public static double multImag (double aR, double aI, double bR, double bI) {
		return aR*bI + aI*bR;
	}
		
	public QuadratureDemodulator ( List<Integer> samples, int sampleScale, QuadratureSinusoids qs ) {
		this.qs = qs;
		normalizedSignal = new ArrayList<>();
		amplitude = new ArrayList<>();
		phase = new ArrayList<>();
		frequency = new ArrayList<>();
		
		qs.needSamples( samples.size() ); // expand qs as necessary
		
		double i0 = samples.get(0)*qs.i(t); // initializes q1 to the same as q0
		double q0 = samples.get(0)*qs.q(t); // initializes q1 to the same as q0
		
		for (int t=0; t<samples.size()) {
			double t0 = (double)samples.get(t)/(double)sampleScale; // normalize signal
			i1 = i0; // shift i in time
			q1 = q0; // shift q in time
			double i0 = t0*qs.i(t);
			double q0 = t0*qs.q(t);
			amplitude.add( Math.sqrt( i0*i0 + q0*q0 ) );
			phase.add( Math.atan2( i0, q0 );
			frequency.add(
				Math.atan2(
					multReal(i0, q0, i1, -q1),
					multImag(i0, q0, i1, -q1)
		  	)
		  );
		}
		
	}
	
	public amplitude () {
		return amplitude;
	}
	
	public phase () {
		return phase;
	}
	
	public frequency () {
		return frequency;
	}

}