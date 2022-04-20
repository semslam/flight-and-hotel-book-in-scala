/**
 * Created by Igbalajobi Jamiu O. on 25/11/2016.
 */
function isSearchValid() {
    var isValid = false;
    var numOfDestination = parseInt(document.getElementsByName("num_of_destination")[0].value);
    for (var i = 1; i <= numOfDestination; i++) {
        var intDeparture = document.getElementsByName("departure_airport_code_" + i)[0];
        var intArrival = document.getElementsByName("arrival_airport_code_" + i)[0];
        intDeparture = intDeparture != 'undefined' ? intDeparture.value : '';
        intArrival = intArrival != 'undefined' ? intArrival.value : '';


        isValid = (intDeparture != '' && intDeparture != 'undefined') && (intArrival != '' && intArrival != 'undefined')
    }
    return isValid;
}

var flightData = null;
angularApp.controller('FlightBookingEngineController', function ($scope, $http, $httpParamSerializer, $location, searchEngine) {
    $scope.headTitle = "Book your <span>flights</span> with us!";
    $scope.flexibleDate = false;
    $scope.submitFormB2C = function () {
        if (isSearchValid()) {
            // Before Sending HTTP Request
            searchEngine.flight(document.getElementById("b2cForm"));
        } else {
            //Invalid form input.
            iziToast.error({
                position: 'topRight',
                title: "Invalid/missing fields",
                message: "Kindly select a departure & arrival city with your preferred date."
            });
        }
    };
});

angularApp.controller('HotelBookingEngineController', function ($scope, $http, $httpParamSerializer, $location, searchEngine) {
    $scope.submitHotelFormB2C = function () {
        if ($scope.b2cHotelForm.$valid) {
            searchEngine.hotel()
        }
    };
});

angularApp.controller('FlightErrorController', function ($scope) {
    document.title = "No Result | Cheap flights, hotels and vacation packages";
});

angularApp.controller('B2C_FlightDetailController', function ($scope) {
    $scope.currentDiv = 1;
    $scope.attrFormBtn = true;
    $scope.nextStep = function (step) {
        if (parseInt(step) == 3) {
            var title = angular.element(document).find('select[data-ref=title]').val();
            var fname = angular.element(document).find('input[data-ref=first_name]').val();
            var lname = angular.element(document).find('input[data-ref=last_name]').val();
            var dob = angular.element(document).find('input[data-ref=date_of_birth]').val();

            var ctitle = angular.element(document).find('select[name=title]').val();
            var cfname = angular.element(document).find('input[name=firstName]').val();
            var csurname = angular.element(document).find('input[name=surname]').val();
            var cemail = angular.element(document).find('input[name=email]').val();
            var cphone = angular.element(document).find('input[name=phone]').val();
            var itineraryRef = angular.element(document).find('input[name=itineraryRef]').val();

            if ((title == '' || title == 'undefined') || (fname == '' || fname == 'undefined') && (lname == '' || lname == 'undefined') || (dob == '' || dob == 'undefined')) {
                $scope.attrFormBtn = true;
                jQuery(function () {
                    iziToast.error({
                        position: 'topRight',
                        title: "Check Input Field.",
                        message: "Title, First, Last Name and Date of Birth Cannot be Left Blank."
                    });
                })
            } else if ((ctitle == '' && ctitle == 'undefined') && (cfname == '' && cfname == 'undefined') && (csurname == '' && csurname == 'undefined') && (cemail == '' && cemail == 'undefined') && (cphone == '' && cphone == 'undefined')) {
                $scope.attrFormBtn = true;
                jQuery(function () {
                    iziToast.error({
                        position: 'topRight',
                        title: "Invalid Contact Information.",
                        message: "Contact Title, First, Surname, Email and Phone Cannot be Left Blank."
                    });
                })
            } else if (!validateEmail(cemail)) {
                $scope.attrFormBtn = true;
                jQuery(function () {
                    iziToast.error({
                        position: 'topRight',
                        title: "Invalid Email Address.",
                        message: "Please enter a valid Email address."
                    });
                });
            } else if (parseInt(cphone) < 6) {
                $scope.attrFormBtn = true;
                jQuery(function () {
                    iziToast.error({
                        position: 'topRight',
                        title: "Invalid Phone Number ",
                        message: "A minimum of six(6) digits phone number is required"
                    });
                });
            } else {
                $scope.attrFormBtn = false;
                document.b2cFlForm.submit();
            }
        } else {
            //Proceed to next step
            $scope.attrFormBtn = false;
            $scope.currentDiv = step;
        }
    };

});

angularApp.controller('B2C_FlightCheckOutController', function($scope) {
    
    $scope.filterSearchEng = false;

    $scope.paymentMethodId = 5;

    $scope.selectPaymentMethod = function (paymentMethodId) {
        $scope.paymentMethodId = paymentMethodId;
    };
});

angularApp.controller('B2C_FlightResultController', function ($scope) {
    $scope.parseDate = function (date) {
        return new Date(date);
    };
    $scope.formSearchRequest = JSON.parse(sessionStorage.getItem(tabHashIndex + "_flight"));
    $scope.range = function (min, max, step) {
        step = step || 1;
        var input = [];
        for (var i = min; i <= max; i += step) {
            input.push(i);
        }
        return input;
    };
    $scope.tabHash = tabHashIndex;
});

angularApp.controller('B2B_FlightResultController', function ($scope, $window, $http, $location) {
    $scope.parseDate = function (date) {
        return new Date(date);
    };

    $scope.isLoading = true;

    $scope.showMoreFlightResult = function (airlineName, airlineCode, $event) {
        $(function () {
            var hidden_div = $("." + airlineCode);
            var is_hidden = hidden_div.is(':visible');
            $scope.isShowAirlineCode = airlineCode;
            if (is_hidden == false) {
                hidden_div.fadeIn('slow').show();
                $scope.isShow = true;
            } else {
                hidden_div.fadeOut('slow').hide();
                $scope.isShow = false;
            }
        })
    };
});

angularApp.controller('B2C_HotelResultController', function ($scope) {
    $scope.isLoading = true;
    $scope.tabHash = tabHashIndex;
    $scope.range = function (min, max, step) {
        step = step || 1;
        var input = [];
        for (var i = min; i <= max; i += step) {
            input.push(i);
        }
        return input;
    };
});

angularApp.controller('B2C_HotelDetailController', function ($scope, $location, $http, pagination, $httpParamSerializer) {
    document.title = "Hotel Detail | Cheap flights, hotels and vacation packages";
    $scope.step = 1;
    $scope.paymentMethodId = 5;
    $scope.filterSearchEng = false;
    $scope.facilityParse = function (itemCode) {
        return facilityIcon(itemCode);
    };
    $scope.selectRoom = function (roomCode, hotelId) {
        window.location.href = '/hotel/booking?id=' + hotelId + '&rid=' + roomCode;
    };

    $scope.steppy = function (step) {
        $scope.step = step;
    }
});

angularApp.controller('B2C_HotelDetailPassengerController', function ($scope, $location, $routeParams, $http, pagination, $httpParamSerializer) {
    $scope.getNumber = function (num) {
        return new Array(num);
    };
    var hotelId = $routeParams.hotelId;
    var roomCode = $routeParams.roomCode;
    $scope.step = 2;
    $scope.searchRequest = JSON.parse(sessionStorage.getItem(tabHashIndex + '_hotel'));
    $http({
        method: 'GET',
        url: '/hotel/booking-availability?hotelId=' + hotelId + '&roomCode=' + roomCode + '&fl_tabHash=' + tabHashIndex
    }).then(function (success) {
        if (success.data.responseCode == 200) {
            $scope.booking = {
                data: JSON.parse(success.data.data),
                hotelAvailabilityRequest: JSON.parse(success.data.hotelAvailabilityRequest),
                room: JSON.parse(success.data.room),
                paymentOptions: {
                    offline: JSON.parse(success.data.paymentOptions.offline),
                    online: JSON.parse(success.data.paymentOptions.online)
                }
            };
            $scope.titleAdults = [{name: 'Mr.', value: 'Mr'}, {name: 'Ms.', value: 'Ms'}, {name: 'Mrs.', value: 'Mrs'}];
            $scope.titleChildren = [{name: 'Mr.', value: 'Mr'}, {name: 'Master', value: 'Master'}, {
                name: 'Miss',
                value: 'Miss'
            }, {name: 'Mrs.', value: 'Mrs'}];
            $scope.paymentMethodId = 5;
            $scope.selectPaymentMethod = function (paymentMethodId) {
                $scope.paymentMethodId = paymentMethodId;
            };
            $scope.disabledBtn = false;
            $scope.btnText = "Make Reservation";
            var data = "";
            var formValid = true;
            $scope.bookHotelRoom = function (form) {
                (function () {
                    var formObj = $("form[name=hotelBookRoom]");
                    $.each(formObj.find('input, select'), function (i, obj) {
                        var attr = $(obj).attr('required');
                        if (typeof attr !== typeof undefined && attr !== false) {
                            if ($(obj).val() === '' || $(obj).val() === 'undefined') {
                                $(obj).css({'border': '1px solid red'});
                                $(obj).focus();
                                formValid = false;
                            } else {
                                formValid = true;
                            }
                        }
                    });
                    data = formObj.serialize();
                })();
                if (form.$valid && formValid) {
                    $scope.btnText = "Loading, please wait.";
                    $scope.disabledBtn = true;
                    $http({
                        method: 'GET',
                        url: '/hotel/book?hotelId=' + hotelId + '&roomCode=' + roomCode + '&fl_tabHash=' + tabHashIndex + '&' + data,
                        data: {hotelId: hotelId, roomCode: roomCode},
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        }
                    }).then(function (success) {
                        if (success.data.responseCode == 200) {
                            $location.url('/hotel/booking/success' + '?refId=' + success.data.id + "&hotelId=" + hotelId + "&roomCode=" + roomCode);
                        } else {
                            $location.url('/hotel/booking/failed' + '?refId=' + success.data.id + "&hotelId=" + hotelId + "&roomCode=" + roomCode);
                        }
                        $scope.btnText = "Make Reservation";
                        $scope.disabledBtn = false;
                    }, function (error) {
                        console.log(error);
                        // iziToast.error({
                        //     title: "Error Occured.",
                        //     message: "Internal Server Error, Please try again"
                        // });
                        $scope.btnText = "Make Reservation";
                        $scope.disabledBtn = false;
                    });
                } else {
                    $scope.btnText = "Make Reservation";
                    $scope.disabledBtn = false;
                    // (function () {
                    //     iziToast.error({
                    //         title: "Invalid form form input",
                    //         message: "Invalid form input. Please check inputs and try again."
                    //     });
                    // })()
                }
            };
        }
    }, function (error) {
        console.log(error);
        // $location.url('/hotel/error?msg=' + encodeURI("Our Server Couldn't Understand this Request") + "&code=EX");
    });
});

angularApp.controller('B2C_HotelBookingConfirmController', function ($scope, $http, $routeParams, $location) {
    $http({
        method: 'GET',
        url: '/hotel/book/confirm/' + $routeParams.refId + "/" + $routeParams.hotelId + "/" + $routeParams.roomCode + "?fl_tabHash=" + tabHashIndex
    }).then(function (success) {
        document.title = "Error Occured | Cheap flights, hotels and vacation packages";

    }, function (error) {
    });

});

