About MaxLock: READ THIS!
=========================
How to use this module:
-----------------------
1. Enable in Xposed Installer and reboot if you haven't done already
2. Choose global locking method and password in *Locking type → ...*
3. Choose the apps you want to have locked in *Choose apps*

Help and tips:
--------------
- Apps get relocked by being cleared from recents, I can't and won't include another possibility to relock apps
- You can relock apps with [Tasker](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm) and [Recent App Cleaner](https://play.google.com/store/apps/details?id=com.droidmate.rcleaner), thanks to Krisztian for pointing that out.
- On Gingerbread, you need a Task Manager (ES Taskmanager if you don't have one) to relock the apps
- Sometimes, MasterSwitch doesn't want to relock. In this case, just force close the apps not relocking in App info and everything is fine again
- After enabling an app, a orange pencil appears next to toggle button, click it for a dialog with further options
- Fake die is a feature which spoofs the user that an app died. To launch it, you have to click *Report* in the dialog and enter the phrase specified in *Locking options → Fake die input*, the default one is *start*
- You can also set a custom locking type and password on app basis
- Long pressing a knock button also clears current input, useful if you hid the input bar
- Same works for PIN keys
- [PREMIUM] Long pressing a backup item in the restore list dialog from app list deletes it

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
- [AndDown Markdown reader by Commonsware](https://github.com/commonsguy/cwac-anddown)
- [android-lockpattern by Hai Bison](https://code.google.com/p/android-lockpattern/)
- [FabView by Sergej Shafarenka, halfbit.de](https://github.com/beworker/fabuless)
- [PreferenceFragment lib for below API 11 by kolavar](https://github.com/kolavar/android-support-v4-preferencefragment)
- [StatusBarTintApi by Mohammad Abu-Garbeyyeh](https://github.com/MohammadAG/Xposed-Tinted-Status-Bar/blob/master/src/com/mohammadag/colouredstatusbar/StatusBarTintApi.java)
- [sha1/256Hash method by Adam on stackoverflow.com](http://stackoverflow.com/a/11978976)
- A massive amount of other stackoverflow answers, thank you very much!

Licenses:
---------
###MaxLock###
Copyright (C) 2014-2015  Maxr1998

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see [http://www.gnu.org/licenses/](http://www.gnu.org/licenses/gpl-3.0.html).

---

###android-lockpattern library###
Copyright 2012 Hai Bison

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

---

Copyright (C) 2007 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

---