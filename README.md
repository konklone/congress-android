## Congress for Android

This is the source code for the Sunlight Foundation's [Congress app](http://congress.sunlightfoundation.com) for Android phones.

Find the most up-to-date version in the Google Play Store: https://play.google.com/store/apps/details?id=com.sunlightlabs.android.congress

We ship the app using the code found in this repository, with some miscellaneous unversioned API keys and build information placed in `res/values/keys.xml`.

We use [Github Issues](/sunlightlabs/congress-android/issues) for issue tracking.

### Setup

When setting this up:

* copy `keys.xml.example` to `res/values/keys.xml` and fill in your [Sunlight API](http://services.sunlightlabs.com) key.
* copy `tracker.xml.example` to `res/xml/tracker.xml`

If you're using Google Analytics, fill in `res/xml/tracker.xml`'s `ga_trackingId` field with your Google Analytics profile tracking ID. (Make sure you've set up a profile in Google Analytics first.)

If you're **not** using Google Analytics, then turn off analytics by setting`res/values/keys,xml`'s `debug_disable_analytics` field to `true`.



### License

We use a [mixed GPLv3 and BSD license](LICENSE) for our code in this repository. Generally speaking, the code specific to our Android app is GPLv3, and the code you could reuse in any Java or Android app is BSD.

The repository also contains some miscellaneous `.jar` files in `/libs`, who have separate individual licenses.

See [LICENSE](LICENSE) for complete details.


### Release Checklist

Final dev check:

* Tested on enough emulators to feel confident?
* Leave any debug stuff commented out or in? (also avoid ever having to do this even temporarily)

Code changes:

* Bump the android:versionCode and android:versionName in AndroidManifest.xml
* Bump the android:app_version in strings.xml to match android:versionName
* Change the android:app_version_older in strings.xml to what's appropriate
* Update the changelog data in arrays.xml to what's appropriate
* Commit changes, add tag in git for version "vX.X.X" where X.X.X is the android:versionName
  - `git tag -a -m "Tagging vX.X.X" vX.X.X`
  - `git push --tags`

Then, release work:

* Check keys.xml:
  - is api_endpoint pointing to production? https?
  - is the distribution_channel correct? (market vs ____)
  - is the market_channel correct? (google vs amazon)
  - are all debug flags set to false?
* Produce unsigned APK version
* Produce signed APK version
  - if desired, produce separate APK version for Amazon
* Take any screenshots needed to replace outdated ones
  - Replace any new screenshots in Dropbox
* Store APKs in Dropbox

Finally, publish:

* Google Play market
* Amazon Appstore market
