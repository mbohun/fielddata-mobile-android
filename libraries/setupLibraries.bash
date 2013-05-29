#!/bin/bash

git clone -b dev https://github.com/JakeWharton/ActionBarSherlock.git
git clone https://github.com/JakeWharton/Android-ViewPagerIndicator.git
git clone https://github.com/jgilfelt/android-mapviewballoons.git
cp -R $ANDROID_HOME/extras/google/google_play_services/libproject/google-play-services_lib .
cp android-mapviewballoons.gradle android-mapviewballoons/android-mapviewballoons/build.gradle
cp google-play-services_lib.gradle google-play-services_lib/build.gradle
cp android-viewpagerindicator.gradle Android-ViewPagerIndicator/library/build.gradle


