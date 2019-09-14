/*
Colliding, a simple SuperCollider environment for learning and live coding
(c) Gerard Roma, 2013-2019

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

CollidingTrack{
	var <>str, <>synth,<playing = false,compiled;
	var parsed = false;
	var spec,<>name,<vol;
	var <>tab;

	*new{^super.new.init}

    init{
	  synth = NodeProxy.new;
	  synth.fadeTime_(0.2);
	  spec = \db.asSpec;
   }

   parse{|str|
		str = str.compile;
	    ^str;
   }
   playStr{|str|
		synth.source=str;
		synth.play;
		if(playing.not){
			playing=true;
		}
	}

	play{|name|
		synth = Synth(name,[\key,60,\freq,400,\amp,0.5,\gate,1]);
	}

	playNote{|name, note, velocity|
		synth = Synth(name,[\key,note,\freq,note.midicps,\amp,velocity/127.0,\gate,1]);
		^synth;
	}

	stop{
		synth.free;
	}

    vol_{|val|
		synth.set(\amp,spec.map(val).dbamp);
	}

	makeDefStr{|str, name, play|
		var defStr;
		var ampVal = spec.map(this.tab.volumeSlider.value).dbamp;
		defStr = "SynthDef('"++name.asSymbol++"', {|key=60,freq=400,gate=0,amp="++ampVal++ "| ";
		if(play){
		 defStr = defStr +str+" }).play(Server.internal); ";
		}{
		 defStr = defStr +str+" }).send(Server.internal); ";
		};
		^defStr;
	}

	makeDef{|defStr, name, play = false|
		var def = defStr.compile.value;
		if(play){synth.free;synth=def};
		this.name = name;
		^def;
	}
}