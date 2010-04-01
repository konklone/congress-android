Summer of Code 2010
--------

_Attention Students_ - If you're looking at this project and wondering what Sunlight is looking for from an applicant, please refer to the description posted on [Sunlight's GSOC 2010 wiki page](http://wiki.sunlightlabs.com/GSOC_2010#Congress_Android_App) about it.  Do not use the Issues page here on Github as an indicator of what features Sunlight is looking for students to add; they are just there to help the lead developer remember things.

Features
--------

* Find members of Congress by using your phone's location, a zipcode, a last name, or a state.
* Read the latest bills and laws.
* Read tweets and watch videos from members' Twitter and YouTube accounts.
* Reply to a member of Congress on Twitter from within the app, using your own account.
* Read the latest news about them, using the [Yahoo News API](http://developer.yahoo.com/search/news/V1/newsSearch.html).


Setup
-----

When setting this up, make sure to copy keys.xml.example to res/values/keys.xml and fill in your Sunlight API key and Yahoo News API key.

To fetch bill information, you will need to enter a base URL for the Drumbone API.  You can use the following URL fragment as the value for "drumbone_base_url":

http://drumbone.services.sunlightlabs.com/v1/api/


Issue Tracking
------

Use the [Github Issues page](http://github.com/sunlightlabs/congress/issues) for this project.