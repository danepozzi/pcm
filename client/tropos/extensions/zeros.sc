FaustZeros : UGen
{
  *ar { | in1, in2, in3, in4, in5, in6 |
      ^this.multiNew('audio', in1, in2, in3, in4, in5, in6)
  }

  *kr { | in1, in2, in3, in4, in5, in6 |
      ^this.multiNew('control', in1, in2, in3, in4, in5, in6)
  } 

  checkInputs {
    if (rate == 'audio', {
      6.do({|i|
        if (inputs.at(i).rate != 'audio', {
          ^(" input at index " + i + "(" + inputs.at(i) + 
            ") is not audio rate");
        });
      });
    });
    ^this.checkValidInputs
  }

  name { ^"FaustZeros" }


  info { ^"Generated with Faust" }
}

