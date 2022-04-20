$(function () {
    'use strict';
    var bookingEngineDOM = $('form[name=b2cForm]');

    $.fn.deletePrefilledInput = function() {
        $('.search_airport_from').on('click', function(e) {
            var $this = $(this);
            var section = $(this).data('section');
            if($this.val() !== '') $this.val('');
            $('input[name=departure_airport_code_'+ section + ']').val('');        
            $('input[name=departure_country_id_'+ section + ']').val('');        
        });

        $('.search_airport_to').on('click', function(e) {
            var $this = $(this);
            var section = $(this).data('section');
            if($this.val() !== '') $this.val('');
            $('input[name=arrival_airport_code_'+ section + ']').val('');        
            $('input[name=arrival_country_id_'+ section + ']').val('');        
        });
    }

    /*
     * Booking engine form
     */
    $.fn.deleteMultiCity = function ($this) {
        var numDest = $('input[name=num_of_destination]'),
            current_num_of_destination = parseInt(numDest.val());
        $('.multi_section_' + current_num_of_destination).remove();
        current_num_of_destination = (current_num_of_destination <= 1) ? 1 : current_num_of_destination - 1;
        numDest.val(current_num_of_destination);
    };

    $.fn.multi_city_html = function (index) {
        var html = '<div class="multi_section_' + index + '">';
        html += '<div class="col-md-3 col-sm-3 search-col-padding" style="padding-right: 2px;padding-left: 2px;">';
        html += '<label for="destination3">Leaving from</label>';
        html += '<div>';
        html += '<input type="text"  class="form-control search_airport_from" id="dp_airport' + index + '" placeholder="City, region, district or specific airport"  name="departure_airport_' + index + '" data-section="' + index + '" autoselect="true" data-error-message="Departure airport is required" data-required="true" autocomplete="off" aria-autocomplete="none">';
        html += '<input type="hidden" name="departure_airport_code_' + index + '" value="">';
        html += '<input type="hidden" name="departure_country_id_' + index + '" value="">';
        // html += '<span class="input-group-addon hidden-xs hidden-sm"><i class="fa fa-plane fa-fw"></i></span>';
        html += '</div>';
        html += '</div>';

        html += '<div class="col-md-3 col-sm-3 search-col-padding" style="padding-right: 2px;padding-left: 2px;">';
        html += '<label for="destination4">Leaving to</label>';
        html += '<div>';
        html += '<input type="text" class="form-control search_airport_to" placeholder="City, region, district or specific airport" id="ar_airport' + index + '" name="arrival_airport_' + index + '" data-section="' + index + '" autocomplete="off" autoselect="true" data-error-message="Arrival airport is required" data-required="true" aria-autocomplete="none">';
        html += '<input type="hidden" name="arrival_airport_code_' + index + '">';
        html += '<input type="hidden" name="arrival_country_id_' + index + '">';
        // html += '<span class="input-group-addon hidden-xs hidden-sm"><i class="fa fa-plane fa-fw"></i></span>';
        html += '</div>';
        html += '</div>';

        html += '<div class="col-md-4 col-sm-4 search-col-padding" style="padding-right: 2px;padding-left: 2px;">';
        html += '<label for="datepicker6">Departing on</label>';
        html += '<div class="datepicker">';
        html += '<input readonly class="form-control departure_date" data-index="'+index+'" style="cursor: pointer;" id="dp_date' + index + '" data-type="dateIso" data-required="true" name="departure_date_' + index + '" data-error-message="Incorrect Date Specified" type="text" placeholder="Departing Date for Itinerary ' + index + '">';
        // html += '<span class="input-group-addon hidden-xs hidden-sm"><i class="fa fa-calendar fa-fw"></i></span>';
        html += '</div>';
        html += '</div>';

        html += '</div>';
        html += '<div class="clearfix"></div>';
        return html;
    };

    var toDate;
    var maxDate = new Date();
    maxDate.setDate(maxDate.getDate() + 365);
    var date_picker_options_to = {
        minDate: new Date(),
        maxDate: maxDate,
        changeMonth: true,
        changeYear: true,
        dateFormat: 'yy-mm-dd',
        onSelect: function (formattedDate, obj) {
            var arrivalDate = new Date(formattedDate);
            var departDate = new Date($('input[name=departure_date_1]').val());
            if (departDate >= arrivalDate) {
                $('input[name=departure_date_1]').val('');
                $('input[name=arrival_date_1]').val('');
                iziToast.warning({
                    position: 'topRight',
                    title: "Select Departing Date",
                    message: "Departing date cannot be greater than arriving date."
                });
            }
            toDate = formattedDate;
            $('.arrival_date').click();
            $('body').focus();
            $('.arrival_date').click();
            date_picker_options_from['minDate'] = arrivalDate;
        }
    };

    var date_picker_options_from = {
        minDate: new Date(),
        maxDate: maxDate,
        changeMonth: true,
        changeYear: true,
        dateFormat: 'yy-mm-dd',
        onSelect: function (formattedDate, obj) {
            var index = $(obj.input).data('index');
            date_picker_options_to['minDate'] = new Date(formattedDate);

            var departDate = new Date(formattedDate);
            var arrivalDate = new Date($('input[name=arrival_date_1]').val());
            if (departDate >= arrivalDate) {
                $('input[name=departure_date_1]').val('');
                $('input[name=arrival_date_1]').val('');
                iziToast.warning({
                    position: 'topRight',
                    title: "Select Departing Date",
                    message: "Departing date cannot be greater than arriving date."
                });
                $('input[name=departure_date_1]').focus();
            } else {
                $('input[name=departure_date_' + index + ']').attr('value', formattedDate);
            }
            $("#b2cForm").find('.arrival_date').datepicker(date_picker_options_to);
            $('.arrival_date').click();
            $('body').focus();
            $('.arrival_date').click();
        }
    };

    //focus on the departure date by default
    $(document).on('ready', function () {
        $('input[name=departure_airport_1]').focus();
    });

    /*
     | Rest form on Change Trip Type
     */
    bookingEngineDOM.find(".ttype").on('click', function () {
        var trip_type = $(this).data('value'), multiCityDiv = $('.multi_city_div'),
            numOfDestination = $('input[name=num_of_destination]'), divDtArr = $("#div_dt_to"),
            divDtDepart = $("#div_dt_depart"), flexibleDate = $("input[name=flexible_date]"),
            arrivalDate = $('input[name=arrival_date_1]');
        multiCityDiv.hide();
        numOfDestination.val(1);
        $('.ttype').removeClass('active');
        switch (trip_type) {
            case 'ONE_WAY':
                $(this).addClass('active');
                divDtArr.hide();
                divDtDepart.removeClass('col-sm-2');
                divDtDepart.removeClass('col-md-2');
                divDtDepart.addClass('col-md-4');
                divDtDepart.addClass('col-sm-4');
                numOfDestination.val(1);
                arrivalDate.removeAttr('required');
                flexibleDate.removeAttr('disabled');
                flexibleDate.removeAttr('checked');
                $("#flexible_date_div").hide();
                break;
            case 'RETURN':
                $(this).addClass('active');
                divDtArr.show();
                divDtDepart.removeClass('col-md-2');
                divDtDepart.removeClass('col-sm-2');
                divDtDepart.addClass('col-md-2');
                divDtDepart.addClass('col-sm-2');
                numOfDestination.val(1); //round trip is a two way destination.
                arrivalDate.attr('required', 'true');
                flexibleDate.removeAttr('disabled');
                $("#flexible_date_div").show();
                break;
            case 'MULTI_CITY':
                $(this).addClass('active');
                divDtArr.hide();
                divDtDepart.removeClass('col-sm-2');
                divDtDepart.removeClass('col-md-2');
                divDtDepart.addClass('col-md-4');
                divDtDepart.addClass('col-sm-4');
                multiCityDiv.show();
                numOfDestination.val(1);
                flexibleDate.attr('disabled', 'disabled');
                $("#flexible_date_div").hide();
                flexibleDate.removeAttr('checked');
                break;
        }
        $('input[name=trip_type]').val(trip_type);
        return false;
    });

    $('.passenger-cabin-btn').on('click', function () {
        $("#passengerCabinModal").modal('show');
    });

    $('.add_more_multi_city').on('click', function () {
        var current_num_of_destination = parseInt($('input[name=num_of_destination]').val());
        current_num_of_destination = current_num_of_destination + 1;
        if (current_num_of_destination <= 5) {
            $('input[name=num_of_destination]').val(current_num_of_destination);
            var html_append = $(this).multi_city_html(current_num_of_destination);
            $(html_append).appendTo('.more_multi_city_div');
            $(".search_airport_from").on('keyup keyenter paste', function () {
                var $this = $(this);
                $(this).funcAutoSuggestDeparture($(this));
            });
            $(".search_airport_to").on('keyup keyenter paste', function () {
                var $this = $(this);
                $(this).funcAutoSuggestArrival($(this));
            });
        } else {
            iziToast.error({
                position: 'topRight',
                title: "You cannot do that!",
                message: "You cannot add more than " + current_num_of_destination + " destinations"
            });
        }
        $(".search_airport_to").on('keyup keyenter paste', function () {
            var $this = $(this);
            if (localStorage.getItem("airport_cities") === 'undefined' || localStorage.getItem("airport_cities") === null) {
                setTimeout(function () {
                    $(this).funcAutoSuggestArrival($this);
                }, 500);
            } else {
                $(this).funcAutoSuggestArrival($(this));
            }
        });
        $(".departure_date").click(function () {
            var $this = $(this);
            date_picker_options_from['onSelect'] = function (dateText, inst) {
                $('input[name=arrival_date_1]').focus();
                console.log(dateText, inst)
            };
            $this.datepicker(date_picker_options_from);
            $this.focus();
        });
        $(".arrival_date").click(function () {
            var $this = $(this);
            $this.datepicker(date_picker_options_to);
            $this.focus();
        });
        $(".departure_date_icon, .arrival_date_icon").on('click', function () {
            $(this).next('input').click();
        });
        $(this).deletePrefilledInput();
    });

    $(".delete_multi").on('click', function () {
        $(this).deleteMultiCity($(this));
        return false;
    });

    $(".search_airport_from").on('keyup keyenter paste', function () {
        $(this).funcAutoSuggestDeparture($(this));
    });

    $(".search_airport_to").on('keyup keyenter paste', function () {
        $(this).funcAutoSuggestArrival($(this));
    });

    $(this).deletePrefilledInput();

    //$('.departure_date').each(function (i, obj) {
    //    $(obj).attr('value', moment(new Date().setDate(new Date().getDate() + 3)).format("YYYY-MM-DD"));
    //});

    //$('.arrival_date').each(function (i, obj) {
    //    $(obj).attr('value', moment(new Date().setDate(new Date($('.departure_date').val()).getDate() + 5)).format("YYYY-MM-DD"));
    //});


//    $('.departure_date').first()
//
//    $('.arrival_date').first()

    $('.departure_date').on('click', function (e) {
        var $this = $(this);
        $this.datepicker(date_picker_options_from);
        $this.focus();
    });

    $('.arrival_date').on('click', function () {
        var $this = $(this);
        $this.datepicker(date_picker_options_to);
//        $this.focus();
    });

    $(".departure_date_icon, .arrival_date_icon").on('click', function () {
        $(this).next('input').click();
    });


    //increase the number of adults if the infant is above the adults. Meaning should be ADULT >= INFANT
    $('select[name=num_of_infant]').on('change', function () {
        var $this = $(this);
        var adult = $("select[name=num_of_adult]");
        if (parseInt($this.val()) > parseInt(adult.val())) {
            var infantCount = $this.val();
            adult.val(infantCount).change();
            iziToast.warning({
                position: 'topRight',
                title: "",
                message: "The number of infants cannot exceed the number of adults."
            });
        }
    });

    $('select[name=num_of_adult]').on('change', function () {
        var $this = $(this);
        var adult = parseInt($this.val()), infant = parseInt($("select[name=num_of_infant]").val());
        if (infant > adult) {
            $("select[name=num_of_infant]").val(adult).change();
            iziToast.warning({
                position: 'topRight',
                title: "",
                message: "The number of infants cannot exceed the number of adults."
            });
        }
    });

    $('select[name=num_of_adult], select[name=num_of_children], select[name=num_of_infant]').on('change', function (e) {
        var $obj = $(this);
        var maxPax = 9;
        var $adt = $("select[name=num_of_adult]"), $chd = $("select[name=num_of_children]"),
            $inf = $("select[name=num_of_infant]");
        var adtCount = parseInt($adt.val()), chdCount = parseInt($chd.val()), infCount = parseInt($inf.val());
        var totalPass = (adtCount + chdCount + infCount);
        $(".passenger-cabin-btn").attr('value', totalPass + ' Passenger' + (totalPass > 1 ? 's' : '') + ', ' + $(this).toTitleCase($('select[name=cabin_class]').val()));
        if ((adtCount + chdCount + infCount) > maxPax) {
            iziToast.error({
                position: 'topRight',
                title: "Exceeded Limit",
                message: "Total number of travellers allowed are " + maxPax
            });
            $('select[name=num_of_infant], select[name=num_of_children]').val('0').change();
        }
    });

    $('select[name=cabin_class]').on('change', function () {
        var $adt = $("select[name=num_of_adult]"), $chd = $("select[name=num_of_children]"),
            $inf = $("select[name=num_of_infant]");
        var adtCount = parseInt($adt.val()), chdCount = parseInt($chd.val()), infCount = parseInt($inf.val());
        var totalPass = (adtCount + chdCount + infCount);
        $(".passenger-cabin-btn").attr('value', totalPass + ' Passenger' + (totalPass > 1 ? 's' : '') + ', ' + $(this).toTitleCase($('select[name=cabin_class]').val()));
    });

    $.fn.toTitleCase = function (val) {
        return val;
    };

    $(".firstName, .lastName, .otherName").on('blur paste', function () {
        var $obj = $(this);
        var replaced = $obj.val().replace(" ", "");
        replaced = replaced.replace("-", "");
        replaced = replaced.replace("_", "");
        $obj.val(replaced);
        $obj.attr('value', replaced);
        var regExp = new RegExp(/[0-9]+/);
        if (regExp.test(replaced)) {
            iziToast.error({
                position: 'topRight',
                title: "Validation Error!",
                message: "Only alphabetic characters are allowed."
            });
            $obj.focus();
        }
    });


    $("#submit-form").on('click', function () {
        var paymentMethodId = $("input[name=paymentMethodId]");
        if (paymentMethodId.val() === 'undefined' || paymentMethodId.val() === null) {
            iziToast.error({
                position: 'topRight',
                title: "Payment Option Required",
                message: "Please select your preferred payment option."
            });
        } else {
            $(this).attr('disabled', 'disabled');
            $(this).addClass("disabled");
            document.b2cFlForm.submit();
            $(this).html("<span class=\"fa fa-spin fa-spinner\"></span> Loading...please wait!")
        }
    });

    /*
     * Engine of Booking Engine form
     */


    $('.more-result').click(function () {
        var airline = $(this).attr('data-airlinecode');
        var obj = $('.' + airline);
        obj.toggle();
        if (obj.is(':visible')) {
            $(this).find('.resultToggleTxt').html('Less');
        } else {
            $(this).find('.resultToggleTxt').html($(this).attr('data-count') + ' More');
        }
        return false;
    });

    $('.view-details').click(function () {
        var item = $(this).attr('data-target');
        var obj = $('.' + item);
        obj.toggle();
        if (obj.is(':hidden')) {
            $(this).html('View Details');
        } else {
            $(this).html('Hide Details');
        }
    });

    $('.showmtxpopup').click(function () {
        var item = $(this).attr('data-target');
        $('.' + item).toggle();
    });


    /**
     * Flight Result Page
     */
    var _fObj = $('._filter');
    var airlinesArr, stopsArr, cabinArr, amtArr;
    var filterObj = {}, sStops = [], sAmt = [], sAirline = [], sCabin = [], toShowItems = [];
    _fObj.on('click', function () {
        var $this = $(this);
        var value = $this.val();
        switch ($this.attr('name')) {
            case 'stop':
                if ($this.is(":checked")) sStops.push(value); else sStops.splice($.inArray(value, sStops), 1);
                filterObj.stops = sStops;
                break;
            case "price":
                if ($this.is(":checked")) sAmt.push(value); else sAmt.splice($.inArray(value, sAmt), 1);
                filterObj.amt = sAmt;
                break;
            case "airline":
                if ($this.is(":checked")) sAirline.push(value); else sAirline.splice($.inArray(value, sAirline), 1);
                filterObj.airline = sAirline;
                break;
            case "class":
                if ($this.is(":checked")) sCabin.push(value); else sCabin.splice($.inArray(value, sCabin), 1);
                filterObj.cabin = sCabin;
                break;

        }
        //Hide the result pending for filter result
        var results = $('.results');
        //_fObj.one('click', function () {
        //    results.show();
        //});

        airlinesArr = filterObj.airline;
        stopsArr = filterObj.stops;
        cabinArr = filterObj.cabin;
        amtArr = filterObj.amt;
        $("button[name=applyFilter]").on('click', function () {
            var urlParam = '&q=true';
            $.each(airlinesArr, function (i, obj) {
                urlParam += '&airline[' + i + ']=' + obj
            });
            $.each(stopsArr, function (i, obj) {
                urlParam += '&stop[' + i + ']=' + obj;
            });
            $.each(cabinArr, function (i, obj) {
                urlParam += '&cabin[' + i + ']=' + obj;
            });
            $.each(amtArr, function (i, obj) {
                urlParam += '&amt[' + i + ']=' + obj;
            });
            if (typeof airlinesArr !== 'undefined') urlParam += '&airlineCnt=' + airlinesArr.length;
            if (typeof stopsArr !== 'undefined') urlParam += '&stopCont=' + stopsArr.length;
            if (typeof cabinArr !== 'undefined') urlParam += '&cabinCnt=' + cabinArr.length;
            if (typeof amtArr !== 'undefined') urlParam += '&amtCnt=' + amtArr.length;
            window.location.href = '/flight/result/international?fl_tabHash=' + tabHashIndex + urlParam;
        });
    });

    //preload the page with content
    var cachedReq = $.parseJSON(sessionStorage.getItem(tabHashIndex + "_flight"));
    if (cachedReq !== null && cachedReq !== "undefined" && cachedReq.trip_type !== 'MULTI_CITY') {
        var form = $('form[name=b2cForm]');
        form.find('input[name=num_of_destination]').val(cachedReq.num_of_destination);
        form.find('input[name=trip_type]').val(cachedReq.trip_type);
        form.find('input[name=sale_category]').val(cachedReq.sale_category);
        form.find('input[name=uuid]').val(cachedReq.uuid);

        (cachedReq.flexible_date === "1") ? form.find('input[name=flexible_date]').val(cachedReq.flexible_date).checked() : '';
        $.each(form.find('input[name=trip_type]'), function (k, obj) {
            var $obj = $(obj);
            if ($obj.val() === cachedReq.trip_type) {
                $obj.attr('checked', true);
            }
        });
        switch (cachedReq.trip_type) {
            case "ONE_WAY":
                $("#div_dt_to").hide();
                break;
            case "MULTI_CITY":
                $("#div_dt_to").hide();
                $('.multi_city_div').show();
                break;
        }
        form.find('input[name=departure_date_1]').val(cachedReq['departure_date_1']);
        form.find('input[name=pass_desc]').val(cachedReq['pass_desc']);

        for (var i = 1; i <= parseInt(cachedReq.num_of_destination); i++) {
            form.find('input[name=departure_airport_' + i + ']').val(cachedReq['departure_airport_' + i]);
            form.find('input[name=departure_airport_code_' + i + ']').val(cachedReq['departure_airport_code_' + i]);
            form.find('input[name=departure_country_id_1' + i + ']').val(cachedReq['departure_country_id_1' + i]);

            form.find('input[name=arrival_airport_' + i + ']').val(cachedReq['arrival_airport_' + i]);
            form.find('input[name=arrival_airport_code_' + i + ']').val(cachedReq['arrival_airport_code_' + i]);
            form.find('input[name=arrival_country_id_' + i + ']').val(cachedReq['arrival_country_id_' + i]);

            form.find('input[name=arrival_date_' + i + ']').val(cachedReq['arrival_date_' + i]);
        }
        form.find('select[name=num_of_adult]').val(cachedReq.num_of_adult).change();
        form.find('select[name=num_of_children]').val(cachedReq.num_of_children).change();
        form.find('select[name=num_of_infant]').val(cachedReq.num_of_infant).change();

        form.find('select[name=airline_code]').val(cachedReq.airline_code).change();
    }
});


/*
 * Search Detail and Booking JS
 */
(function () {

    //onLoad, preload the prices
    $(".jsCheckPrice").each(function (index, obj) {
        if ($(obj).is(":checked")) {
            var htmlDisp = '<tr data-id="' + $(obj).val() + '" class="item_' + $(obj).val() + '" data-amount="' + $(obj).attr('data-amount') + '">';
            htmlDisp += '<td><strong>' + $(obj).attr("data-title") + '</strong></td>';
            htmlDisp += '<td>' + $.number($(obj).attr("data-amount"), 2) + '</td>';
            htmlDisp += '</tr>';
            $(htmlDisp).prependTo("#jsTablePriceDisplay");
        }
    });

    $('input[name=includeSMS]').on('load', function () {
        var $this = $(this), smsCharge = $this.val();
        var htmlDisp = '<tr data-id="sms" class="item_smscharge">';
        htmlDisp += '<td><strong>SMS Reminder</strong></td>';
        htmlDisp += '<td>' + $.number(smsCharge, 2) + '</td>';
        htmlDisp += '</tr>';
        $(htmlDisp).prependTo("#jsTablePriceDisplay");
        $('#jsTotalCost').html($.number($(this).getTotal(), 2));
    });

    //OnChecked, load the price
    $('.jsCheckPrice').click(function () {
        if ($(this).is(":checked")) {
            var htmlDisp = '<tr data-id="' + $(this).val() + '" class="item_' + $(this).val() + '">';
            htmlDisp += '<td><strong>' + $(this).attr("data-title") + '</strong></td>';
            htmlDisp += '<td>' + $.number($(this).attr("data-amount"), 2) + '</td>';
            htmlDisp += '</tr>';
            $(htmlDisp).prependTo("#jsTablePriceDisplay");
        } else {
            $("#jsTablePriceDisplay").find('.item_' + $(this).val()).remove();
        }
        $('#jsTotalCost').html($.number($(this).getTotal(), 2));
    });


    $.fn.getTotal = function () {
        var sum = 0.0;
        $.each($('#jsTablePriceDisplay').find('tr'), function (index, obj) {
            var amount = $(obj).find('td:nth-child(2)').html();
            //Check if 'Total' keyword doesn't exist in the 'td' html
            if (amount.indexOf('Total') <= -1) {
                sum += parseFloat(amount.replace(',', ''));
            }
        });
        return $.number(sum, 2);
    };

    $('#jsTotalCost').html($.number($(this).getTotal(), 2));


    // var isPaymentExist = false;
    // $('input[name=paymentMethodId]').click(function () {
    //     var paymentMethodId = $(this).val();
    //     var onlinePayIds = [2];
    //     //Check if paymentMethod is Online pay
    //     if (onlinePayIds.indexOf(parseInt(paymentMethodId)) > -1 && isPaymentExist === false) {
    //         var webpayExtraCharge = 2000.0;
    //         var htmlDisp = '<tr data-id="webpay" class="item_webpay">';
    //         htmlDisp += '<td><strong>Online WebPay</strong></td>';
    //         htmlDisp += '<td>' + webpayExtraCharge + '</td>';
    //         htmlDisp += '</tr>';
    //         $(htmlDisp).prependTo("#jsTablePriceDisplay");
    //         isPaymentExist = true
    //     } else {
    //         $("#jsTablePriceDisplay").find('.item_webpay').remove();
    //         isPaymentExist = false;
    //     }
    //     //Update the price display info
    //     $('#jsTotalCost').html($.number($(this).getTotal(), 2));
    // });

    $('input[name=includeSMS]').click(function () {
        var notDefined = $("#jsTablePriceDisplay").find('.item_smscharge') === 'undefined';
        if (!notDefined) {
            $("#jsTablePriceDisplay").find('.item_smscharge').remove();
        }
        if ($(this).is(":checked")) {
            var smsCharge = $(this).data('value');
            var htmlDisp = '<tr data-id="sms" class="item_smscharge">';
            htmlDisp += '<td><strong>SMS Reminder</strong></td>';
            htmlDisp += '<td>' + smsCharge + '</td>';
            htmlDisp += '</tr>';
            $(htmlDisp).prependTo("#jsTablePriceDisplay");
        } else {
            $("#jsTablePriceDisplay").find('.item_smscharge').remove();
        }
        //Update the price display info
        $('#jsTotalCost').html($.number($(this).getTotal(), 2));
    });

})();