#!/bin/bash
cd ${0%/*}
pwd
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



