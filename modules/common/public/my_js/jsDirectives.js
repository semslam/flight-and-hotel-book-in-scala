/**
 * Created by Igbalajobi Jamiu O. on 25/11/2016.
 */

angularApp.directive('ngNgn', function () {
    return {
        template: "&#x20a6;"
    };
});

angularApp.directive('ngCurrency', function () {
    return {
        restrict: 'E',
        transclude: true,
        template: function(currency){
            return "<span>"+ (currency == 'NGN' || currency == 'ngn') ? '&#x20a6;' : currency + "</span>";
        },
        replace: true
    };
});

angularApp.directive('ngFlightItinerary', function() {
    return {
        templateUrl: "/web/spa/flight/itinerary.html"
    }
});

angularApp.directive('ngFlightMatrixTbl', function() {
    return {
        templateUrl: "/web/spa/flight/itineraryMatrixTbl.html"
    }
});