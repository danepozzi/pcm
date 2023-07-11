SynthDef(\strip, {
	arg leak=0.9999, out=100, amp= -38.1, rrate=0.0005,
	freq=0.5, mod=0.01, mindx1=0.95, mindx2=0.8, rms,
	tone=250000, zeroscale=4, zeroadd=0, zc = 10, zeta = 0, gate = 1,
	m_threshold = -36, m_gain = 65, zc_outside_gain = 0, fm_outside_gain = 0, freq_outside_gain = 0;

	var router= InFeedback.ar((200..209)).checkBadValues.sanitize;
	var local= LocalIn.ar(2);
	var sig, op= Array.newClear(2);
	var op_freq= Array.newClear(2);
	var op_mod= Array.fill(2, {[0,0]});
	var mic = SoundIn.ar(1);
	var outside = Compander.ar(mic, mic, m_threshold.dbamp, 1, 0) * m_gain.dbamp;
	var zc_outside = [ (outside*300).lag2(5), outside*3000 ] * zc_outside_gain;
	var fm_outside = [ RunningSum.rms(outside, 480), RunningSum.rms(outside, 480000).lag2(10) ] * fm_outside_gain;
	var zero= ZeroCrossing.ar(router[1]) + (outside * freq_outside_gain) / zc + zeroadd;
	var p3= tone*router[0];
	var mood= Pulse.ar(freq+p3)*mod;
	var sens;

	op_freq[0]= 0.01*rrate+(router[4].abs*100) + fm_outside[0];
	op_freq[1]= router[0].sign + fm_outside[1];

	op_mod[0][0]= router[2];
	op_mod[1][0]= router[3];
	op_mod[0][1]= mindx1+router[4];
	op_mod[1][1]= mindx2+router[5];

	op_mod[0][0]= local[0]*op_mod[0][0];
	op_mod[0][1]= local[1]*op_mod[0][1];
	op_mod[1][0]= local[0]*op_mod[1][0];
	op_mod[1][1]= local[1]*op_mod[1][1];

	op[0]= SinOsc.ar(op_freq[0], op_mod[0][0]+op_mod[0][1]);
	op[1]= SinOsc.ar(op_freq[1], op_mod[1][0]+op_mod[1][1]);

	LocalOut.ar(op);

	sig= op*amp.dbamp;
	sig= LeakDC.ar(sig, leak);
	sens= SinOsc.ar((zero*(zeroscale)))*router[3];

	Out.ar(300, sig + (
		FaustZeros.ar(
			sig,
			1.4-sig.sum.lag + zc_outside[0],
			K2A.ar(2000),[582, 2635]+(sig.sum*20000*zc_outside[1]),
			K2A.ar(0),
			K2A.ar(gate))*zeta
	));
	Out.ar(301, mood*0.5);
	Out.ar(302, sens);
	sig= HPF.ar(sig, 60);

	sig = CompanderD.ar(sig, -320.dbamp, 1, 0);
	sig = sig*300.dbamp;
	rms = RunningSum.rms(sig, 440);
	sig = LPF.ar(sig, 6000);
	Out.ar(10, (sig+(
		FaustZeros.ar(
			sig,
			1.4-sig.sum.lag + zc_outside[0],
			K2A.ar(2000),[582, 2635]+(sig.sum*20000*zc_outside[1]),
			K2A.ar(0),
			K2A.ar(gate))
		*zeta)).sum);
}, [0.5]).add;

SynthDef(\rou, {
	arg in, out, amp=0, scale=1;
	var signal = In.ar(in);
	Out.ar(out, signal*amp*scale.dbamp.pow(8));
}).add;

SynthDef(\eq, {
	arg in, out, lo = 0, mid = 0, hi = 0, amp = 0;
	var sig = In.ar(in);
	var filt;
	filt = BLowShelf.ar(sig, 900, 1.0, -1*hi, (-1*hi.min(0).abs).dbamp);
	filt = filt + BPeakEQ.ar(sig, 2000, 1.0, 1*mid, (-1*mid.max(0).abs).dbamp);
	filt = filt + BHiShelf.ar(sig, 900, 1.0, -1*lo, (-1*lo.min(0).abs).dbamp);
	//filt = BRF.ar(filt, 6350, 0.1);
	//filt = BRF.ar(filt, 12700, 0.1);
	filt = filt * amp.dbamp.lag(40);
	Out.ar(out, filt/3);
}).add;