/**
 * Created by Igbalajobi Jamiu O. on 8/3/17.
 */
//jQuery(function () {
//    $('input[name=chartDateRange]').daterangepicker({
//            locale: {
//                format: 'YYYY-MM-DD'
//            },
//            startDate: '2015-01-01',
//            endDate: new Date(),
//            ranges: {
//                'Last 7 Days': [moment().subtract(6, 'days'), moment()],
//                'Last 30 Days': [moment().subtract(29, 'days'), moment()],
//                'This Month': [moment().startOf('month'), moment().endOf('month')],
//                'Last Month': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')]
//            }
//        },
//        function (start, end, label) {
//            alert("A new date range was chosen: " + start.format('YYYY-MM-DD') + ' to ' + end.format('YYYY-MM-DD'));
//        }
//    );
//
//    $.fn.loadChart = function (dataType) {
//        $.ajax({
//            url: '/xhr-chart?dtype='+ dataType,
//            type: 'GET',
//            dataType: 'JSON',
//            data: {
//                "dType": dataType
//            },
//            beforeSend: function () {
//
//            },
//            success: function (xhrResponse) {
//                var ctx = $('#salesFlow');
//                new Chart(ctx, {
//                    type: 'bar',
//                    data: {
//                        labels: JSON.parse(xhrResponse.label),
//                        datasets: [
//                            {
//                                label: "B2C Sales",
//                                backgroundColor: 'rgb(255, 0, 0)',
//                                borderColor: 'rgb(255, 64, 0)',
//                                data: JSON.parse(xhrResponse.b2c) //[0, 10, 5, 2, 20, 30, 45]
//                            },
//                            {
//                                label: "B2B Sales",
//                                backgroundColor: 'rgb(0, 0, 255)',
//                                borderColor: 'rgb(0, 64, 255)',
//                                data: JSON.parse(xhrResponse.b2b)// [3, 20, 24, 3, 20, 10, 25]
//                            }
//                        ]
//                    },
//
//                    // Configuration options go here
//                    options: {}
//                });
//            }
//        });
//    };
//
//    $(this).loadChart('monthOfYear');
//
//    var b2cColorRGB = ['rgb(0, 0, 255)', 'rgb(0, 64, 255)'];
//    var b2bColorRGB = ['rgb(255, 0, 0)', 'rgb(255, 64, 0)'];
//    //Event listener to 'chartType' var value
//    $('input[name=chartType]').on('change', function () {
//        var dType = $(this).val();
//        $(this).loadChart(dType);
//        console.log("changed flox");
//    });
//});

