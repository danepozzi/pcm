~play = {
	var synth;
	var rouSynth = 24.collect(0);
	var eqSynth = 2.collect(0);
	var ll;

	var rou = {
		arg in = 300, out = 200;

		24.collect({
			arg i;
			rouSynth[i] = Synth(
				\rou, [
					\in, in+(i/8).floor,
					\out, out+(i%8),
					\amp, m.knobs.subBus(i).asMap,
					\scale, m.faders.subBus(i%8).asMap,
			], addAction: 'addToTail');
		});
	};

	var eq = {
		2.collect({
			arg i;
			eqSynth[i] = Synth(
				\eq, [
					\in, 10+i,
					\out, 0+i,
					\lo, m.knobs.subBus(23).asMap,
					\mid, m.knobs.subBus(15).asMap,
					\hi, m.knobs.subBus(7).asMap,
					\amp, m.faders.subBus(7).asMap,
			], addAction: 'addToTail');
		});
	};

	var rouFunc = {{rou.value}.defer(0.01)};
	var eqFunc = {{eq.value}.defer(0.01)};

	SynthDef(\rou, {
		arg in, out, amp=0, scale=1;
		var signal = In.ar(in);
		Out.ar(out, signal*amp*scale.dbamp.squared.squared.squared);
	}).add;

	SynthDef(\eq, {
		arg in, out, lo = 0, mid = 0, hi = 0, amp = 0;
		var sig = In.ar(in);
		var filt;
		filt = BLowShelf.ar(sig, 900, 1.0, -1*hi, (-1*hi.min(0).abs).dbamp);
		filt = filt + BPeakEQ.ar(sig, 2000, 1.0, 1*mid, (-1*mid.max(0).abs).dbamp);
		filt = filt + BHiShelf.ar(sig, 900, 1.0, -1*lo, (-1*lo.min(0).abs).dbamp);
		filt = BRF.ar(filt, 6845, 0.1);
		filt = BRF.ar(filt, 6845*2, 0.1);
		filt = filt * amp.dbamp;
		Out.ar([out, out+2], filt/3);
	}).add;

	m.buttonsOn[7]= {
		rouFunc.value;
		eqFunc.value;
		CmdPeriod.add(rouFunc);
		CmdPeriod.add(eqFunc);
	};

	m.buttonsOn[7]= {
		rouFunc.value;
		eqFunc.value;
		CmdPeriod.add(rouFunc);
		CmdPeriod.add(eqFunc);
	};

	m.buttonsOff[7]= {
		24.do({arg i; rouSynth[i].free});
		2.do({arg i; eqSynth[i].free});
		CmdPeriod.remove(rouFunc);
		CmdPeriod.remove(eqFunc);
	};

	m.buttonsOn[15]= {
		synth = Synth(
			\strip, [
				\freq, m.eqSum.subBus(0).asMap,
				\rrate, m.eqSum.subBus(1).asMap,
				\zeroadd, m.eqSum.subBus(2).asMap,
				\zc, m.eqSum.subBus(3).asMap,
				\zeta, m.eq.subBus(7).asMap,
				\m_threshold, m.faders.subBus(8).asMap,
				\m_gain, m.faders.subBus(6).asMap,
				\zc_outside_gain, m.knobs.subBus(22).asMap,
				\fm_outside_gain, m.knobs.subBus(14).asMap,
				\freq_outside_gain, m.knobs.subBus(6).asMap
	])};
	m.buttonsOff[15]= {synth.free};

	m.buttonsOn[14]= {synth.set(\gate, 1)};
	m.buttonsOff[14]= {synth.set(\gate, 0)};
};

~play.value();