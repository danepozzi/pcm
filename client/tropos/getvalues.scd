~getvalues = {
	var seed = ~taxonomy.size.rand;
	~knobs = ~taxonomy[seed][0];
	~knobs.postln;
	~sliders = ~taxonomy[seed][1];
	~sliders.postln;
	~args = ~taxonomy[seed][2];
	~args.postln;

	("!!! snapshot n. " ++ seed ++ " loaded from 220707_taxonomy.scd !!!").postln;
};

// args are (play.scd):
// 0 \freq, m.eqSum.subBus(0).asMap,
// 1 \rrate, m.eqSum.subBus(1).asMap,
// 2 \zeroadd, m.eqSum.subBus(2).asMap,
// 3 \zc, m.eqSum.subBus(3).asMap,
// 4 \zeta, m.eq.subBus(7).asMap,
// 5 \m_threshold, m.faders.subBus(8).asMap,
// 6 \m_gain, m.faders.subBus(6).asMap,
// 7 \freq_outside_gain, m.knobs.subBus(6).asMap,
// 8 \fm_outside_gain, m.knobs.subBus(14).asMap,
// 9 \zc_outside_gain, m.knobs.subBus(22).asMap

// or (MidiMixKN.sc):
// 0 sargs = 4.collect({arg i; eqSum.subBus(i).getSynchronous});
// 4 sargs = sargs.add(eq.subBus(7).getSynchronous);		//zeta
// 5 sargs = sargs.add(faders.subBus(8).getSynchronous); //compressor threshold
// 6 sargs = sargs.add(faders.subBus(6).getSynchronous); //compressor gain
// 7 sargs = sargs.add(knobs.subBus(6).getSynchronous); 	//freq outside gain
// 8 sargs = sargs.add(knobs.subBus(14).getSynchronous); //fm outside gain
// 9 sargs = sargs.add(knobs.subBus(22).getSynchronous); //zc outside gain
