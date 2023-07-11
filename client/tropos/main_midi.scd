Server.killAll;

fork( {
	var numIn = 2;
	var numOut = 2;

	s.options
	.numOutputBusChannels_(numOut)
	.numInputBusChannels_(numIn)
	.blockSize_(4)
	.hardwareBufferSize_(64)
	.numAudioBusChannels_(1024)
	.numControlBusChannels_(4096)
	.maxNodes_(2048)
	.memSize_(8192*64)
	.numWireBufs_(1024)
	.numBuffers_(2048)
	.sampleRate_(48000)
	;

	s.waitForBoot{
		(thisProcess.nowExecutingPath.dirname +/+ "midi.scd").load;
		s.bind {
			(thisProcess.nowExecutingPath.dirname +/+ "synths.scd").load;
			s.sync;
			(thisProcess.nowExecutingPath.dirname +/+ "play_midi.scd").load;
			s.sync;
			(thisProcess.nowExecutingPath.dirname +/+ "test.scd").load;
		}
	};
}, AppClock
);

m.write(30);

~tax = (thisProcess.nowExecutingPath.dirname +/+ "mmx/220707/220707_taxonomy.scd").load;