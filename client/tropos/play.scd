~play = {
	~knobs.do({
		arg item, i;

		if ((i == 6) || (i == 7) || (i == 14) || (i == 15) || (i == 22) || (i == 23)) {
			("knob " ++ i ++ " = ---------------").postln;
		}{
			if (item == 0) {
				("knob " ++ i ++ " = zero").postln;
			}{
				("knob " ++ i ++ " = " ++ item).postln;

				Synth(\rou, [
					\in, 300+(i/8).floor,
					\out, 200+(i%8),
					\amp, item,
					\scale, ~sliders[i%8],
				], addAction: 'addToTail');
			}
		}
	});

	~eq = Synth(\eq, [
		\in, 10,
		\out, 0,
		\lo, ~knobs[23],
		\mid, ~knobs[15],
		\hi, ~knobs[7],
		\amp, -84,
	], addAction: 'addToTail');

	~strip = Synth(\strip, [
		\freq, ~args[0],
		\rrate, ~args[1],
		\zeroadd, ~args[2],
		\zc, ~args[3],
		\zeta, ~args[4],
		\m_threshold, ~args[5],
		\m_gain, ~args[6],
		\freq_outside_gain, ~args[7],
		\fm_outside_gain, ~args[8],
		\zc_outside_gain, ~args[9]
	]);
};