# Sonarflow - Music Discovery App for Android

## Introduction

The Sonarflow Android App is a fun, simple and interactive way to discover new music on Android smartphones and tablets.

The app targets Android 4.x and is currently available in Google Play store, since 2012.

Sonarflow is published under the MIT license (see LICENSE file in the same directory for the complete terms).

Main features of Sonarflow for Android:

* One touch access to your world of music
* Discover new bands and artists
* Sleek user interface
* Get recommendations from last.fm
* Listen to preview clips
* Browse your music by genre or mood


### Spectralmind

Spectralmind was an innovative media technology company founded 2008 by a group of music enthusiasts and semantic audio analysis experts in Vienna, Austria:

Thomas Lidy, Ewald Peiszer, Johann Waldherr and Wolfgang Jochum

Spectralmind�s audio analysis and music discovery applications allow computers to hear music in a similar way as humans do and consequently to find and recommend music by its mere content. This technology is an enabler for solutions in media search, categorization and recommendation.

In addition to Sonarflow Android App, Spectralmind also created Sonarflow iOS App and SEARCH by Sound Platform for audio content analysis (see below).

Spectralmind ceased operations as of September 2015 and published its software stack as open source software under the MIT license.

### Available software

Spectralmind's open source software is comprised of four repositories:

* [SEARCH by Sound Platform a.k.a. Smafe](https://www.github.com/spectralmind/smafe)
* [SEARCH by Sound Web Application a.k.a. Smint](https://www.github.com/spectralmind/smint)
* [Sonarflow iOS App](https://www.github.com/spectralmind/sonarflow-ios)
* [Sonarflow Android App](https://www.github.com/spectralmind/sonarflow-android)

## Build

The source code is organised as an Eclipse based Android project and has not yet been ported to Android Studio.

You can either 
- import it in Eclipse, build and run it (Maven will pull all dependencies), or
- convert the project to Android Studio and start from there (not tested, and any contribution is highly appreciated!)

### Known issues

`pom.xml` currently refers to Spectralmind a development server and repository which is not available anymore.

Note that the app has not yet been tested on Android 5.

The app uses third party APIs (7digital, last.fm) which have changed since the implementation. This makes some of the App's features unavailable for the time being (e.g., pre-listening to song snippets).

## Support

As Spectralmind ceased operation, no support can be given by the company. Please contact any active members on github, or otherwise you can still try technology@spectralmind.com .

## Acknowledgement

We wish to thank all the contributors to this software, with a special thank you to all former employees and freelancers of Spectralmind.

September 2015
The Founders of Spectralmind: Thomas Lidy, Ewald Peiszer, Johann Waldherr 
