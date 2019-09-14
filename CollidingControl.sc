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

CollidingControl{
    var ctlKeys;
    var <>view;
    var colorIsSet;
    var lineIsNew;
	var synths;
    var <app;

	*new{|app, player|
		^super.new.init(app)
	}

	init{|anApp|
		app = anApp;
		this.initMIDI;
	}

	initMIDI{
		MIDIClient.init;
		MIDIIn.connectAll;
		synths = Dictionary.new;
		MIDIFunc.noteOn({|vel, key|
			var tab = app.gui.tabs[app.gui.tabView.activeTab.index];
			var name = "colliding_" ++ app.id ++ "_" ++ tab.id;
			synths[key] = tab.track.playNote(name, key, vel);
		});
		MIDIFunc.noteOff({|vel, key|
			synths[key].set(\gate, 0);
		});
	}


	tabKeyDown{|tab, view, char, mod, unicode, keycode|

		var str;
		var cmdK,returnK,backspaceK;
		Platform.case( // TODO: linux
			\osx,{cmdK = 1048576; returnK = 36; backspaceK = 51},
			\windows,{ cmdK = 262144; returnK = 13; backspaceK = 8},
			\linux,{ cmdK = 262144; returnK = 36; backspaceK = 22}
		);

		if(mod==cmdK){
			keycode.switch(
				returnK,{ this.playTrack(tab,view)},
				backspaceK,{ this.stopTrack(tab,view)}
			);
		};

		if(mod==2228224){
			keycode.switch(
				126, {10.do({tab.volumeSlider.increment});
					tab.volumeSlider.action.value},
				125,{10.do({tab.volumeSlider.decrement});
					tab.volumeSlider.action.value;
				}
			);
		};

		if((32..127).indexOf(unicode).notNil)
		    {view.stringColor_(Color.grey)};
	}

	playTrack{|tab, view|
		if(app.mode == 0){// synthdef
			this.compile(tab, view, true);
		}{ // nodeproxy
			var parsed = tab.track.parse(view.string);
			if (parsed.notNil){
				view.stringColor_(Color.white);
				tab.track.playStr(parsed);
				tab.state = 1;
			}{
				view.stringColor_(Color.red);
			}
		};
		app.gui.updateScope;
	}

	stopTrack{|tab, view|
		tab.state = 0;
		tab.track.stop;
	}

    compile{|tab, view, play = false|
		var def;
		var name = "colliding_" ++ app.id ++ "_" ++ tab.id;
		var defStr = tab.track.makeDefStr(tab.textView.string, name, play);
		tab.post("compiling");

		try{
			def = tab.track.makeDef(defStr, name, play);
		};

		if (def.notNil){
			view.stringColor_(Color.white);
			view.syntaxColorize; // hopefully some day
			tab.post("OK");
			if(play){
				tab.state = 1;
			}
		}{
			view.stringColor_(Color.red);
			tab.post("ERROR");
			this.postErr(name, defStr, tab, view);
		};
		//app.gui.updateScope;
		^def.notNil;
	}

	postErr{|name, str, tab, view|
		var def, tmpFile, tmpFileName, cmdStr, resultLines, lineNum;
		tmpFileName = Platform.defaultTempDir ++ name;
		tmpFile = File.use(tmpFileName,"w+", {|f|
			f.write("protect{\"" ++ str ++ "\".compile.value}{0.exit}")});
		cmdStr = app.sclangPath ++ " " ++ tmpFileName;
		resultLines = cmdStr.unixCmdGetStdOutLines;

		resultLines.do({|line|
			if(line.beginsWith("ERROR")){
				if(line!="ERROR: Command line parse failed"){
					tab.post(line);
				}
			};
			if("char".matchRegexp(line)){
				var lineStart, lineEnd, breaks;
				lineNum = line.split($ )[3].asInt;
				breaks = view.string.findAll("\n");
				lineStart = breaks[lineNum - 2];
				if (lineNum > breaks.size){
					lineEnd = view.string.size - lineStart;
				}{
					lineEnd =  breaks[lineNum-1] - lineStart;
				};
				view.setStringColor(Color.new255(255, 140, 0), lineStart, lineEnd);
			}
		});
	}

	qwertyButton{|tab|
		if (this.compile(tab,tab.textView)){
			var qwerty = Qwerty.new(("colliding_" ++ app.id ++ "_" ++ tab.id).asSymbol);
			// TODO
			//tab.state = 1;
			//qwerty.win.onClose={tab.state = tab.prevState};
		}
	}

	trackVolume{|tab, slider|
		tab.track.vol_(slider.value);
	}

	helpButton{|tab|
		var selected = tab.textView.selectedString;
		HelpBrowser.front.goTo(SCDoc.findHelpFile(selected));
	}
	removeTab{|tab|
		var baseName = app.projectPath ++ tab.id.asString;
		if(File.exists(baseName ++ ".cld")){
			File.copy(baseName ++ ".cld",baseName ++ ".deleted");
			File.delete(baseName ++ ".cld");
		};
		app.gui.tabs.remove(tab);
	}


	saveBuffers{|path|
		app.sounds.do({|f, i|
			if(f.notNil){
				var fname = f.path.split($/).pop;
				var bufPath,filePath;
				bufPath=path ++ "/" ++ i;
				bufPath.mkdir;
				filePath = bufPath ++ "/" ++ fname;
				app.buffers[i].write(filePath);
			}
		});
	}

	loadBuffers{|folder|
		PathName(folder ++ "audio/").folders.do({|f|
			if("^[0-7]$".matchRegexp(f.folderName)){
				app.loadBuffer(f.folderName.asInt, f.files[0].fullPath,{})
			}
		});
	}

	saveProject{
		if(app.projectPath.isNil){
			FileDialog.new({|path|
				path.mkdir;
				path = path ++ "/";
				(path ++ "audio").mkdir;

				app.gui.tabs.do({|tab|
					tab.save(path)}
				);
				app.projectPath = path;
				this.saveBuffers(app.projectPath ++ "audio");
			}, fileMode:2, acceptMode:1, stripResult:true);
		}{
			app.gui.tabs.do({|tab|tab.save(this.app.projectPath)});
			this.saveBuffers(app.projectPath ++ "audio");
			app.post("Saved " + PathName(app.projectPath).folderName);
		};
	}

	loadProject{
		if(app.projectPath.isNil){
			var content = app.gui.tabs.collect({|tab|tab.textView.string.size}).sum;
			if(content == 0){
				FileDialog.new({|p|
					this.load(p);
				}, fileMode:2, acceptMode:0, stripResult:true);
			}{
			app.post("Already did some work");
			}
		}{
			app.post("Already loaded a project");
		}
	}

	load{|folder|
		var textFiles;
		app.projectPath_(folder);
		app.gui.removeFirst;
		app.tabId = 0;
		textFiles = PathName(folder).files.select(
			{|x| x.fullPath.endsWith(".cld")}
		);

		if(textFiles.size > 0){
			textFiles.do({|f|
				var text,data,tab,id;
				data = File.open(f.fullPath,"r");
				text = data.readAllString;
				id = f.fileNameWithoutExtension.asInt;
				if(id > app.tabId){ app.tabId=id };
				tab = app.gui.newTab(id);
				tab.textView.string=text;
			});
		};
		this.loadBuffers(folder);
	}
}
