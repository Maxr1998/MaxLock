MaxLock
=======
This is an Xposed Framework App lock module.

Links:
------
+ [Official MaxLock Website](http://maxlock.nfshost.com/)
+ [XDA Forum Thread](http://forum.xda-developers.com/xposed/modules/app-maxlock-applock-alternative-t2883624/post55583623)
+ [Google Play Page](https://play.google.com/store/apps/details?id=de.Maxr1998.xposed.maxlock)
+ [Xposed Repo](http://repo.xposed.info/module/de.maxr1998.xposed.maxlock)
+ [Changelog](https://github.com/Maxr1998/MaxLock/wiki/Changelog)
+ [Source code](https://github.com/Maxr1998/MaxLock)


Changelog (Temporary place)
===========================

###5.2 (WIP)###
- Apps now always relock on pausing, no need to clear from recents
- Tasker support
- improved I.MoD

###5.1.1###
- Fix crash at startup
- Fix a crash in app list
- added Mini fake die, which leads you to the unlock screen directly after clicking report in fake die dialog
- Fix widget not clickable
- Fix unlimited loading in Guide when no network is available

###5.1###
**You need to reboot and launch MaxLock main app at least once after updating!**
- delayed relock: Don't ask for password for a certain amount of time after last unlock, note that apps still get relocked **only after being cleared from recents!** Thanks to @Intika for creating that feature!
- ability to exclude activities from lock (WhatsApp Calls users: exclude com.whatsapp.VoipActivity!)
- Official MaxLock guide by @TechnoSparks
- more Material design (in app list, new launcher icons)
- updated Simplified Chinese and Japanese translation, added Traditional Chinese translation (Thanks @alexking99)
- MANY under-the-hood changes, fixes and improvements, updated all libs, improved MaxLock Pro handling
- added a MasterSwitch shortcut to launch directly (only visible in Activity browser like Tasker or Nova Launcher), in addition to the widget and the launcher shortcut

###5.0.3###
- improved MasterSwitch widget, now resizable (1x1, 1x2, 2x3...)
- Quick unlock now also applies for password
- Passwords consisting of numbers only now show number keyboard (reapplying password required)
- fixed MasterSwitch shortcut, works for all apps now (not only launcher)
- updated and fixed the in-app billing library, improved recognition of MaxLock Pro
- Lollipop and KitKat style improvements


###5.0.2.1###
- Small fix

###5.0.2###
**REBOOT REQUIRED after this update!**
- added MasterSwitch widget instead of shortcut
- improved app launching time
- Chinese translation update by @liveasx
- new app icons
- fixed Snackbar not hidden (continue button in Knock code setup invisible)

###5.0.1###
- Japanese translation update by @t61p
- more prominent info screen
- fixes

###5.0 Celebrating Xposed Lollipop compatibility!###
- added Pattern lock
- color as lockscreen background
- hide app icon option
- package (un)installer can now be locked
- improved how-to, included licenses
- fixed tablet layout
- few Lollipop design tweaks
- other fixes

###4.0.4###
- crash fix in lockscreen

###4.0.3###
- bugfixes
- custom-app password type knock code now shows correct layout (you have to set password type and password again for apps using knock code)

###4.0.2###
- many bugfixes
- (Write Settings permission is needed to get system-wallpaper on some Samsung devices)
- Japanese translation by @t61p

###4.0.1###
- feature to protect app from being uninstalled
- fixes

###4.0###
- Premium features:
** backup/restore/clear packages list
** log failed unlock attempts
- translation updates
- fixed MasterSwitch shortcut
- many other fixes
- decreased app size

###3.3: Christmas update!###
- added per app password and locking type
- rewritten lock ui
- improved MasterSwitch, again
- many many other bug and layout fixes, check my git ;)
- changed to language level 1.7 and other build environment fixes

###3.2.1###
- support for Android 2.3
- workaround for wallpaper bug
- Chinese translation by @liveasx

###3.2###
- added PIN locking type
- added MasterSwitch shortcut
- added about screen
- added landscape layout for knockcode
- reordered preferences as suggested by @TechnoSparks ;)
- better filter method in appslist
- translation updates
- LOTS of fixes


###3.1.1###
- fixed crash

###3.1###
- fixed Intent extras problem: Yippee-ki-yay, motherf*cker
- MasterSwitch: Welcome back!
- fixed using other background types than custom
- added default input for Fake die

###3.0.1###
- fixed crash on API 15
- fixed password locking type
- fixed dark app theme
- new launcher icon (but not finished yet)

###3.0###
- Material Design!
- multipane preferences on tablets
- new UI options, e.g. black text color, hide app name, hide input box
- custom background images
- fixed wallpaper bug
- SHA-256 encryption
- translation updates
- updated all the deprecated stuff
- lots of other fixes, cleanup
- temporarily disabled master switch because of bugs

###2.1.1###
- fixes

###2.1###
- added master switch
- added better fake die feature: to open app, click on "report" in fc dialog, then enter "start" and click OK to open password entry
- option to use dark style
- option to hide dividers and knock button highlight
- updated German translation by @mihahn
- fixes

###2.0.1###
- changed package-name
- added Spanish translation from @badkill, thanks!

###2.0: Initial re-release with new features###
- new Settings UI
- Knock code lock type
- added German translation from @mihahn, thanks!
- encrypted passwords