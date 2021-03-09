Macro Recorder
=============
------------------------------------------
Latest Version: [v.1.2.2][releases]

A simple and lightweight global mouse and keyboard recorder and player. The recordings log the time between actions to simulate realistic playback when playing a recording. 

Mouse clicks and keystrokes are recorded as presses and releases, allowing simulation of dragging the mouse or holding down a key. This allows for commands that involve a combination of keys.
<br></br>

Library
----
[JNativeHook][home] ([v1.2.0-Beta2][git]) distributed under the GNU GPL
<br></br>

Requirements
----
*Only tested on 64-Bit Windows 7 with Lenono T430s keyboard. Should work with most North American keyboards.*

- JDK 1.7
<br></br>

Use
----
Download latest [release][releases]. Open cmd and go to directory containing the .jar file. Type java -jar [releaseName].jar
(OR, Download the source zip and run MainProgram.java if you want the sweet icons)

For recording, type in the text field for desired output filename. Files are saved in the directory of the programs file browser. Press record when ready.

Player and recording can be interrupted at any time by pressing and releasing ESC.

You can add "-play macro.txt" in order to automatically start a macro when the software is opened.

Known Issues
----
You cannot hold down more than two keys and have it function properly (e.g. Ctrl+Alt+Del won't work). This is due JNativeHook's listeners.

The program will not work if opened by double-clicking the jar.

[home]:https://code.google.com/p/jnativehook/
[git]:https://github.com/kwhat/jnativehook/releases
[releases]:https://github.com/8bitFra/macroRecorder/releases
    
