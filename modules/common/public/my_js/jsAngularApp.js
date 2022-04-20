/**
 * Created by Igbalajobi Jamiu O. on 25/11/2016.
 */
//Unique Hash Index for mapping a search query to unique browser.
var tabHashIndex = "";
if (typeof sessionStorage != "undefined") {
    //If 'sessionStorage' is supported on the browser, go ahead with implementing the feature.
    if (sessionStorage.fl_tabHash) {
        tabHashIndex = sessionStorage.fl_tabHash;
    } else {
        tabHashIndex = makeId();
        sessionStorage.fl_tabHash = tabHashIndex;
    }
}

/*
 * Angular JS
 *
 */
var angularApp = angular.module('my-app', ['angular-loading-bar', 'angularLazyImg']); //'ngCookies','ngRoute', 
