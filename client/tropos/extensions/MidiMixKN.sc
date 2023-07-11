MidiMixKN {
	var idx, knobsMapArray, knobsMap, eqMapArray, eqMap, cc_knob, cc_numBox, cc_button, <>cc_slider, posx=750, posy=320, buf, <>mmrec, recSynth, osc,
	playb, <>knobs, <>knobsSum, <>eqSum, <>eq, <>eqFaders, <>buttons, <>faders, <>buttonsOn, <>fadersFunc, <>knobsFunc, buff,
	<>buttonsOff, gate=1, <>mmx_out, <>mm_dest, <>mm_port;

	*initClass {
		StartUp.add {
			this.initSynthDefs;
		}
	}

	*initSynthDefs {
		SynthDef(\mmx_record, {
			arg bufnum=0, bus=30;
			DiskOut.ar(bufnum, K2A.ar(In.kr(bus,2)));
			//DiskOut.ar(bufnum, SinOsc.ar);
		}).add;

		SynthDef(\mmx_pb, {
			arg bufnum = 0;
			SendReply.kr(Impulse.kr(50), '/pb', PlayBuf.ar(2, bufnum));
		}).add;

		SynthDef(\write, { |bufnum|
			DiskOut.ar(bufnum, In.ar(0,2));
		}).add;

		SynthDef(\channelStrip, {
			arg in, out, lo = 0, mid = 0, hi = 0, amp = 0;
			var sig = In.ar(in);
			var filt;
			SendTrig.kr(Impulse.kr(100), 0, RunningSum.rms(sig, 480));
			filt = BLowShelf.ar(sig, 900, 1.0, -1*hi, (-1*hi.min(0).abs).dbamp);
			filt = filt + BPeakEQ.ar(sig, 2000, 1.0, 1*mid, (-1*mid.max(0).abs).dbamp);
			filt = filt + BHiShelf.ar(sig, 900, 1.0, -1*lo, (-1*lo.min(0).abs).dbamp);
			filt = BRF.ar(filt, 6845, 0.1);
			filt = BRF.ar(filt, 6845*2, 0.1);
			filt = filt * amp.dbamp;
			Out.ar([out, out+2], filt/3);
		}).add;
	}

	*new {
		arg index=0, px=750, py=320;
		^super.new.init(index,px,py)
	}

	init {
		arg index, px, py;
		var mouseDown= false, offset=0;
		var gui_objects_width= 40;
		var wind, krouter, mode;

		mm_dest = MIDIClient.destinations.collect({arg item; item.asString}).indicesOfEqual("MIDIEndPoint(\"MIDI Mix\", \"MIDI Mix MIDI 1\")");
		mm_port = MIDIClient.destinations[mm_dest[index]];

		idx = index;
		posx = px;
		posy = py;
		wind = Window.new(mm_port.uid.asString, Rect(posx, posy, gui_objects_width*9-18, 340)).front.alwaysOnTop_(true);
		wind.view.decorator=FlowLayout(wind.view.bounds);
		wind.view.decorator.gap=2@2;
		wind.background= Color.black.alpha = 0.25;

		knobs= Bus.control(numChannels:24);		//knobs
		knobsSum= Bus.control(numChannels:8);	//knobs
		eq= Bus.control(numChannels:24);			//eq
		eqSum= Bus.control(numChannels:8);			//eq
		eqFaders= Bus.control(numChannels:9);			//eq
		buttons= Array.fill(16, {0});		    		//buttons
		buttonsOn= Array.newClear(16);
		buttonsOff= Array.newClear(16);
		fadersFunc= Array.newClear(9);
		knobsFunc= Array.newClear(24);
		faders= Bus.control(numChannels:9);		//faders
		mmrec = Bus.control(numChannels:2);
		cc_knob= 24.collect;
		cc_numBox= 48.collect;
		cc_button= 16.collect;
		cc_slider= 9.collect;

		eqMapArray= Array.fill(48, {arg i; i%2});
		knobsMapArray= Array.fill(48, {arg i; i%2});
		this.knobsMapping;

		//krouter = KnobRouter(300, 200, 3, 8, px: posx+500, py: posy+240).survive;
		24.do({arg i; eq.setAt(i, 0.5)});
		9.do({arg i; eqFaders.setAt(i, -30)});

		//KNOBS
		3.do{
			arg kn;
			8.do{
				arg knob;
				var which= knob+(kn*8);
				cc_knob[which]= Knob(
					parent:wind,
					bounds:gui_objects_width @ 15)
				.action_({
					arg obj;
					if(gate!=0){
						//if((obj.value-knobs.subBus(which+offset).getSynchronous).abs<0.05){
						knobs.setAt(which+offset, knobsMap[which].map(obj.value).post);
						mmrec.setAt(0, which/100);
						mmrec.setAt(1, obj.value);

						if(knobsFunc[which+offset].notNil){
							knobsFunc[which+offset].value(knobsMap[which].map(obj.value));
						};


						"   |   ".post;

						if(which>15){
							knobsSum.setAt(which+offset-16,
								(
									knobsMap[which].map(obj.value)+
									knobsMap[which-8].map(knobs.subBus(which-8).getSynchronous)+
									knobsMap[which-16].map(knobs.subBus(which-16).getSynchronous)
							).postln)
						}{if(which>7){
							knobsSum.setAt(which+offset-8,
								(
									knobsMap[which].map(obj.value)+
									knobsMap[which+8].map(knobs.subBus(which+8).getSynchronous)+
									knobsMap[which-8].map(knobs.subBus(which-8).getSynchronous)
							).postln)
						}{
							knobsSum.setAt(which+offset,
								(
									knobsMap[which].map(obj.value)+
									knobsMap[which+8].map(knobs.subBus(which+8).getSynchronous)+
									knobsMap[which+16].map(knobs.subBus(which+16).getSynchronous)
							).postln)
						}
						}
						/*}{
						("MAGNET: "++knobs.subBus(which+offset).getSynchronous).postln;
						}*/
					}{
						//if((obj.value-eq.subBus(which+offset).getSynchronous.linlin(-40,40,0,1)).abs<0.05){
						//eq.setAt(which+offset, obj.value.postln);
						eq.setAt(which+offset, eqMap[which].map(obj.value).post);

						"   |   ".post;

						eqSum.setAt(which+offset % 8, (
							eqMap[which].map(obj.value) *
							eq.subBus((which+8)%24).getSynchronous *
							eq.subBus((which+16)%24).getSynchronous).postln
						);
						/*}{
						("MAGNET: "++eq.subBus(which+offset).getSynchronous.linlin(0,1,-40,40)).postln;
						}*/
					}
				})
				.mouseDownAction_{mouseDown=true}
				.mouseUpAction_{mouseDown=false};
			};
			8.do{
				arg map;
				2.do{
					arg mapp;
					cc_numBox[mapp+(map*2)+(kn*16)]= NumberBox(
						parent: wind,
						bounds:gui_objects_width/2.1@ 11)
					.font_(Font(size:6))
					.background_(Color.white.alpha = 0.45)
					.value_(knobsMapArray[mapp+(map*2)+(kn*16)])
					.action_({
						arg num;
						knobsMapArray[mapp+(map*2)+(kn*16)]= num.value;
						this.knobsMapping;
					})
				}
			}
		};

		//BUTTONS
		16.do{
			arg button;
			cc_button[button]= Button(
				parent:wind,
				bounds:gui_objects_width @ 12

			)
			.states_([
				[""],
				["", Color.gray, Color.gray]])
			.action_({
				arg obj;
				//knob.postln;
				obj.value.postln;
				buttons[button+offset]= obj.value;
				mmrec.setAt(0, button+24/100);
				mmrec.setAt(1, obj.value);

				if(MIDIIn.findPort("MIDI Mix", "MIDI Mix MIDI 1").notNil) {
					mmx_out.noteOn(0, ((button+offset)*3+1)%22, 127*obj.value);
					if(button==7){mmx_out.noteOn(0, 22, 127*obj.value)};
					if(button==15){mmx_out.noteOn(0, 24, 127*obj.value)};
				};
				if(obj.value==1){
					if(buttonsOn[button+offset].notNil)
					{
						buttonsOn[button+offset].value;
					}
				}{
					if(buttonsOff[button+offset].notNil)
					{
						buttonsOff[button+offset].value
					}
				}
			})
		};

		//FADERS
		6.do{
			arg slider;
			var sld;
			cc_slider[slider]= EZSlider(
				parent: wind,
				bounds: Rect(0, 0, gui_objects_width, 180),
				label: "    " ++ (slider),
				controlSpec: \db,
				action: {
					arg obj;
					/*if(gate!=0){
					faders.setAt(slider+offset, obj.value.postln);
					mmrec.setAt(0, slider+40/100);
					mmrec.setAt(1, obj.value.dbamp.sqrt);
					}{
					eqFaders.setAt(slider+offset, (obj.value.dbamp**2).ampdb.postln);//GUADAGNO
					};*/

					faders.setAt(slider+offset, obj.value.postln);
					mmrec.setAt(0, slider+40/100);
					mmrec.setAt(1, obj.value.dbamp.sqrt);

					if(fadersFunc[slider+offset].notNil)
					{
						fadersFunc[slider+offset].value(obj.value);
					}
				},
				unitWidth:30,
				initVal:-3,
				numberWidth:60,
				layout:\vert
			)
			.setColors(Color.grey,Color.white)
			.font_(Font("Helvetica",9))
			.sliderView.mouseDownAction_{mouseDown=true}
			.mouseUpAction_{mouseDown=false};

			sld=slider*2;
		};

		1.do{
			arg slider;
			var sld;
			cc_slider[6]= EZSlider(
				parent: wind,
				bounds: Rect(0, 0, gui_objects_width, 180),
				label: "    " ++ (slider),
				controlSpec: \db,
				action: {
					arg obj;
					/*if(gate!=0){
					faders.setAt(slider+offset, obj.value.postln);
					mmrec.setAt(0, slider+40/100);
					mmrec.setAt(1, obj.value.dbamp.sqrt);
					}{
					eqFaders.setAt(slider+offset, (obj.value.dbamp**2).ampdb.postln);//GUADAGNO
					};*/

					faders.setAt(6, (obj.value.dbamp*2-1*240).postln); // comp gain
				},
				unitWidth:30,
				initVal:-3,
				numberWidth:60,
				layout:\vert
			)
			.setColors(Color.grey,Color.white)
			.font_(Font("Helvetica",9))
			.sliderView.mouseDownAction_{mouseDown=true}
			.mouseUpAction_{mouseDown=false};

			sld=slider*2;
		};

		1.do{
			arg slider;
			var sld;
			cc_slider[7]= EZSlider(
				parent: wind,
				bounds: Rect(0, 0, gui_objects_width, 180),
				label: "    " ++ (slider),
				controlSpec: \db,
				action: {
					arg obj;
					/*if(gate!=0){
					faders.setAt(slider+offset, obj.value.postln);
					mmrec.setAt(0, slider+40/100);
					mmrec.setAt(1, obj.value.dbamp.sqrt);
					}{
					eqFaders.setAt(slider+offset, (obj.value.dbamp**2).ampdb.postln);//GUADAGNO
					};*/

					faders.setAt(7, obj.value.postln);
				},
				unitWidth:30,
				initVal:-3,
				numberWidth:60,
				layout:\vert
			)
			.setColors(Color.grey,Color.white)
			.font_(Font("Helvetica",9))
			.sliderView.mouseDownAction_{mouseDown=true}
			.mouseUpAction_{mouseDown=false};

			sld=slider*2;
		};

		1.do{
			arg slider;
			var sld;
			cc_slider[8]= EZSlider(
				parent: wind,
				bounds: Rect(0, 0, 240, 30),
				controlSpec: \db,
				action: {
					arg obj;
					faders.setAt(8, (obj.value.dbamp + (1/127)).squared.squared.ampdb.postln); //threshold
				},
				unitWidth:30,
				initVal:-3,
				numberWidth:60,
				//layout:\vert
			)
			.setColors(Color.grey,Color.white)
			.font_(Font("Helvetica",9))
			.sliderView.mouseDownAction_{mouseDown=true}
			.mouseUpAction_{mouseDown=false};
		};

		Button(wind, Rect(0, 0, 29, 30))
		.states_([["n_box", Color.white, Color.grey]])
		.font_(Font("Helvetica",7))
		.action_({
			knobsMapArray.postln;
		});

		Button(wind, Rect(0, 0, 29, 30))
		.states_([["dump", Color.white, Color.grey]])
		.font_(Font("Helvetica",7))
		.action_({
			this.dump;
		});

		mode= Button(wind, Rect(0, 0, 29, 30))
		.states_([["CC", Color.white, Color.grey],["EQ", Color.white, Color.red]])
		.font_(Font("Helvetica",7))
		.action_({
			if(gate!=0){
				this.setGate(0);
				defer{48.do({arg i; cc_numBox[i].value=eqMapArray[i]})};
				defer{24.do({arg i; cc_knob[i].centered_(false).value= eqMap[i].unmap(eq.subBus(i).getSynchronous)})};
				defer{6.do({arg i; cc_button[i].valueAction_(0); cc_button[i+8].valueAction_(0)})};
				defer{4.do({arg i; cc_button[i].valueAction_(127)})};
				if(MIDIIn.findPort("MIDI Mix", "MIDI Mix MIDI 1").notNil) {
					mmx_out.noteOn(0, 25, 127);
					mmx_out.noteOn(0, 26, 0);
				}
				//defer{9.do({arg i; cc_slider[i].value=eqFaders.subBus(i).getSynchronous.dbamp.sqrt})};
			}{
				this.setGate(1);
				defer{48.do({arg i; cc_numBox[i].value=knobsMapArray[i]})};
				defer{24.do({arg i; cc_knob[i].centered_(false).value= knobsMap[i].unmap(knobs.subBus(i).getSynchronous)})};
				defer{6.do({arg i; cc_button[i].valueAction_(0); cc_button[i+8].valueAction_(0)})};
				defer{6.do({arg i; cc_button[i+8].valueAction_(127)})};
				if(MIDIIn.findPort("MIDI Mix", "MIDI Mix MIDI 1").notNil) {
					mmx_out.noteOn(0, 25, 0);
					mmx_out.noteOn(0, 26, 127);
				}
				//defer{9.do({arg i; cc_slider[i].value=faders.subBus(i).getSynchronous.dbamp.sqrt})};
			}
		});

		if(MIDIIn.findPort("MIDI Mix", "MIDI Mix MIDI 1").notNil){
			var case=[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];
			var midiout=[1,4,7,10,13,16,19,22,3,6,9,12,15,18,21,24];
			var noteout=[2,5,8,11,14,17,20,23];

			Platform.case(
				\osx,       { mmx_out = MIDIOut.newByName("MIDI Mix", "MIDI Mix MIDI 1") },
				\linux,     { mmx_out = MIDIOut(0, mm_port.uid); mmx_out.connect(mm_dest[idx]); },
				\windows,   { mmx_out = MIDIOut.newByName("MIDI Mix", "MIDI Mix MIDI 1") }
			);

			//hello
			Task({
				16.do({ arg i;
					mmx_out.noteOn(0, midiout[i%16], 127);
					mmx_out.noteOn(0, 25, 0);
					mmx_out.noteOn(0, 26, 0);
					(0.02).wait;
					mmx_out.noteOn(0, midiout[15-i%16], 127);
					(0.02).wait;
					mmx_out.noteOn(0, midiout[i%16], 0);
					(0.02).wait;
					mmx_out.noteOn(0, midiout[15-i%16], 0);
					mmx_out.noteOn(0, 25, 0);
					mmx_out.noteOn(0, 26, 127);
				});
			}).play;

			//KNOBS
			MIDIdef.cc(\mm_k ++ mm_port.uid, {arg val, num, chan;
				defer{cc_knob[num-16].valueAction=(val/127)};
			}, (16..48), srcID: mm_port.uid).permanent_(true);

			//BUTTONS
			MIDIdef.noteOn(\mm_ba ++ mm_port.uid, {arg val, num, chan;
				if (case[num-65] == 0){
					defer{cc_button[num-65].valueAction=127};
					case[num-65] = 1;
					//mmx_out.noteOn(0, midiout[num-65], 127)
				} {
					defer{cc_button[num-65].valueAction=0};
					case[num-65] = 0;
					//mmx_out.noteOn(0, midiout[num-65], 0)
				}
			}, (65..80), srcID: mm_port.uid).permanent_(true);


			MIDIdef.noteOn (\bankleft ++ mm_port.uid, {
				if(gate!=0){
					\left.postln;
					defer{mode.valueAction=1};
				}
			}, 25, srcID: mm_port.uid).permanent_(true);

			MIDIdef.noteOn (\bankright ++ mm_port.uid, {
				if(gate==0){
					\right.postln;
					defer{mode.valueAction=0};
				}
			}, 26, srcID: mm_port.uid).permanent_(true);

			//FADERS
			MIDIdef.cc(\mm_s ++ mm_port.uid, {arg val, num, chan;
				defer{cc_slider[num-1].valueAction=((val+1)/127)}//sens
				//defer{cc_slider[num-1].valueAction=((val+0.00001)/127)}//sens
			}, (1..9), srcID: mm_port.uid).permanent_(true);

			MIDIdef.cc(\mm_bb ++ mm_port.uid, {arg val, num, chan;
				mmx_out.noteOn(0, noteout[num-65], val)
			}, (65..73), srcID: mm_port.uid).permanent_(true);
		};

		//Qknob.sc ---> defaultKeyDownAction
		wind.view.keyDownAction = {
			arg view, char, modifiers, unicode, keycode;
			switch(keycode,
				18,{if(cc_button[0].value==0){defer{cc_button[0].valueAction=1}}{defer{cc_button[0].valueAction=0}}},
				19,{if(cc_button[1].value==0){defer{cc_button[1].valueAction=1}}{defer{cc_button[1].valueAction=0}}},
				20,{if(cc_button[2].value==0){defer{cc_button[2].valueAction=1}}{defer{cc_button[2].valueAction=0}}},
				21,{if(cc_button[3].value==0){defer{cc_button[3].valueAction=1}}{defer{cc_button[3].valueAction=0}}},
				23,{if(cc_button[4].value==0){defer{cc_button[4].valueAction=1}}{defer{cc_button[4].valueAction=0}}},
				22,{if(cc_button[5].value==0){defer{cc_button[5].valueAction=1}}{defer{cc_button[5].valueAction=0}}},
				26,{if(cc_button[6].value==0){defer{cc_button[6].valueAction=1}}{defer{cc_button[6].valueAction=0}}},
				28,{if(cc_button[7].value==0){defer{cc_button[7].valueAction=1}}{defer{cc_button[7].valueAction=0}}},

				12,{if(cc_button[8].value==0){defer{cc_button[8].valueAction=1}}{defer{cc_button[8].valueAction=0}}},
				13,{if(cc_button[9].value==0){defer{cc_button[9].valueAction=1}}{defer{cc_button[9].valueAction=0}}},
				14,{if(cc_button[10].value==0){defer{cc_button[10].valueAction=1}}{defer{cc_button[10].valueAction=0}}},
				15,{if(cc_button[11].value==0){defer{cc_button[11].valueAction=1}}{defer{cc_button[11].valueAction=0}}},
				17,{if(cc_button[12].value==0){defer{cc_button[12].valueAction=1}}{defer{cc_button[12].valueAction=0}}},
				16,{if(cc_button[13].value==0){defer{cc_button[13].valueAction=1}}{defer{cc_button[13].valueAction=0}}},
				32,{if(cc_button[14].value==0){defer{cc_button[14].valueAction=1}}{defer{cc_button[14].valueAction=0}}},
				34,{if(cc_button[15].value==0){defer{cc_button[15].valueAction=1}}{defer{cc_button[15].valueAction=0}}},

				0,{defer{cc_slider[0].valueAction=(cc_slider[0].value+0.01)}},
				6,{defer{cc_slider[0].valueAction=(cc_slider[0].value-0.01)}},
				1,{defer{cc_slider[1].valueAction=(cc_slider[1].value+0.01)}},
				7,{defer{cc_slider[1].valueAction=(cc_slider[1].value-0.01)}},
				2,{defer{cc_slider[2].valueAction=(cc_slider[2].value+0.01)}},
				8,{defer{cc_slider[2].valueAction=(cc_slider[2].value-0.01)}},
				3,{defer{cc_slider[3].valueAction=(cc_slider[3].value+0.01)}},
				9,{defer{cc_slider[3].valueAction=(cc_slider[3].value-0.01)}},
				5,{defer{cc_slider[4].valueAction=(cc_slider[4].value+0.01)}},
				11,{defer{cc_slider[4].valueAction=(cc_slider[4].value-0.01)}},
				4,{defer{cc_slider[5].valueAction=(cc_slider[5].value+0.01)}},
				45,{defer{cc_slider[5].valueAction=(cc_slider[5].value-0.01)}},
				38,{defer{cc_slider[6].valueAction=(cc_slider[6].value+0.01)}},
				46,{defer{cc_slider[6].valueAction=(cc_slider[6].value-0.01)}},
				40,{defer{cc_slider[7].valueAction=(cc_slider[7].value+0.01)}},
				43,{defer{cc_slider[7].valueAction=(cc_slider[7].value-0.01)}}
			);
		};
	}

	numBoxUpdate {
		cc_numBox.size.do({arg i; if(knobsMapArray[i].notNil){cc_numBox[i].value = knobsMapArray[i]}});
	}

	knobsMapping {
		arg ... knobs;
		knobsMap = 24.collect({arg i; ControlSpec(knobsMapArray[2*i],knobsMapArray[2*i+1])});
		knobsMap.postln;
	}

	eqMapping {
		arg ... knobs;
		eqMap = 24.collect({arg i; ControlSpec(eqMapArray[2*i],eqMapArray[2*i+1])});
		eqMap.postln;
	}

	singleKnobMap {
		arg which, linear = true, lo, hi;
		if(linear){knobsMap[which] = ControlSpec(lo,hi,\lin)}{knobsMap[which] = ControlSpec(lo,hi,\exponential)}
	}

	setKnobs {
		arg ... knobs;
		knobs.size.do({ arg i;
			knobsMapArray[i]= knobs[i];
		});
		this.knobsMapping;
		this.numBoxUpdate;
	}

	setEQ {
		arg ... knobs;
		knobs.size.do({ arg i;
			eqMapArray[i]= knobs[i];
		});
		this.eqMapping;
	}

	write {
		arg seconds = 10;
		var file, tax;
		var buffer = Buffer.alloc(Server.default, 65536, 2);
		var synth;
		var lights;
		var karray, sarray, sargs;

		File.mkdir(thisProcess.nowExecutingPath.dirname +/+ "mmx" +/+ Date.getDate.format("%y%m%d"));

		buffer.write(thisProcess.nowExecutingPath.dirname +/+ "mmx" +/+ Date.getDate.format("%y%m%d") +/+Date.getDate.format("%y%m%d_%H%M%S_")++ "snap.aiff".standardizePath, "aiff", "int32", 0, 0, true);

		lights = Task({ { this.flash(7); this.flash(15); 0.5.wait;}.loop });

		{
			("recording " ++ seconds ++ " seconds").postln;
			synth = Synth.tail(nil, \write, ["bufnum", buffer]);
			lights.start;
			seconds.wait;
			lights.stop;
			synth.free;
			1.wait;
			buffer.close;
			1.wait;
			buffer.free;
			"done".postln;
		}.fork;

		if (File.exists(thisProcess.nowExecutingPath.dirname +/+"mmx" +/+Date.getDate.format("%y%m%d") +/+Date.getDate.format("%y%m%d")++"_snap.scd".standardizePath)) {
			file = File(thisProcess.nowExecutingPath.dirname +/+"mmx" +/+Date.getDate.format("%y%m%d") +/+Date.getDate.format("%y%m%d")++"_snap.scd".standardizePath,"a+")} {
			file = File(thisProcess.nowExecutingPath.dirname +/+"mmx" +/+Date.getDate.format("%y%m%d") +/+Date.getDate.format("%y%m%d")++"_snap.scd".standardizePath,"w");
			file.write("//snapshots recorded on Supercollider v. " ++ Main.version ++ " using MidiMix v. 1.1 by daniele pozzi");
			file.write("\n");
			file.write("//server at " ++ Server.default.options.sampleRate ++ " (sr) " ++ Server.default.options.blockSize ++ " (bs) " ++ Server.default.options.memSize ++ " (mems)");
			file.write("\n");
			file.write("//called on " ++ Date.getDate.format("%d %b %Y") ++ " from");
			file.write("\n");
			file.write("\"" ++ thisProcess.nowExecutingPath ++ "\".openDocument;");
			file.write("\n");
			file.write("\n");
		};


		file.write("//");
		file.write(Date.getDate.format("%y%m%d_%H%M%S"));
		file.write("----------------------------------------------------------------------");
		file.write("\n");
		file.write("b = Buffer.read(Server.default, \"" ++ (thisProcess.nowExecutingPath.dirname +/+ "mmx" +/+ Date.getDate.format("%y%m%d") +/+Date.getDate.format("%y%m%d_%H%M%S_")++ "snap.aiff").asString ++ "\"); \nb.play; \nb.free;\n\n");

		file.write("(\n");
		file.write("m.setEQ(");
		48.do({arg i; file.write(eqMapArray[i].asString);file.write(",")});
		file.write(");\n");
		file.write("m.setKnobs(");
		48.do({arg i; file.write(knobsMapArray[i].asString);file.write(",")});
		file.write(");\n\n");
		file.write("m.loadEQ(");
		24.do({arg i; file.write((eqMap[i].unmap(eq.subBus(i).getSynchronous)).asString);file.write(",")});
		file.write("); m.multiply;\n");
		file.write("m.loadKnobs(");
		24.do({arg i; file.write((knobsMap[i].unmap(knobs.subBus(i).getSynchronous)).asString);file.write(",")});
		file.write(");\n");
		file.write("m.loadFaders(");
		9.do({arg i; file.write((cc_slider[i].value).asString);file.write(",")});
		karray = 24.collect({arg i; knobs.subBus(i).getSynchronous});
		sarray = 9.collect({arg i; faders.subBus(i).getSynchronous});
		sargs = 4.collect({arg i; eqSum.subBus(i).getSynchronous});
		sargs = sargs.add(eq.subBus(7).getSynchronous);		//zeta
		sargs = sargs.add(faders.subBus(8).getSynchronous); //compressor threshold
		sargs = sargs.add(faders.subBus(6).getSynchronous); //compressor gain
		sargs = sargs.add(knobs.subBus(6).getSynchronous); 	//freq outside gain
		sargs = sargs.add(knobs.subBus(14).getSynchronous); //fm outside gain
		sargs = sargs.add(knobs.subBus(22).getSynchronous); //zc outside gain

		file.write(");\n");

		file.write(")\n");
		file.write("\n");
		file.write("~knobs = " + karray + ";\n");
		file.write("~sliders = " + sarray + ";\n");
		file.write("~args = " + sargs + ";\n");
		file.write("//------------------------------------------------------------------------------------");
		file.write("\n\n\n");
		file.close;

		~knobs = karray;
		~sliders = sarray;
		~args = sargs;

		if (File.exists(thisProcess.nowExecutingPath.dirname +/+"mmx" +/+Date.getDate.format("%y%m%d") +/+Date.getDate.format("%y%m%d")++"_taxonomy.scd".standardizePath)) {
			tax = File(thisProcess.nowExecutingPath.dirname +/+"mmx" +/+Date.getDate.format("%y%m%d") +/+Date.getDate.format("%y%m%d")++"_taxonomy.scd".standardizePath,"a+")} {
			tax = File(thisProcess.nowExecutingPath.dirname +/+"mmx" +/+Date.getDate.format("%y%m%d") +/+Date.getDate.format("%y%m%d")++"_taxonomy.scd".standardizePath,"w");
			tax.write("//taxonomy generated on Supercollider v. " ++ Main.version ++ " using MidiMix v. 1.1 by daniele pozzi");
			tax.write("\n");
			tax.write("//server at " ++ Server.default.options.sampleRate ++ " (sr) " ++ Server.default.options.blockSize ++ " (bs) " ++ Server.default.options.memSize ++ " (mems)");
			tax.write("\n");
			tax.write("//called on " ++ Date.getDate.format("%d %b %Y") ++ " from");
			tax.write("\n//");
			tax.write("\"" ++ thisProcess.nowExecutingPath ++ "\".openDocument;");
			tax.write("\n");
			tax.write("\n");
		};

		tax.write("[\n" + karray + ",\n" + sarray + ",\n" + sargs + "\n],");
		tax.close;
	}

	strip {
		~knobs = 24.collect({arg i; knobs.subBus(i).getSynchronous});
		~sliders = 9.collect({arg i; faders.subBus(i).getSynchronous});
		~args = 4.collect({arg i; eqSum.subBus(i).getSynchronous});
	}

	flash {
		arg i;

		if(MIDIIn.findPort("MIDI Mix", "MIDI Mix MIDI 1").notNil) {
			if (buttons[i] == 0) {
				{
					mmx_out.noteOn(0, (i*3+1)%22, 127);
					if(i==7){mmx_out.noteOn(0, 22, 127)};
					if(i==15){mmx_out.noteOn(0, 24, 127)};

					0.25.wait;

					mmx_out.noteOn(0, (i*3+1)%22, 0);
					if(i==7){mmx_out.noteOn(0, 22, 0)};
					if(i==15){mmx_out.noteOn(0, 24, 0)};
				}.fork;
			} {
				{
					mmx_out.noteOn(0, (i*3+1)%22, 0);
					if(i==7){mmx_out.noteOn(0, 22, 0)};
					if(i==15){mmx_out.noteOn(0, 24, 0)};

					0.25.wait;

					mmx_out.noteOn(0, (i*3+1)%22, 127);
					if(i==7){mmx_out.noteOn(0, 22, 127)};
					if(i==15){mmx_out.noteOn(0, 24, 127)};
				}.fork;
			}
		}
	}

	flashQ {
		arg i;

		if(MIDIIn.findPort("MIDI Mix", "MIDI Mix MIDI 1").notNil) {
			if (buttons[i] == 0) {
				{
					mmx_out.noteOn(0, (i*3+1)%22, 127);
					if(i==7){mmx_out.noteOn(0, 22, 127)};
					if(i==15){mmx_out.noteOn(0, 24, 127)};

					0.01.wait;

					mmx_out.noteOn(0, (i*3+1)%22, 0);
					if(i==7){mmx_out.noteOn(0, 22, 0)};
					if(i==15){mmx_out.noteOn(0, 24, 0)};
				}.fork;
			} {
				{
					mmx_out.noteOn(0, (i*3+1)%22, 0);
					if(i==7){mmx_out.noteOn(0, 22, 0)};
					if(i==15){mmx_out.noteOn(0, 24, 0)};

					0.01.wait;

					mmx_out.noteOn(0, (i*3+1)%22, 127);
					if(i==7){mmx_out.noteOn(0, 22, 127)};
					if(i==15){mmx_out.noteOn(0, 24, 127)};
				}.fork;
			}
		}
	}

	light {
		arg i;

		if(MIDIIn.findPort("MIDI Mix", "MIDI Mix MIDI 1").notNil) {
			if (buttons[i] == 0) {
				mmx_out.noteOn(0, (i*3+1)%22, 127);
				if(i==7){mmx_out.noteOn(0, 22, 127)};
				if(i==15){mmx_out.noteOn(0, 24, 127)};

			} {
				mmx_out.noteOn(0, (i*3+1)%22, 0);
				if(i==7){mmx_out.noteOn(0, 22, 0)};
				if(i==15){mmx_out.noteOn(0, 24, 0)};
			}
		}
	}


	load {
		arg ... values;

		var knobs = values.copyRange(0,23);
		var buttons = values.copyRange(24,39);
		var sliders = values.copyRange(40,47);

		cc_button.do({arg item; item.valueAction = 0});

		knobs.do({ arg item, i;
			defer{cc_knob[i].valueAction = item};
		});
		buttons.do({ arg item, i;
			if(item!=0){defer{cc_button[i].valueAction = item}};
		});
		sliders.do({ arg item, i;
			defer{cc_slider[i].valueAction = item};
		});
	}

	loadKnobs {
		arg ... values;

		var knobs = values.copyRange(0,23);

		knobs.do({ arg item, i;
			defer{cc_knob[i].valueAction = item};
		});
	}

	loadFaders {
		arg ... values;

		var faders = values.copyRange(0,7);

		faders.do({ arg item, i;
			defer{cc_slider[i].valueAction = item};
		});
	}

	multiply {
		{
			8.do({ arg item, i;
				eqSum.setAt(i, (
					eq.subBus((i)).getSynchronous *
					eq.subBus((i+8)).getSynchronous *
					eq.subBus((i+16)).getSynchronous).postln
				);
			});
		}.defer(0.1)
	}

	loadEQ {
		arg ... values;

		var knobs = values.copyRange(0,23);

		//mmx_out.noteOn(0, 25, 127);
		//mmx_out.noteOn(0, 26, 0);
		this.setGate(0);

		knobs.do({ arg item, i;
			defer{cc_knob[i].valueAction = item};
		});

		this.setGate(1);
		//mmx_out.noteOn(0, 25, 0);
		//mmx_out.noteOn(0, 26, 127);

		defer{24.do({arg i; cc_knob[i].centered_(false).value= this.knobs.subBus(i).getSynchronous})};
	}

	dump {
		cc_knob.do({ arg item, i;
			item.value.trunc(0.01).post;
			",".post;
		}); "".postln;
		cc_button.do({ arg item, i;
			item.value.trunc(0.01).post;
			",".post;
		}); "".postln;
		cc_slider.do({ arg item, i;
			item.value.trunc(0.01).post;
			",".post;
		}); "".postln;
	}

	writeStateToFile {
		var dir = thisProcess.platform.recordingsDir ++ "/mmx";
		var timestamp = Date.localtime.stamp;
		var path = dir +/+ "mmx_" ++ timestamp ++ "." ++ "txt";
		var file = File(path,"w");

		file.write(knobsMapArray.asString.replace("[ ",""));

		cc_knob.do({ arg item, i;
			file.write(item.value.trunc(0.01).asString);
			file.write(",");
		});
		cc_button.do({ arg item, i;
			file.write(item.value.trunc(0.01).asString);
			file.write(",");
		});
		cc_slider.do({ arg item, i;
			file.write(item.value.trunc(0.01).asString);
			file.write(",");
		});

		file.close;
	}

	readStateFromFile {
		arg path;
		var file = File(path ++ ".txt","r");
		var string = file.readAllString;
		var arr = string.split($]);
		var setKn = arr[0].split($,);
		var values = arr[1].split($,);
		var knobs, buttons, sliders;
		setKn = setKn.collect({arg item; item.asFloat});
		values = values.collect({arg item; item.asFloat});
		file.close;
		setKn.size.do({ arg i;
			knobsMapArray[i]= setKn[i];
		});
		this.knobsMapping;
		this.numBoxUpdate;

		knobs = values.copyRange(0,23);
		buttons = values.copyRange(24,39);
		sliders = values.copyRange(40,47);

		cc_button.do({arg item; item.valueAction = 0});

		knobs.do({ arg item, i;
			defer{cc_knob[i].valueAction = item};
		});
		buttons.do({ arg item, i;
			if(item!=0){defer{cc_button[i].valueAction = item}};
		});
		sliders.do({ arg item, i;
			defer{cc_slider[i].valueAction = item};
		});
	}

	setGate {
		arg newGate;
		gate = newGate;
	}

	makePath {
		var dir = thisProcess.platform.recordingsDir ++ "/mmx";
		var timestamp = Date.localtime.stamp;
		^dir +/+ "mmx_" ++ timestamp ++ "." ++ Server.local.recHeaderFormat;
	}

	prepare {
		var path = this.makePath;
		buf = Buffer.alloc(Server.local, 65536, 2);
		// create an output file for this buffer, leave it open
		buf.write(path, "aiff", "int16", 0, 0, true);
	}

	rec {
		this.prepare;
		this.writeStateToFile;
		recSynth = Synth.tail(nil, \mmx_record, [\bufnum, buf.bufnum, \bus, mmrec.index]);
	}

	stopRec {
		recSynth.free;
		buf.close;
		buf.free;
	}

	playback {
		arg path;
		this.readStateFromFile(path);
		buff = Buffer.read(Server.local, path ++ ".aiff");
		osc = OSCFunc({
			arg msg;
			var index = msg[3]*100;
			var val = msg[4].asFloat.postln;
			index = index.round.asInt;
			if(index<24){
				defer{cc_knob[index].valueAction=val}
			}{
				if(index<40){
					defer{cc_button[index-24].valueAction=val}
				}{
					defer{cc_slider[index-40].valueAction=(val)}
				}
			}
		}, '/pb');
		playb = Synth(\mmx_pb, [bufnum: buf]);
	}

	stopPlayback {
		osc.free;
		playb.free;
		buff.free;
	}

	channelStrip {
		arg in = 20, out = 0, n = 2, strip = 0, mindB = -120, maxdB = -30;
		var eqSynth = n.collect(0);
		var osc, node = 0, task, rms = 0.05;

		var eq = {
			n.collect({
				arg i;
				eqSynth[i] = Synth(\channelStrip, [
					\in, in+i,
					\out, out+i,
					\lo, this.knobs.subBus(strip+16).asMap,
					\mid, this.knobs.subBus(strip+8).asMap,
					\hi, this.knobs.subBus(strip).asMap,
					\amp, this.faders.subBus(strip).asMap,
				], addAction: 'addToTail');
			});
			node = eqSynth[0].nodeID.postln;
		};

		var eqFunc = {{eq.value}.defer(0.01)};

		this.buttonsOn[strip]= {
			var tskk;
			eqFunc.value;
			osc = OSCFunc({ arg msg, time; msg; if (msg[1]==node) {rms = msg[3]}},'/tr', Server.default.addr).permanent_(true);
			task = Task({ { this.flashQ(strip+8); rms.linexp(0,1,1,0.05).pow(2).max(0.01).wait;}.loop });
			task.start;
			tskk = {var task = Task({ { this.flashQ(strip+8); rms.explin(mindB.dbamp,maxdB.dbamp,2,0.05).max(0.01).wait;}.loop });task.start;};
			CmdPeriod.add(eqFunc);
			CmdPeriod.add(tskk);
		};

		this.buttonsOff[strip]= {
			2.do({arg i; eqSynth[i].free});
			CmdPeriod.remove(eqFunc);
			task.stop;
			osc.free;
		};

		cc_numBox[strip*2].valueAction = -40;
		cc_numBox[strip*2+1].valueAction = 40;
		cc_numBox[strip*2+16].valueAction = -40;
		cc_numBox[strip*2+17].valueAction = 40;
		cc_numBox[strip*2+32].valueAction = -40;
		cc_numBox[strip*2+33].valueAction = 40;
	}
}