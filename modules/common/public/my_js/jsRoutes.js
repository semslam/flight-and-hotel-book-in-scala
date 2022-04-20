/**
 * Created by Igbalajobi Jamiu O. on 25/11/2016.
 */
// angularApp.config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {
//     // $routeProvider
//     // .when('/flight/result/:locale', {
//     //     templateUrl: '/web/spa/flight/result.html',
//     //     controller: 'B2C_FlightResultController'
//     // })
//     $locationProvider.html5Mode({
//         enabled: true,
//         requireBase: false
//     });
// }]);
//
// angularApp.run(function ($rootScope, $location) {
//     var spaUrl = [];
//     $rootScope.$on("$locationChangeStart", function (event, next, current) {
//         if (spaUrl.indexOf($location.path()) == -1 && next != current) {
//             window.location.href = next;
//         }
//     });
// });