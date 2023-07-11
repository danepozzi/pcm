var tax = File(thisProcess.nowExecutingPath.dirname +/+ "mmx/220707/220707_taxonomy.scd", "r");
var string = tax.readAllString;
var concatenate = "[ " ++ string ++ " ]";
~taxonomy = concatenate.interpret;
("taxonomy contains " ++ ~taxonomy.size ++ " snapshots").postln;