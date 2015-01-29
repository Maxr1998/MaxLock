About
=====
How to use this module:
-----------------------
1. Enable in Xposed Installer and reboot if you haven't done already
2. Choose global locking method and password in *Locking type → ...*
3. Choose the apps you want to have locked in *Choose apps*

Optional tips:

- After enabling an app, a orange pencil appears next to toggle button, click it for further options
- Fake die is a feature which spoofs the user that an app died. To launch it, you have to click *Report* in the dialog and enter the phrase specified in *Locking options → Fake die input*, the default one is *start*
- You can also set a custom locking type and password on app basis

Help and tips:
--------------
- Long pressing a knock button also clears current input, useful if you hid the input bar
- Same works for PIN keys
- On Gingerbread, you need a Task Manager (ES Taskmanager if you don't have one) to relock the apps
- Sometimes, MasterSwitch doesn't want to relock. In this case, just force close the apps not relocking in App info and everything is fine again

Thanks to:
----------
- @FatMinMin for original module
- @TechnoSparks for general support and new ideas
- @badkill for Spanish translation
- @liveasx for Chinese translation
- @t61p for Japanese translation
- @mihahn for parts of the German translation
- ? for persian translation (please send me a message, I forgot your name :/)

Libs and code:
--------------
- [AndDown Markdown reader by Commonsware](https://github.com/commonsguy/cwac-anddown)
- [FabView by Sergej Shafarenka, halfbit.de](https://github.com/beworker/fabuless)
- [PreferenceFragment lib for below API 11 by kolavar](https://github.com/kolavar/android-support-v4-preferencefragment)
- [StatusBarTintApi by Mohammad Abu-Garbeyyeh](https://github.com/MohammadAG/Xposed-Tinted-Status-Bar/blob/master/src/com/mohammadag/colouredstatusbar/StatusBarTintApi.java)
- [sha1/256Hash method by Adam on stackoverflow.com](http://stackoverflow.com/a/11978976)
- A massive amount of other stackoverflow answers, thank you very much!