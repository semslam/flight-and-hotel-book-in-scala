
# flight-and-hotel-book-in-scala

## Multidomain Auth [Play 2.5 - Scala]

This is a second part of [Multidomain Seed](https://github.com/adrianhurt/play-multidomain-seed) project mixed with [Silhouette Credentials Seed](https://github.com/adrianhurt/play-silhouette-credentials-seed). Please check them for detailed explanations.

This project tries to be an example of how to implement an Authentication and Authorization layer for a __multiproject__ using the [Silhouette authentication library](http://silhouette.mohiva.com).

The public web page (`www.myweb.com`) implements the typical authentication functionality. You can:

* Sign up (with email confirmation)
* Sign in (with remember me)
* Sign out
* Change password
* Reset password (via email)
* Control of public and private areas

The administration web page (`admin.myweb.com`) also implements an authorization functionality based on roles . You can:

* Sign in (with remember me)
* Sign out
* Change password
* Reset password (via email)
* Control of public and private areas
* Restrict areas to those users whose roles match with the specified ones (with logic `OR` or `AND`)


And please, don't forget starring this project if you consider it has been useful for you.

Also check my other projects:

* [Play Multidomain Seed [Play 2.5 - Scala]](https://github.com/adrianhurt/play-multidomain-seed)
* [Play Silhouette Credentials Seed [Play 2.5 - Scala]](https://github.com/adrianhurt/play-silhouette-credentials-seed)
* [Play-Bootstrap - Play library for Bootstrap [Scala & Java]](https://adrianhurt.github.io/play-bootstrap)
* [Play API REST Template [Play 2.5 - Scala]](https://github.com/adrianhurt/play-api-rest-seed)
