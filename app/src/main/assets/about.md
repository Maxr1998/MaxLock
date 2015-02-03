About MaxLock: READ THIS!
=========================
How to use this module:
-----------------------
1. Enable in Xposed Installer and reboot if you haven't done already
2. Choose global locking method and password in *Locking type → ...*
3. Choose the apps you want to have locked in *Choose apps*

Help and tips:
--------------
- Apps get relocked by being cleared from recents, I can't and won't add another method to relock apps
- You can also relock apps with [Tasker](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm) and [Recent App Cleaner](https://play.google.com/store/apps/details?id=com.droidmate.rcleaner), thanks to Krisztian for pointing that out.
- On Gingerbread, you need a Task Manager (ES Taskmanager if you don't have one) to relock the apps
- Alternatively, most custom roms allow to set special actions to long clicking navigation keys, one preconfigured is "close app". Now, when long clicking e.g. back button, the apps gets fully closed and has to be unlocked on next start
- After enabling an app, a orange pencil appears next to toggle button, click it for a dialog with further options
- Fake die is a feature which spoofs the user that an app died. To launch it, you have to click *Report* in the dialog and enter the phrase specified in *Locking options → Fake die input*, the default one is *start*
- You can also set a custom locking type and password on app basis
- Long pressing a knock button also clears current input, useful if you hid the input bar
- Same works for PIN keys
- Lock Package-Installer and Google Play Store to forbid uninstalling of apps
- [PREMIUM] Long pressing a backup item in the restore list dialog from app list deletes it

Known Issues:
-------------
- Apps do not relock automatically: refer to first tip, this is not an issue!
- MasterSwitch doesn't want to relock: In this case, just force close the apps not relocking in App info and everything is fine again
- Package-Installer sometimes doesn't ask for password on uninstallation: I found out that when you unlocked Package-Installer but then clicked cancel in uninstall dialog, the Package-Installer doesn't ask for password anymore. You can fix this by rebooting or uninstalling an app via Package-Installer

Thanks to:
----------
- @FatMinMin for original module
- @TechnoSparks for general support and new ideas
- @badkill for Spanish translation
- @liveasx for Chinese translation
- @t61p for Japanese translation
- @mihahn for parts of the German translation
- ? for persian translation (please send me a message, I forgot your name :/)

Links:
------
- [Google Play Store page](https://play.google.com/store/apps/details?id=de.Maxr1998.xposed.maxlock)
- [Xposed-Repo](http://repo.xposed.info/module/de.maxr1998.xposed.maxlock)
- [XDA-Thread](http://forum.xda-developers.com/xposed/modules/app-maxlock-applock-alternative-t2883624)
- [GitHub repository](https://github.com/Maxr1998/MaxLock)

Libs and code used:
-------------------
- [android-inapp-billing-v3 by anjlab](https://github.com/anjlab/android-inapp-billing-v3)
- [android-lockpattern by Hai Bison (Modified by me)](https://code.google.com/p/android-lockpattern/)
- [android-support-v4-preferencefragment by kolavar](https://github.com/kolavar/android-support-v4-preferencefragment)
- [Apache Commons IO](http://commons.apache.org/proper/commons-io/)
- [CWAC AndDown Markdown reader by Commonsware](https://github.com/commonsguy/cwac-anddown)
- [FabView by Sergej Shafarenka, halfbit.de](https://github.com/beworker/fabuless)
- [Google Guava](https://github.com/google/guava)
- [StatusBarTintApi by Mohammad Abu-Garbeyyeh](https://github.com/MohammadAG/Xposed-Tinted-Status-Bar/blob/master/src/com/mohammadag/colouredstatusbar/StatusBarTintApi.java)
- [sha1/256Hash method by Adam on stackoverflow.com](http://stackoverflow.com/a/11978976)
- [XposedBridge by rovo89 and Tungstwenty](https://github.com/rovo89/XposedBridge)
- A massive amount of stackoverflow answers, thank you very much!

***

Licenses:
---------
###MaxLock###
Licensed under the [GNU GPL License Version 3](http://www.gnu.org/licenses/gpl-3.0.txt) or any later version.

###android-inapp-billing-v3, android-lockpattern, android-support-v4-preferencefragment, Apache Commons IO, CWAC AndDown, FabView, Google Guava, XposedBridge###
Licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).