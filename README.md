![MaxLock (Logo)](http://i.imgur.com/wxNJX7O.png?1)

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
- **Apps now always relock on pausing, no need to clear from recents**
- Knock Code now with new relative button styled input like original LG KNOCKcode
- Tasker support
- Option to filter apps on activated state in Apps list
- Make Tablet mode toggleable for people with DPI mods ;)
- Added a first start screen
- **Removed Gingerbread support**
- Updated translations for Chinese (both) and Japanese
- Improved size of app
- Improved fluidity of I.MoD
- Fixed bugs and improved app experience
- Updated libraries

###5.1.1###
- Fixed crash at startup
- Fixed a crash in app list
- Added Mini fake die, which leads you to the unlock screen directly after clicking report in fake die dialog
- Fixed widget not clickable
- Fixed unlimited loading in Guide when no network is available

###5.1###
**You need to reboot and launch MaxLock main app at least once after updating!**
- Delayed relock: Don't ask for password for a certain amount of time after last unlock, note that apps still get relocked **only after being cleared from recents!** Thanks to @Intika for creating that feature!
- Ability to exclude activities from lock (WhatsApp Calls users: exclude com.whatsapp.VoipActivity!)
- Official MaxLock guide by @TechnoSparks
- More Material design (in app list, new launcher icons)
- Updated Simplified Chinese and Japanese translation, added Traditional Chinese translation (Thanks @alexking99)
- MANY under-the-hood changes, fixes and improvements, updated all libs, improved MaxLock Pro handling
- Added a MasterSwitch shortcut to launch directly (only visible in Activity browser like Tasker or Nova Launcher), in addition to the widget and the launcher shortcut

###5.0.3###
- Improved MasterSwitch widget, now resizable (1x1, 1x2, 2x3...)
- Quick unlock now also applies for password
- Passwords consisting of numbers only now show number keyboard (reapplying password required)
- Fixed MasterSwitch shortcut, works for all apps now (not only launcher)
- Updated and fixed the in-app billing library, improved recognition of MaxLock Pro
- Lollipop and KitKat style improvements


###5.0.2.1###
- Small fix

###5.0.2###
**REBOOT REQUIRED after this update!**
- Added MasterSwitch widget instead of shortcut
- Improved app launching time
- Chinese translation update by @liveasx
- New app icons
- Fixed Snackbar not hidden (continue button in Knock code setup invisible)

###5.0.1###
- Japanese translation update by @t61p
- More prominent info screen
- Fixes

###5.0 Celebrating Xposed Lollipop compatibility!###
- Added Pattern lock
- Custom Color as lockscreen background
- Hide app icon option
- Package Installer can now be locked
- Improved how-to, included licenses
- Fixed tablet layout
- Few Lollipop design tweaks
- Other fixes

###4.0.4###
- Crash fix in lockscreen

###4.0.3###
- Bugfixes
- Custom-app password type knock code now shows correct layout (you have to set password type and password again for apps using knock code)

###4.0.2###
- Many bugfixes
- (Write Settings permission is needed to get system-wallpaper on some Samsung devices)
- Japanese translation by @t61p

###4.0.1###
- Feature to protect app from being uninstalled
- Fixes

###4.0###
- Premium features:
** backup/restore/clear packages list
** log failed unlock attempts
- Translation updates
- Fixed MasterSwitch shortcut
- Many other fixes
- Decreased app size

###3.3: Christmas update!###
- Added per app password and locking type
- Rewritten lock ui
- Improved MasterSwitch, again
- Many many other bug and layout fixes, check my git ;)
- Changed to language level 1.7 and other build environment fixes

###3.2.1###
- Support for Android 2.3
- Workaround for wallpaper bug
- Chinese translation by @liveasx

###3.2###
- Added PIN locking type
- Added MasterSwitch shortcut
- Added about screen
- Added landscape layout for knockcode
- Reordered preferences as suggested by @TechnoSparks ;)
- Better filter method in appslist
- Translation updates
- LOTS of fixes


###3.1.1###
- Fixed crash

###3.1###
- Fixed Intent extras problem: Yippee-ki-yay, motherf*cker
- MasterSwitch: Welcome back!
- Fixed using other background types than custom
- Added default input for Fake die

###3.0.1###
- Fixed crash on API 15
- Fixed password locking type
- Fixed dark app theme
- New launcher icon (but not finished yet)

###3.0###
- Material Design!
- Multipane preferences on tablets
- New UI options, e.g. black text color, hide app name, hide input box
- Custom background images
- Fixed wallpaper bug
- SHA-256 encryption
- Translation updates
- Updated all the deprecated stuff
- Lots of other fixes, cleanup
- Temporarily disabled master switch because of bugs

###2.1.1###
- Fixes

###2.1###
- Added master switch
- Added better fake die feature: to open app, click on "report" in fc dialog, then enter "start" and click OK to open password entry
- Option to use dark style
- Option to hide dividers and knock button highlight
- Updated German translation by @mihahn
- Fixes

###2.0.1###
- Changed package-name
- Added Spanish translation from @badkill, thanks!

###2.0: Initial re-release with new features###
- New Settings UI
- Knock code lock type
- Added German translation from @mihahn, thanks!
- Encrypted passwords