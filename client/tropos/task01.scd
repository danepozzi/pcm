~cycle = fork {
	loop {
		~getvalues.value();
		"getVal".postln;
		(0 + 10.rand).wait;
		~play.value();
		"makeSound".postln;
		5.wait;
		~eq.set(\amp, ~sliders[7] + 6);
		"fadeIn".postln;
		(40 + 20.rand).wait;
		"fadeOut".postln;
		~eq.set(\amp, -84);
		(40 + 20.rand).wait;
		"freeAll".postln;
		s.freeAll;

		(10 + 10.rand).wait;
	}
};


