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


Colliding{
	classvar <>textFont;
	classvar <>guiFont;
	classvar <>symbolFont;

	classvar <instanceId = 0;
	classvar <max_tabs = 8;

	var <id,>tabId;
    var <>gui, <>server, <controller;
	var <>buffers, <>sounds;
	var <projectPath;
    var <mode = 0;
	var <sclangPath;

	*new{|mode = 0,freesound_key = nil|
		^super.new.init(mode,freesound_key);
    }

	tabId{
		tabId = tabId + 1;
		^tabId;
	}

    init{|aMode,aFreesoundKey|
		textFont = Font("Verdana", 24);
	    guiFont = Font("Arial", 12);
	    symbolFont = Font("Arial", 28);
		mode = aMode;
		tabId = 0;
		instanceId = instanceId + 1;
		id = instanceId;
        server = Server.internal;
        Server.default = Server.internal;
		server.waitForBoot({gui.makeScopeDef});
		buffers = Array.fill(8);
		sounds = Array.fill(8);
        gui = CollidingGUI.new.init(this);
		sclangPath = Platform.resourceDir++
		             Platform.pathSeparator;
		if (thisProcess.platform.name==\osx){
				sclangPath = sclangPath ++"../MacOS/sclang";
		}{
			sclangPath = sclangPath ++ "sclang";
		};
		if(aFreesoundKey.notNil){Freesound.token = aFreesoundKey};
    }


	post{|str|
		gui.tabs[gui.tabView.activeTab.index].post(str);
	}

	loadBuffer{|idx,path,doneFunc|
		sounds[idx] = SoundFile.openRead(path);
		if (sounds[idx].notNil){
			buffers[idx] = Buffer.read(server, path, bufnum:idx, action:doneFunc);
		}{
			"Could not read file".postln;
		}
	}

	projectPath_{|path|
		gui.win.name = PathName(path).folderName;
		projectPath = path;
	}
}

