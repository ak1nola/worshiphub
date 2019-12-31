# WorshipHub

This is a church projection application

### What

This is a clone of https://github.com/iyobo/epicworship. 

EpicWorship is an excellent application and I recommend it to anybody looking for a free worship projection tool. 
My church used it for 2 or 3 years with no unknown issues popping up. 

### Why

So why this if EpicWorship was/is so cool. 

* I've had to edit the fxml file for EpicWorship to simplify the UI for non-techie folks in my church. 
* According to https://github.com/iyobo/epicworship/commit/31a89ef5fa4f6efea72acbfbdaf28f4bff56635e, Iyobo switching to nodejs more or less meant EpicWorship, in it's current iteration, is not the future. I've honestly looked at other presentation tools but the folks in my church really like the simpler EpicWorship UI.
* Sometime in Dec 2018, I decided to bite the bullet and learn Kotlin. I picked EpicWorship as the project to modify/write in Kotlin. I'm still pretty much a kotlin newb and I think this is reflected a lot in the way the code looks like Java code sometimes.
* This also gave me the opportunity to try out new features like modules and jpackage in java9 upwards
* The bulk of my dev experience has been as a non-desktop UI developer. This just felt like a fun project to try out to see how feature full desktop dev experience would feel like. 

My church now uses WorshipHub instead of EpicWorship full time but I've shied away from putting the code on-line because there have always been one or two issues to resolve. Honestly don't know if I'll get to those so I'm putting it out here anyway.

![](screenshot.png)

### How to run downloaded application-image

For now, you have to build one yourself. See the steps below. And yes, I understand that I'm setting myself up to fail here :)

### How to build

I'll upload download app-images in a few days but for now. 

* Clone or download this repo
* Download java-14 (https://jdk.java.net/14/) - you need this for jpackage
* make sure you have maven installed on your laptop
* run the following command `mvn clean package -Djpackage=/opt/jdk-14/bin/jpackage` - remember to change jpackage parameter to the location of the unzipped java-14 tarball you downloaded earlier
* Simply double-click (or click on mac?) the build worshiphub executable

### Other very very important stuff

* The motion file: http://video2.ignitemotion.com/files/mp4/Looping_Clouds.mp4. There are tonnes of sites to download similar videos free. Google is your friend here (or maybe not)
* The background files in this repo are download from https://www.pexels.com 
* As much as possible where code was lifted from other sources, I've included references to such in the source code
* Bibles are xmm format download from https://sourceforge.net/projects/zefania-sharp/files/Bibles/ENG/ and somewhere else I can't remember right now :(

### Known stuff to think about and features to fix

* The most important is that if the text is too long it will fall outside of the projector screen. At church we currently break up the verses of songs at no more than 7 lines. We find that we could make the font size smaller but at more than 7 lines, the text is too small to read anyway so it's better to break the text into no more than 7 lines.
* I tried showing whitespace in the editor at some point but I never really finished that code so it's mostly crippled on purpose.
* I'll like some kind of theme feature so that, for instance, the text can appear right aligned instead of always centralized, etc.
* I'll like to be able to change the order of items on the projector list e.g. move an hymn above the bible text etc. This was also partially implemented but I left it out eventually
* The buttons and links on the UI could do with a few tooltips
* openjfx doesn't play nice with flv (flash) files on linux - my default os. 
* Also, I didn't test this app at all on mac - my macbook's been dead for almost 2 years now so it is what it is.
* Dialogs should open to the most appropriate folder instead of the installation folder. For instance when changing backgrounds it should open the default OS' picture/image folder at the very least