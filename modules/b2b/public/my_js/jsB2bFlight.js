/**
 * Created by Kode on 21/07/2016.
 */

/*
 * Booking Engine Search JS
 */
(function () {
    'use strict';

    //focus on the departure date by default
    $(document).on('ready', function () {
        $('input[name=departure_airport_1]').focus();
    });

    var form_DOM = $('#b2b_flight_bkEngine'), to_d = moment(new Date().setDate(new Date().getDate() + 5)).format("YYYY-MM-DD");

    $.fn.deleteMultiCity = function ($this) {
        var numDest = $('input[name=num_of_destination]'), current_num_of_destination = parseInt(numDest.val());
        $('.multi_section_' + current_num_of_destination).remove();
        current_num_of_destination = (current_num_of_destination <= 1 ) ? 1 : current_num_of_destination - 1;
        numDest.val(current_num_of_destination);
    };

    var date_picker_options_to = {
        minDate: new Date(),
        changeMonth: true,
        changeYear: true,
        dateFormat: 'yy-mm-dd'
    };

    var date_picker_options_from = {
        minDate: new Date(),
        changeMonth: true,
        changeYear: true,
        dateFormat: 'yy-mm-dd',
        onSelect: function (formattedDate, obj) {
            date_picker_options_to['minDate'] = new Date(formattedDate);
            $('input[name=arrival_date_1]').val(formattedDate);
        }
    };


    function multi_city_html(index) {
        var html = '<div class="multi_section_' + index + '">';
        html += '<div class="col-md-6 col-lg-6 col-xs-6 col-sm-6 iconic-input form-group">';
        html += '<label for="dp_airport' + index + '">Departure Airport ' + index + '</label>';
        html += '<i class="fa fa-plane"></i>';
        html += '<input class="form-control search_airport_from" id="dp_airport' + index + '" name="departure_airport_' + index + '" data-section="' + index + '" autoselect="true" data-error-message="Departure airport is required"  autocomplete="off" aria-autocomplete="none" placeholder="Departing Airport" />';
        html += '<input type="hidden" name="departure_airport_code_' + index + '"  />';
        html += '<input type="hidden" name="departure_country_id_' + index + '" />';
        html += '</div>';
        html += '<div class="col-md-6 col-lg-6 col-xs-6 col-sm-6 iconic-input form-group">';
        html += '<label for="ar_airport' + index + '">Arrival Airport</label>';
        html += '<i class="fa fa-plane"></i>';
        html += '<input class="form-control search_airport_to" id="ar_airport' + index + '" name="arrival_airport_' + index + '" data-section="' + index + '" autocomplete="off" autoselect="true" data-error-message="Arrival airport is required" aria-autocomplete="none" placeholder="Arrival Airport" />';
        html += '<input type="hidden" name="arrival_airport_code_' + index + '"  />';
        html += '<input type="hidden" name="arrival_country_id_' + index + '" />';
        html += '</div><div class="clearfix"></div>';
        html += '<div class="col-md-6 col-lg-6 col-xs-6 col-sm-6 iconic-input form-group">';
        html += '<label for="dp_date' + index + '">Departure Date</label>';
        html += '<i class="fa fa-calendar"></i>';
        html += '<input class="form-control departure_date" id="dp_date' + index + '" data-type="dateIso" name="departure_date_' + index + '" data-error-message="Incorrect Date Specified" type="text">';
        html += '<input type="hidden" name="arrival_date_' + index + '" />';
        html += '</div>';
        html += '<div class="clearfix"></div>';
        html += '</div>';
        return html;
    }


    /*
     | Rest form on Change Trip Type
     */
    $("#b2b_flight_bkEngine input[name=trip_type]").on('change', function () {
        var $this = $(this);
        //$('input[type=hidden]').val('');
        //$('input[type=radio]').removeAttr('id');
        //$('input[type=radio]').removeAttr('checked');
        var trip_type = $(this).val();
        //form_DOM.get(0).reset();
        //form_DOM.parsley('reset');
        var multiCityDiv = $('.multi_city_div'),
            numOfDestination = $('input[name=num_of_destination]'),
            divDtTo = $("#div_dt_to"),
            flexibleDate = $("input[name=flexible_date]"),
            arrivalDate = $('input[name=arrival_date_1]');
        multiCityDiv.hide();
        numOfDestination.val(1);
        switch (trip_type) {
            case 'ONE_WAY':
                divDtTo.hide();
                numOfDestination.val(1);
                arrivalDate.removeAttr('required');
                flexibleDate.removeAttr('disabled');
                flexibleDate.show();
                break;
            case 'RETURN':
                divDtTo.show();
                numOfDestination.val(1); //round trip is a two way destination.
                arrivalDate.attr('required', 'true');
                flexibleDate.removeAttr('disabled');
                flexibleDate.show();
                break;
            case 'MULTI_CITY':
                //$('.search_airport_from').attr('required', 'required');
                //$('.search_airport_to').attr('required', 'required');
                //$("#div_dt_from").removeClass('col-sm-2');
                //$("#div_dt_from").addClass('col-sm-4');
                //$("#div_dt_from").removeClass('col-xs-6');
                //$("#div_dt_from").addClass('col-xs-12');
                $("#div_dt_to").hide();
                $('.multi_city_div').show();
                $('input[name=num_of_destination]').val(1);
                //$('input[name=arrival_date_1]').attr('data-required', 'true');
                //$('input[name=arrival_date_1]').attr('data-type', 'dateIso');
                //$('input[name=arrival_date_1]').addClass('parsley-validated');
                $("input[name=flexible_date]").attr('disabled', 'disabled');
                flexibleDate.hide();
                flexibleDate.removeAttr('checked');
                break;
        }
    });

    $(".delete_multi").on('click', function () {
        $(this).deleteMultiCity($(this));
        return false;
    });

    $('.add_more_multi_city').on('click', function () {
        var current_num_of_destination = parseInt($('input[name=num_of_destination]').val());
        current_num_of_destination = current_num_of_destination + 1;
        if (current_num_of_destination <= 5) {
            $('input[name=num_of_destination]').val(current_num_of_destination);
            var html_append = multi_city_html(current_num_of_destination);
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
            $.growl.error({
                title: "You cannot do that!",
                message: "You cannot add more than " + (current_num_of_destination - 1) + " destinations"
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
            $this.datepicker(date_picker_options_from);
            $this.focus();
        });
        $(".arrival_date").click(function () {
            var $this = $(this);
            date_picker_options_to['onSelect'] = function () {

            }
            $this.datepicker(date_picker_options_to);
            $this.focus();
        });
        $(".delete_multi").on('click', function () {
            $(this).deleteMultiCity($(this));
        });
        $(".departure_date_icon, .arrival_date_icon").on('click', function () {
            $(this).prev('input').click();
        });
    });

    $(".delete_multi").on('click', function () {
        $(this).deleteMultiCity($(this));
    });

    $(".search_airport_from").on('keyup keyenter paste', function () {
        var $this = $(this);
        if (localStorage.getItem("airport_cities") === 'undefined' || localStorage.getItem("airport_cities") === null) {
            setTimeout(function () {
                $(this).funcAutoSuggestDeparture($this);
            }, 500);
        } else {
            $(this).funcAutoSuggestDeparture($(this));
        }
    });

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

    $('.departure_date').first().attr('value', moment(new Date().setDate(new Date().getDate() + 2)).format("YYYY-MM-DD"));

    $('.departure_date').on('click', function (e) {
        var $this = $(this);
        $this.datepicker(date_picker_options_from);
        $this.focus();
    });

    $('.arrival_date').first().attr('value', to_d);

    $('.arrival_date').on('click', function () {
        var $this = $(this);
        $this.datepicker(date_picker_options_to);
        $this.focus();
    });

    //increase the number of adults if the infant is above the adults. Meaning should be ADULT >= INFANT
    $('input[name=num_of_infant]').on('keyup keyenter paste', function () {
        var $this = $(this);
        var adult = $("input[name=num_of_adult]");
        if (parseInt($this.val()) > parseInt(adult.val())) {
            adult.attr('value', $this.val());
            adult.val($this.val());
        }
    });

    var submitBtn = form_DOM.find('button[type=submit]');
    form_DOM.on('submit', function (e) {
        var destination_locale = $("input[name=region]").val();
        if (form_DOM.parsley().isValid() === false) {
            $.growl.error({
                title: "Validation Error",
                message: "Invalid form input, please check your form input"
            });
        } else {
            if ($('input[name=trip_type]').val() !== 'RETURN') {
                var num_of_destination = $('input[name=num_of_destination]').val();
                for (var i = 1; i <= num_of_destination; i++) {
                    var o = $('input[name=arrival_airport_id_' + num_of_destination + ']');
                    o.attr('value', null);
                    o.val(null);
                }
            }
            sessionStorage.setItem(tabHashIndex + "_flight", JSON.stringify(form_DOM.serializeArray));
            $.ajax({
                // url: '/flight/search/' + destination_locale,
                url: FLIGHT_SEARCH_API,
                type: 'get',
                data: form_DOM.serialize() + "&fl_tabHash=" + tabHashIndex,
                dataType: 'json',
                beforeSend: function () {
                    $("#load_screen").show();
                    submitBtn.addClass('disabled');
                    submitBtn.html('<i class="fa fa-spin fa-spinner"></i> Loading...');
                },
                success: function (response) {
                    if (response.responseCode === 200) {
                        window.location.href = "/flight/search-result/" + response.region + "?fl_tabHash=" + tabHashIndex;
                    } else if (response.responseCode === 503) {
                        $.growl.error({
                            title: "Request failed",
                            message: "Request failed, please check your form input and try again"
                        });
                    }
                },
                error: function () {
                    $.growl.error({
                        title: "Request failed",
                        message: "Request failed, please try again"
                    });
                },
                complete: function () {
                    $("#load_screen").hide();
                    submitBtn.removeClass('disabled');
                    submitBtn.html("Search Flight");
                }
            });
        }
        return false;
    });

    console.log(sessionStorage.getItem(tabHashIndex + "_flight"))
    //var cachedReq = $.parseJSON(sessionStorage.getItem(tabHashIndex + "_flight"));
    //if (cachedReq !== null && cachedReq !== "undefined" && cachedReq.trip_type !== 'MULTI_CITY') {
    //    var form = $('form[name=b2bForm]');
    //    form.find('input[name=num_of_destination]').val(cachedReq.num_of_destination);
    //    form.find('input[name=trip_type]').val(cachedReq.trip_type);
    //    form.find('input[name=sale_category]').val(cachedReq.sale_category);
    //    form.find('input[name=uuid]').val(cachedReq.uuid);
    //    form.find('select[name=cabin_class]').val(cachedReq.cabin_class).change();
    //    switch (cachedReq.trip_type) {
    //        case "ONE_WAY":
    //            $("#div_dt_to").hide();
    //            break;
    //        case "MULTI_CITY":
    //            $("#div_dt_to").hide();
    //            $('.multi_city_div').show();
    //            break;
    //    }
    //    form.find('input[name=departure_date_1]').val(cachedReq['departure_date_1']);
    //    for (var i = 1; i <= parseInt(cachedReq.num_of_destination); i++) {
    //        form.find('input[name=departure_airport_' + i + ']').val(cachedReq['departure_airport_' + i]);
    //        form.find('input[name=departure_airport_code_' + i + ']').val(cachedReq['departure_airport_code_' + i]);
    //        form.find('input[name=departure_country_id_1' + i + ']').val(cachedReq['departure_country_id_1' + i]);
    //        form.find('input[name=arrival_airport_' + i + ']').val(cachedReq['arrival_airport_' + i]);
    //        form.find('input[name=arrival_airport_code_' + i + ']').val(cachedReq['arrival_airport_code_' + i]);
    //        form.find('input[name=arrival_country_id_' + i + ']').val(cachedReq['arrival_country_id_' + i]);
    //        form.find('input[name=arrival_date_' + i + ']').val(cachedReq['arrival_date_' + i]);
    //    }
    //    form.find('select[name=num_of_adult]').val(cachedReq.num_of_adult);
    //    form.find('select[name=num_of_children]').val(cachedReq.num_of_children);
    //    form.find('select[name=num_of_infant]').val(cachedReq.num_of_infant);
    //    if(cachedReq.flexible_date === 1) {
    //        form.find('select[name=flexible_date]').val(cachedReq.cabin_class).change();
    //    }
    //
    //    form.find('select[name=airline_code]').val(cachedReq.airline_code).change();
    //}

})();

/*
 * Flight Result Controller
 */
(function () {
    var text = '';
    $('.show-more-result').click(function () {
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
        $('.' + item).toggle();
    });
})();

/*
 * Search Detail and Booking JS
 */
(function () {

    $(".modifySearchBtn").on('click', function () {
        $(".modifySearchCont").toggle('slow');
    });


    //onLoad, preload the prices
    $(".jsCheckPrice").each(function (index, obj) {
        if ($(obj).is(":checked")) {
            var htmlDisp = '<tr data-id="' + $(obj).val() + '" class="item_' + $(obj).val() + '" data-amount="' + $(obj).attr('data-amount') + '">';
            htmlDisp += '<td><strong>' + $(obj).attr("data-title") + '</strong></td>';
            htmlDisp += '<td>' + $(obj).attr("data-amount") + '</td>';
            htmlDisp += '</tr>';
            $(htmlDisp).prependTo("#jsTablePriceDisplay");
        }
    });

    //OnChecked, load the price
    $('.jsCheckPrice').click(function () {
        if ($(this).is(":checked")) {
            var htmlDisp = '<tr data-id="' + $(this).val() + '" class="item_' + $(this).val() + '">';
            htmlDisp += '<td><strong>' + $(this).attr("data-title") + '</strong></td>';
            htmlDisp += '<td>' + $(this).attr("data-amount") + '</td>';
            htmlDisp += '</tr>';
            $(htmlDisp).prependTo("#jsTablePriceDisplay");
        } else {
            $("#jsTablePriceDisplay").find('.item_' + $(this).val()).remove();
        }
        $('#jsTotalCost').html($(this).getTotal());
    });

    $.fn.getTotal = function () {
        var sum = 0.0;
        $.each($('#jsTablePriceDisplay').find('tr'), function (index, obj) {
            var amount = $(obj).find('td:nth-child(2)').html();
            //Check if 'Total' keyword doesn't exist in the 'td' html
            if (amount.indexOf('Total') <= -1) {
                sum += parseFloat(amount);
            }
        });
        return formatMoney(sum);
    };

    $('#jsTotalCost').html($(this).getTotal());


    var htmlDisp = '<tr data-id="markup" class="item_markup">';
    htmlDisp += '<td><strong class="text-danger">Mark-Up (On-Fly)</strong></td>';
    htmlDisp += '<td>' + $('input[name=markup_value]').val() + '</td>';
    htmlDisp += '</tr>';
    $(htmlDisp).prependTo("#jsTablePriceDisplay");

    $("input[name=apply_markup]").on('click', function () {
        $(".div_markup").toggle();
        if ($(this).is(':checked')) {
            var markupAmount = $('input[name=markup_value]').val();
            var htmlDisp = '<tr data-id="markup" class="item_markup">';
            htmlDisp += '<td><strong class="text-danger">Mark-Up (On-Fly)</strong></td>';
            htmlDisp += '<td>' + markupAmount + '</td>';
            htmlDisp += '</tr>';
            $(htmlDisp).prependTo("#jsTablePriceDisplay");
        } else {
            //remove the markup
            $("#jsTablePriceDisplay").find('.item_markup').remove();
        }
    });

    //Update the price display info
    $(".applyMU").on('click', function () {
        var markupValue = $('input[name=markup_value]').val();
        if (markupValue === 'undefined' || markupValue === '') markupValue = 0;
        $('.item_markup').remove();
        var htmlDisp = '<tr data-id="markup" class="item_markup">';
        htmlDisp += '<td><strong class="text-danger">Mark-Up (On-Fly)</strong></td>';
        htmlDisp += '<td>' + markupValue + '</td>';
        htmlDisp += '</tr>';
        $(htmlDisp).prependTo("#jsTablePriceDisplay");
        $('#jsTotalCost').html($(this).getTotal());
    });


    $("input[name=same_as_adult1]").on('click', function () {
        var c_fname = $('input[data-ref=first_name]').first();
        var c_lname = $('input[data-ref=last_name]').first();
        if ($(this).is(':checked')) {
            var title = $("select[data-ref=title] option:selected").first().text();
            var first_name = c_fname.val();
            var last_name = c_lname.val();
            $('select[name=title]').val(title);
            $('input[name=firstName]').val(first_name);
            $('input[name=surname]').val(last_name);
        } else {
            $('select[name=title]').val('');
            $('input[name=firstName]').val('');
            $('input[name=surname]').val('');
        }
    });

    $("input[data-ref=first_name], input[data-ref=last_name], input[data-ref=middle_name]").on('blur paste', function () {
        var $obj = $(this);
        var replaced = $obj.val().replace(" ", "");
        replaced = replaced.replace("-", "");
        replaced = replaced.replace("_", "");
        $obj.val(replaced);
        $obj.attr('value', replaced);
        var regExp = new RegExp(/[0-9]+/);
        if (regExp.test(replaced)) {
            $.growl.error({
                title: "Validation Error!",
                message: "Only alphabetic characters are allowed."
            });
            $obj.focus();
        }
    });

    $("input[data-ref=date_of_birth]").css('background', '#fff');

    // var totalPrice = parseInt($('input[class=totalCost]').val());
    // $('.totalCost').html(formatMoney(totalPrice));

    $('input[name=paymentMethodId]').click(function () {
        var paymentMethodId = $(this).val();
        var onlinePayIds = [2];
        //Check if paymentMethod is Online pay
        if (onlinePayIds.indexOf(parseInt(paymentMethodId)) > -1) {
            var webpayExtraCharge = 2000.0;
            var htmlDisp = '<tr data-id="webpay" class="item_webpay">';
            htmlDisp += '<td><strong>Online WebPay</strong></td>';
            htmlDisp += '<td>' + webpayExtraCharge + '</td>';
            htmlDisp += '</tr>';
            $(htmlDisp).prependTo("#jsTablePriceDisplay");
        } else {
            $("#jsTablePriceDisplay").find('.item_webpay').remove();
        }
        //Update the price display info
        $('#jsTotalCost').html($(this).getTotal());
    });

})();