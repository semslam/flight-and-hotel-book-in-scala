var hotelBookingEngineDOM = $('form[name=b2cHotelForm]');

(function () {
    'use strict';
    /**
     * Check Hotel Room Availability
     */
    $('.checkavail_async').on('click', function () {
        var $this = $(this), href = $this.attr('href'), hotelId = $this.attr('data-hotelid'),
            roomRateId = $this.attr('data-roomrateid');
        $.ajax({
            url: href,
            data: {
                hotelId: hotelId,
                roomRateId: roomRateId
            },
            dataType: 'JSON',
            type: 'GET',
            beforeSend: function () {
                $this.find('.loader-btn').html('<span class="fa fa-spinner fa-spin"></span>Checking...');
                iziToast.warning({message: "checking room availability", title: "loading..."});
                $this.find('.loader-btn').attr('disabled', 'disabled');
                $this.attr('disabled', 'disabled');
            },
            success: function (response) {
                if(response.status === 200){
                    iziToast.notice({message: "Room available for booking...redirecting", title: "Room Available"});
                    location.href = $this.attr('data-nexturl');
                } else {
                    iziToast.error({message: "Sorry, hotel room not available at the moment.", title: "Not Available"});
                }
                $this.find('.loader-btn').html('Book');
            },
            error: function () {
                iziToast.error({message: "Sorry, request couldn't be processed at the moment. Please try again.", title: "Request Failed"});
            },
            complete: function () {
                $this.find('.loader-btn').html('Book');
                $this.removeAttr('disabled');
                $this.find('.loader-btn').removeAttr('disabled');
            }
        });
        return false;
    })
})();

jQuery(function () {
    $.fn.roomContainer = function (roomIndex) {
        var content = '<div id="room' + roomIndex + '"><div class="col-md-6"></div>';
        content += '<div class="col-xs-12 col-md-2">';
        content += '<label>Adult <small>(12+yrs)</small></label>';
        content += '<select class="form-control" name="room' + roomIndex + '_no_of_adult">';
        content += '<option>1</option>';
        content += '<option selected="selected">2</option>';
        content += '<option>3</option>';
        content += '<option>4</option>';
        content += '<option>5</option>';
        content += '<option>6</option>';
        content += '</select>';
        content += '</div>';
        content += '<div class="col-xs-12 col-md-2">';
        content += '<label>Child <small>(0-11yrs)</small></label>';
        content += '<select class="form-control" name="room' + roomIndex + '_no_of_child">';
        content += '<option value="0" selected="selected">0</option>';
        content += '<option value="1">1</option>';
        content += '<option value="2">2</option>';
        content += '</select>';
        content += '</div>';
        content += '<div class="col-xs-12 col-md-2">';
        content += '<label>&nbsp;</label>';
        content += '<button type="button" class="btn btn-danger btn-submit btn-md del_room" style="margin-top: 23px;width: auto;float: right;"><span class="fa fa-minus"></span> Delete</button>';
        content += '</div>';
        content += '<div id="room' + roomIndex + 'ChildAge" class="row"></div>';
        content += '</div>';
        return content;
    };

    $.fn.childAgeHTML = function (roomIndex, index) {
        return "<div class=\"col-md-2 col-xs-12 right\"><div class=\"form-group\">" + "<label for=\"child" + index + "\">Age of Child " + index + "</label>" +
            "<select class=\"form-control\" name=\"room" + roomIndex + "Child" + index + "Age" + "\">" +
            // "<option value=\"0\">0</option>" +
            // "<option value=\"1\">1</option>" +
            "<option value=\"2\">2 &amp; Below</option>" +
            "<option value=\"3\">3</option>" +
            "<option value=\"4\">4</option>" +
            "<option value=\"5\">5</option>" +
            "<option value=\"6\">6</option>" +
            "<option value=\"7\">7</option>" +
            "<option value=\"8\">8</option>" +
            "<option value=\"9\">9</option>" +
            "<option value=\"10\">10</option>" +
            "<option value=\"11\">11</option>" +
            "</select>" +
            "</div></div>";
    };


    $(".ht_city_auto_suggest").on('keyup keyenter paste', function () {
        $(this).funcAutoHotelSuggest($(this));
    });

    $('.checkIn').on('click', function (e) {
        $('.arrival_date').focus();
    });

    $('.checkOut').on('click', function () {

    });

    var maxRoomPerSearch = 5;
    $('.add_room').on('click', function () {
        var currentRoomSize = parseInt($("input[name=no_of_rooms]").val());
        if (currentRoomSize < maxRoomPerSearch) {
            currentRoomSize += 1;
            $("input[name=no_of_rooms]").attr('value', currentRoomSize);
            $("input[name=no_of_rooms]").val(currentRoomSize);
            $('#moreRoomsContainer').append($(this).roomContainer(currentRoomSize));

            $('.del_room').on('click', function () {
                $('#room' + currentRoomSize).remove();
                currentRoomSize -= 1;
                $("input[name=no_of_rooms]").attr('value', currentRoomSize);
            });
            $(this).addChildAge(currentRoomSize);
        }
        return false;
    });

    $.fn.addChildAge = function (roomIndex) {
        $('select[name=room' + roomIndex + '_no_of_child]').on('change', function () {
            var $this = $(this), numOfChd = parseInt($this.val());
            $('#room' + roomIndex + 'ChildAge').html('<div class="col-md-6"></div>');
            if (numOfChd > 0) {
                for (var i = 1; i <= numOfChd; i++) {
                    $('#room' + roomIndex + 'ChildAge').append($(this).childAgeHTML(roomIndex, i));
                }
                $('.childAgeDiv').removeClass('hidden');
            }
        });
    };

    $(this).addChildAge(1);


    $('.radioroom').click(function () {
        if ($(this).is(':checked')) {

        }
    });
});
