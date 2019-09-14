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

CollidingConfig{

	*new{|configFile| ^super.new.init(configFile)}

	defaults{
		^Dictionary[
			"useNodeProxy"-> false,
			"freesoundKey"-> "",
			"showSliders"-> true,
			"sclangPath"-> ""
		]
	}

	init{|configFile|
		var dict = Dictionary.new;
		configFile = configFile ? "~/.colliding";
		if (File.exists(configFile.standardizePath)){
			dict = configFile.standardizePath.parseYAMLFile;
			if(dict.isNil){"Invalid config file".throw}
		};
		dict = this.defaults ++ dict;
		dict.keysDo{|k|
			this.addUniqueMethod(k.asSymbol, {
				switch(dict[k],"true",true, "false", false,dict[k]);
			});
		};
   }
}