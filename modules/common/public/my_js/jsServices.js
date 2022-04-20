/**
 * Created by Igbalajobi Jamiu O. on 25/11/2016.
 */

/*
 * Using angular service to perform search request
 */



/*
 * Using angular to save the search query into the sessionStorage
 */
angularApp.factory("searchEngine", function ($http, $httpParamSerializer, $location) {
    var bookingEngine = {};
    bookingEngine.flight = function (form) {
        var tripName = document.getElementsByName("trip_type")[0].value;
        var departureAirport = document.getElementsByName("departure_airport_1")[0].value;
        var departureAirportCode = document.getElementsByName("departure_airport_code_1")[0].value;
        var arrivalAirport = document.getElementsByName("arrival_airport_1")[0].value;
        var arrivalAirportCode = document.getElementsByName("arrival_airport_code_1")[0].value;
        var depDate = document.getElementsByName("departure_date_1")[0].value;
        var arrDate = document.getElementsByName("arrival_date_1")[0].value;

        // var desc_html = "Looking for the best deals<br />";
        //
        // if (tripName !== 'MULTI_CITY') {
        //     desc_html += departureAirportCode + " <span class='fa fa-exchange'></span> " + arrivalAirportCode;
        // } else {
        //     desc_html += departureAirportCode + " <span class='fa fa-long-arrow-right'></span> " + arrivalAirportCode;
        // }
        // desc_html += "<br />" + depDate + " - " + arrDate;
        var desc_html = "<span class='text-primary'>Please be patient while we search for best options for you <br /></span>";

        if (tripName !== 'MULTI_CITY') {
            desc_html += departureAirport + " <br /><span class='fa fa-exchange'></span><br /> " + arrivalAirport;
        } else {
            desc_html += departureAirport + " <span class='fa fa-long-arrow-right'></span> " + arrivalAirport;
        }
        desc_html += "<br />" + depDate + " - " + arrDate;
        document.getElementsByClassName('flight-desc')[0].innerHTML = "<h4>" + desc_html + "</h4>";


        var data = {};
        var inputField = document.getElementsByTagName('input');
        var selectField = document.getElementsByTagName('select');
        for (var i = 0; i < inputField.length; ++i) {
            data[inputField[i].name] = inputField[i].value;
        }
        for (var i = 0; i < selectField.length; ++i) {
            data[selectField[i].name] = selectField[i].value;
        }

        var isFlexibleDate = document.getElementsByName("flexible_date")[0].checked;

        if (isFlexibleDate) {
            data['flexible_date'] = "1";
            data['airline_code'] = ''; //do not apply airline code while it's flexible date.
        } else data['flexible_date'] = null;

        sessionStorage.setItem(tabHashIndex + "_flight", JSON.stringify(data));
        setCookie(JSON.stringify(data), '_flight_data');
        data = $httpParamSerializer(data);
        angular.element(document.getElementById("loader-wrapper")).show();
        document.getElementsByTagName("body")[0].style = 'position: fixed';
        return $http({
            method: 'GET',
            url: FLIGHT_SEARCH_API + '?fl_tabHash=' + tabHashIndex + '&' + data,
            headers: {'Content-Type': 'application/x-www-form-urlencoded'}
        }).then(function (response) {
            if (response.data.responseCode == 200) {
                window.location.href = "/flight/result/" + response.data.region + "?fl_tabHash=" + tabHashIndex;
            } else {
                angular.element(document.getElementById("loader-wrapper")).hide();
                document.getElementsByTagName("body")[0].style = 'position: inherit';
                $(function () {
                    iziToast.error({
                        position: 'topRight',
                        message: "No available flight found. Please check another date or try later."
                    });
                });
            }
        }, function (error) {
            angular.element(document.getElementById("loader-wrapper")).hide();
            document.getElementsByTagName("body")[0].style = 'position: inherit';
            $(function () {
                iziToast.error({
                    position: 'topRight',
                    message: "Request failed. Please try again."
                });
            })
        });
    };
    bookingEngine.hotel = function () {
        var data = {};
        angular.element(document.getElementById("loader-wrapper")).show();
        var desc_html = "<h3>Searching for the best available hotels in " + document.getElementsByName("city")[0].value + "</h3>";
        // desc_html += "<br /><h4>from " + angular.element(document.getElementsByName('check_in')[0]).val() + " to " + angular.element(document.getElementsByName('check_out')[0]).val() + " </h4>";
        document.getElementsByClassName('hotel-desc')[0].innerHTML = desc_html;
        document.getElementsByTagName("body")[0].style = 'position: fixed';
        var form = document.getElementById("b2cHotelForm");
        var inputField = form.getElementsByTagName('input');
        var selectField = form.getElementsByTagName('select');
        for (var i = 0; i < inputField.length; ++i) {
            data[inputField[i].name] = inputField[i].value;
        }
        for (var i = 0; i < selectField.length; ++i) {
            data[selectField[i].name] = selectField[i].value;
        }
        setCookie(JSON.stringify(data), "_hotel_data");
        sessionStorage.setItem(tabHashIndex + "_hotel", JSON.stringify(data));
        data = $httpParamSerializer(data);
        xhrLoadHotelSearch($http, data);
    };
    return bookingEngine;
});

angularApp.factory('pagination', function ($log, $location, $http, $sce) {
    return {
        paginate: function (records, perPage, page) {
            return {
                data: function () {
                    return records;
                },
                linkPageHTML: function (uiType) {
                    //uiType options are
                    //slide, next
                    var startPage = page - perPage;
                    var endPage = page + perPage;
                    var totalPage = records.length;

                    if (startPage <= 0) {
                        endPage -= (startPage - 1);
                        startPage = 1;
                    }
                    if (endPage > totalPage) endPage = totalPage;

                    var htmlLink = '<nav aria-label="Page navigation"><ul class="pagination">';
                    if (startPage > 1) htmlLink += '<li><a href="#" aria-label="Previous"><span aria-hidden="true">&laquo;</span></a></li>';
                    for (var i = startPage; i <= endPage; i++) {
                        htmlLink += '<li><a href="#">' + i + '</a></li>';
                    }
                    if (endPage < totalPage) htmlLink += '<li><a href="#" aria-label="Next"><span aria-hidden="true">&raquo;</span></a></li>';
                    htmlLink += "</ul></nav>";
                    return $sce.trustAsHtml(htmlLink);
                }
            };
        }
    }
});

var xhrMaxRetry = 6;
var currentRetry = 0;
function xhrLoadHotelSearch(http, data) {
    http({
        method: 'GET',
        url: '/hotel/search' + '?fl_tabHash=' + tabHashIndex + '&' + data,
        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
    }).then(function (response) {
        if (response.data.responseCode === 200) {
            window.location.href = "/hotel/search-result" + '?fl_tabHash=' + tabHashIndex;
        } else {
            if (currentRetry <= xhrMaxRetry) {
                xhrLoadHotelSearch(http, data);
                currentRetry += 1;
            } else {
                document.getElementsByTagName("body")[0].style = 'position: inherit';
                angular.element(document.getElementById("loader-wrapper")).hide();
                currentRetry = 0;
                (function () {
                    iziToast.error({
                        position: 'topRight',
                        title: "No Result!",
                        message: "No result available. Please try again later."
                    });
                })()
            }
        }
    }, function (error) {
        if (currentRetry <= xhrMaxRetry) {
            xhrLoadHotelSearch(http, data);
            currentRetry += 1;
        } else {
            document.getElementsByTagName("body")[0].style = 'position: inherit';
            angular.element(document.getElementById("loader-wrapper")).hide();
            iziToast.error({
                position: 'topRight',
                title: "Failed!",
                message: "Request failed. Please try again later."
            });
            currentRetry = 0;
        }
    })
}