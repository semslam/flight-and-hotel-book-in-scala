/**
 * Developed By Igbalajobi Jamiu
 * Date: November 19, 2015 6:10PM
 *
 */


var isLocalStored;

jQuery(function () {
    'use strict';

    var toast = document.querySelector('.iziToast');

    var querySearch = "";
    var autoSuggestObj = {
        serviceUrl: AIRPORT_SUGGEST_API,
        paramName: 'key',
        preventBadQueries: true,
        width: 250,
        minChars: 3,
        clearCache: true,
        onSearchStart: function () {
            NProgress.start();
        },
        onSearchComplete: function (query, suggestions) {
            querySearch = query.toLowerCase();
            NProgress.done();
        },
        transformResult: function (response) {
            return {
                suggestions: $.map(JSON.parse(response), function (obj) {
                    var stateCity = obj.state + ", ";
//                    if (obj.state != null) stateCity = obj.state + ", ";
                    return {
                        code: obj.airCode,
                        airportCity: obj.state,
                        countryCode: obj.countryCode,
                        countryName: obj.countryName,
//                        value: "(" + obj.airCode + ") " + obj.airportName + " - " + stateCity  + " " + obj.city  +", " + obj.countryName
                        value: "(" + obj.airCode + ") " + obj.airportName + ", " + stateCity + obj.city + " - " + obj.countryName
                    };
                })
            };
        }
    };

    $.fn.funcAutoSuggestDeparture = function ($this) {
        var id = $this.data('section');
        autoSuggestObj['onSelect'] = function (suggestion) {
            var value = suggestion.code;
            var selectedArrivalAirport = $('input[name=arrival_airport_code_' + id + ']').attr('value');
            if (value === selectedArrivalAirport) {
                iziToast.error({
                    title: "You cannot do that!",
                    message: "You cannot select the same location as departure and destination",
                    position: 'right'
                });
            } else {
                $('input[name=departure_airport_code_' + id + ']').attr('value', value);
            }
        };
        $this.autocomplete(autoSuggestObj);
        $this.focus();
    };

    $.fn.funcAutoSuggestArrival = function ($this) {
        var id = $this.data('section');
        autoSuggestObj['onSelect'] = function (suggestion) {
            var value = suggestion.code;
            var selectedDepAirport = $('input[name=departure_airport_code_' + id + ']').attr('value');
            if (value === selectedDepAirport) {
                iziToast.error({
                    title: "You cannot do that!",
                    message: "You cannot select the same location as departure and destination",
                    position: 'right'
                });
            } else {
                $('input[name=arrival_airport_code_' + id + ']').attr('value', value);
            }
        };
        $this.autocomplete(autoSuggestObj);
        $this.focus();
    };

    /*
     * Hotel JS AutoComplete
     */
    $.fn.funcAutoHotelSuggest = function ($this) {
        $this.autocomplete({
            serviceUrl: HOTEL_SUGGEST_API,
            paramName: 'key',
            preventBadQueries: true,
            minChars: 3,
            clearCache: true,
            onSearchComplete: function (query, suggestions) {
                querySearch = query.toLowerCase();
            },
            transformResult: function (response) {
                return {
                    suggestions: $.map(JSON.parse(response), function (obj) {
                        return {
                            code: obj.code,
                            destinationType: obj.destinationType,
                            value: obj.name + ", " + obj.countryName
                        };
                    })
                };
            },
            onSelect: function (suggestion) {
                $("input[name=destination_code]").attr("value", suggestion.code);
                $("input[name=destination_type]").attr("value", suggestion.destinationType);
            }
        });
        $this.focus();
    };

    $("input[name=same_as_adult1]").on('click', function () {
        var c_fname = $('.firstName').first();
        var c_lname = $('.lastName').first();
        if ($(this).is(':checked')) {
            var title = $(".title option:selected").first().text();
            var first_name = c_fname.val();
            var last_name = c_lname.val();
            $('select[name=contactTitle]').val(title).change();
            $('input[name=contactFirstName]').val(first_name);
            $('input[name=contactLastName]').val(last_name);
        } else {
            $('select[name=contactTitle]').val('').change();
            $('input[name=contactFirstName]').val('');
            $('input[name=contactSurname]').val('');
        }
    });

    $(document).on('click', function () {
        var departure = $('.search_airport_from').val().length;
        var arriving = $('.search_airport_to').val().length;
        if(departure <= 10 && departure !== 0) {
            $('.search_airport_from').focus();
            iziToast.warning({
                position: 'topRight',
                title: "Invalid Departure Selected",
                message: "Please select an option form the drop-down list"
            })
        }
        if(arriving <= 10 && arriving !== 0) {
            $('.search_airport_to').focus();
            iziToast.warning({
                position: 'topRight',
                title: "Invalid Arrival Selected",
                message: "Please select an option form the drop-down list"
            })
        }
    });
});