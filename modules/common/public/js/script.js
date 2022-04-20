/**
 * Developed By Igbalajobi Jamiu
 * Date: November 19, 2015 5:24PM
 *
 */

jQuery(function () {
    'use strict';

    //subscribe to user newsletter.
    $("#newsletterSubscribe").on('submit', function (e) {
        var $this = $(this);
        if ($this.parsley('isValid')) {
            $.ajax({
                url: $this.attr('action'),
                type: $this.attr('method'),
                data: $this.serialize(),
                beforeSend: function () {
                    $this.find('button[type=submit]').html('Loading...');
                },
                success: function (xhr) {
                    if (xhr.status === 1) {
                        iziToast.success({
                            position: 'topRight',
                            message: "Subscription was successful",
                            title: "Successful"
                        });
                    } else if (xhr.status === 0) {
                        iziToast.notice({
                            position: 'topRight',
                            message: "Email address has already been registered.",
                            title: "Notice"
                        });
                    } else {
                        // iziToast.error({
                        //     position: 'topRight',
                        //     message: "Request failed, please try again later",
                        //     title: "Unknown Error"
                        // });
                    }
                },
                complete: function () {
                    $this.find('button[type=submit]').html('Subscribe');
                }
            });
        } else {
            iziToast.error({
                position: 'topRight',
                message: "Invalid email address entered",
                title: "Validation Error"
            });
        }
        e.preventDefault();
        return false;
    });


    if (typeof($.datepicker) !== 'undefined') {
        $("input[name=dob], .dob").datepicker({
            dateFormat: 'yyyy-mm-dd',
            maxDate: new Date(),
            language: 'en',
            autoClose: true
        });
        $(".futureDate").datepicker({
            dateFormat: 'yyyy-mm-dd',
            minDate: new Date(),
            language: 'en',
            autoClose: true
        });

        $('.dtpicker').datepicker({
            autoClose: true,
            language: 'en',
            dateFormat: 'yy-mm-dd'
        });
    }


    if (typeof($.dataTable) !== 'undefined') {
        $('.datatable').dataTable();
    }

    if (typeof('CKEDITOR') !== 'undefined' && typeof('CKEDITOR') !== 'string') {
        var config = {
            codeSnippet_theme: 'Monokai',
            language: 'en',
            height: 400,
            //width: '100%',
            filebrowserUploadUrl: '/jargon/uploader.php',
            filebrowserBrowseUrl: '/browser/browse?type=Images',
            filebrowserWindowWidth: 100,
            filebrowserWindowHeight: 100,
            toolbarGroups: [
                {name: 'clipboard', groups: ['clipboard', 'undo']},
                {name: 'editing', groups: ['find', 'selection', 'spellchecker']},
                {name: 'links'},
                {name: 'insert'},
                {name: 'forms'},
                {name: 'tools'},
                {name: 'document', groups: ['mode', 'document', 'doctools']},
                {name: 'others'},
                {name: 'basicstyles', groups: ['basicstyles', 'cleanup']},
                {name: 'paragraph', groups: ['list', 'indent', 'blocks', 'align', 'bidi']},
                {name: 'styles'},
                {name: 'colors'}
            ]
        };
        CKEDITOR.replace('ck-editor', config);
    }

    if (typeof($.summernote) !== 'undefined') {
        var $summernote = $('.sm-editor').summernote({
            minHeight: 200,
            height: 200
        });
    }

});