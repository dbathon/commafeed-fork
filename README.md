CommaFeed Fork
==============

A fork of [commafeed](https://github.com/Athou/commafeed) with some changes and improvements.

The fork is based on version 1.0.0 (commit e9cc6fd518c144302efa08a487c5590ba3652d7f from 2013-07-21 12:37:34) before the "persist read status instead of unread status" refactoring.

I use this version for my personal commafeed instance. The main reason for not merging newer changes is that I didn't want to lose old read-statuses during the migration for that refactoring...


Extracts from Original Readme Below
===================================
Sources for [CommaFeed.com](http://www.commafeed.com/).

Google Reader inspired self-hosted RSS reader, based on JAX-RS, Wicket and AngularJS.

Deploy on your own server (using WildFly) or even in the cloud for free on OpenShift.


Translate CommaFeed into your language
--------------------------------------

Files for internationalization are located [here](https://github.com/Athou/commafeed/tree/master/src/main/resources/i18n).

To add a new language, create a new file in that directory.
The name of the file should be the two-letters [ISO-639-1 language code](http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes).
The language has to be referenced in the `languages.properties` file to be picked up.

When adding new translations, add them in en.properties then run `mvn -e groovy:execute -Pi18n`. It will parse the english file and add placeholders in the other translation files. 


Copyright and license
---------------------

Copyright 2013 CommaFeed.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
