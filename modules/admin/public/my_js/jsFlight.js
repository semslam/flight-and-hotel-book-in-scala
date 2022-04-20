/**
 * Created by Igbalajobi Jamiu on 8/3/17.
 *
 */


/*
 * JQuery Code.
 */
jQuery(function () {
    'use strict';
    var form_DOM = $('#flight_bkEngine'), to_d = moment(new Date().setDate(new Date().getDate() + 5)).format("YYYY-MM-DD");

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


    //focus on the departure date by default
    $(document).on('ready', function () {
        $('input[name=departure_airport_1]').focus();
    });
    /*
     | Rest form on Change Trip Type
     */
    $("#flight_bkEngine input[name=trip_type]").on('change', function () {
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
            $.ajax({
                url: FLIGHT_SEARCH_API,
                type: 'get',
                data: form_DOM.serialize() + "&fl_tabHash=" + tabHashIndex,
                dataType: 'json',
                beforeSend: function () {
                    submitBtn.addClass('disabled');
                    submitBtn.html('<i class="fa fa-spin fa-spinner"></i> Loading...');
                },
                success: function (response) {
                    if (response.responseCode === 200) {
                        window.location.href = "/flight/result/" + response.region + "?fl_tabHash=" + tabHashIndex;
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
                    submitBtn.removeClass('disabled');
                    submitBtn.html("Search Flight");
                }
            });
        }
        return false;
    });


    $('.continue-booking').click(function () {
        var close = $('#' + $(this).data('close')), next = $('#' + $(this).data('next'));
        close.removeClass('in');
        next.click();
        return false;
    });

    $("input[name=apply_markup]").on('click', function () {
        var totalFare = parseInt($("input[name=totalCost]").val());
        var initMU = parseInt($("input[name=initMU]").val());
        $('.div_markup').toggle('fast');
        var isChecked = $(this).is(':checked');
        if (!isChecked) {
            var mkUp = 0;
            $("input[name=markup_value]").val(mkUp);
            $('.invoice').html("&#x20a6; " + formatMoney(mkUp + totalFare));
            $('.totalMarkup').html("&#x20a6; " + formatMoney(mkUp + initMU));
        }
    });


    var payableSpan = $('.payable');
    var payableInput = $("input[name=payable]");
    var payableVal = parseInt(payableInput.val());

    $(".xhrPrice").each(function (i, obj) {
        if ($(this).is(":checked")) {
            payableVal = payableVal + parseInt($(this).data('amt'));
            payableInput.val(payableVal);
            payableInput.attr('value', payableVal);
            payableSpan.html("&#x20a6; " + payableVal);
        }
    });


    $('.xhrPrice').on('click', function () {
        var $this = $(this), amt = parseInt($this.data('amt'));
        var isChecked = $(this).is(':checked');
        if (isChecked) {
            payableVal = amt + payableVal;
        } else {
            payableVal = payableVal - amt;
        }
        if (payableVal !== 'NaN') {
            payableInput.val(payableVal);
            payableInput.attr('value', payableVal);
            payableSpan.html("&#x20a6; " + payableVal);
        }
    });

    $('.applyMU').on('click', function () {
        var totalFare = parseInt($("input[name=totalCost]").val());
        console.log('clicked', totalFare);
        var muObj = $("input[name=markup_value]");
        var mkUp = parseInt((muObj.val() !== '' && muObj !== 'undefined') ? muObj.val() : "0");
        $('.invoice-price').html("&#x20a6; " + formatMoney(mkUp + totalFare));
        var initMU = parseInt($("input[name=initMU]").val());
        $('.totalMarkup').html("&#x20a6; " + formatMoney(mkUp + initMU));
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

    var text = '';
    var isVisible = false;
    $('.show-more-result').click(function () {
        var airline = $(this).attr('data-airlinecode');
        $('.' + airline).toggle();
        text = $(this).html();
        if ($('.' + airline).is(':visible')) {
            // $(this).html('Show Less Results For '+ $(this).attr('data-airline'));
        } else {
            // $(this).html(text);
        }
        return false;
    });

    $('.view-details').click(function () {
        var item = $(this).attr('data-target');
        $('.' + item).toggle();
    });
});


/**
 * Traveller's PNR Information Validations
 */
jQuery(function () {

});

