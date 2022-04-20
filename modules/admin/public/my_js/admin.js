// tinymce.init({
//     selector: '#editor'
// });

jQuery(function () {
    'use strict';
    $(".work-on-ticket").on('click', function () {
        if (!window.confirm("Do you want to work on this ticket?")) {
            return false;
        }
    });

    $(".popAlert").on('click', function () {
        if (!window.confirm($(this).data('message'))) {
            return false;
        }
    });

    var height = ($("#editable").data('height') !== 'undefined') ? $("#editable").data('height') : 220;
    // tinymce.init({
    //     selector: 'textarea#editable',
    //     height: height,
    //     plugins: [
    //         'advlist autolink lists link image charmap print preview anchor',
    //         'searchreplace visualblocks code fullscreen',
    //         'insertdatetime media table contextmenu paste code'
    //     ],
    //     toolbar: 'insertfile undo redo | styleselect | bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image',
    //     content_css: '//www.tinymce.com/css/codepen.min.css'
    // });
    tinymce.init({
        selector: 'textarea.editable',
        height: 320,
        theme: 'modern',
        plugins: 'print preview fullpage searchreplace autolink directionality visualblocks visualchars fullscreen image link media template codesample table charmap hr pagebreak nonbreaking anchor toc insertdatetime advlist lists textcolor wordcount imagetools contextmenu colorpicker textpattern help',
        // plugins: 'print preview fullpage powerpaste searchreplace autolink directionality advcode visualblocks visualchars fullscreen image link media template codesample table charmap hr pagebreak nonbreaking anchor toc insertdatetime advlist lists textcolor wordcount tinymcespellchecker a11ychecker imagetools mediaembed  linkchecker contextmenu colorpicker textpattern help',
        toolbar1: 'formatselect | bold italic strikethrough forecolor backcolor | link | alignleft aligncenter alignright alignjustify  | numlist bullist outdent indent  | removeformat',
        image_advtab: true,
        content_css: [
            '//fonts.googleapis.com/css?family=Lato:300,300i,400,400i',
            '//www.tinymce.com/css/codepen.min.css'
        ],
        // fullpage_default_doctype: "<!DOCTYPE html>",
        fullpage_default_encoding: "UTF-8"
    });
    $('select[name=displayHtmlCode]').on('click', function () {
        var isHtml = $(this).is(':checked');
        var summernote = $('.sm-editor').summernote({
            minHeight: 200,
            height: 200
        });
        if (isHtml) {
            //display the
            summernote.summernote({
                minHeight: 400,
                height: 400
            });
        } else {
            //remove WYSIWYG from the textarea.
            summernote.destroy();
            summernote.summernote('destroy');
        }
    });

    // var editable = $('.editable').summernote({
    //     height: 400,
    //     minHeight: null,
    //     maxHeight: null,
    //     focus: false
    // });


    $('.chosen-select').chosen({
        no_results_text: "Oops, nothing found!"
    });
});