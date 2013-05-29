#!/bin/bash
cd ${0%/*}
pwd
if [ ! -d "ActionBarSherlock" ]; then
    echo "Cloning ActionBarSherlock"
    git clone -b dev https://github.com/JakeWharton/ActionBarSherlock.git
fi
if [ ! -d "Android-ViewPagerIndicator" ]; then
    echo "Cloning Android-ViewPagerIndicator"
    git clone https://github.com/JakeWharton/Android-ViewPagerIndicator.git
    cp android-viewpagerindicator.gradle Android-ViewPagerIndicator/library/build.gradle
fi
if [ ! -d "android-mapviewballoons" ]; then
    echo "Cloning android-mapviewballoons"
    git clone https://github.com/jgilfelt/android-mapviewballoons.git
    cp android-mapviewballoons.gradle android-mapviewballoons/android-mapviewballoons/build.gradle

fi
if [ ! -d "google-play-services_lib" ]; then
    echo "Copying google-play-services_lib"
    cp -R $ANDROID_HOME/extras/google/google_play_services/libproject/google-play-services_lib .
    cp google-play-services_lib.gradle google-play-services_lib/build.gradle
fi



