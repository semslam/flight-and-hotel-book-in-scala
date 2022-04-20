/**
 * Created by Igbalajobi Jamiu on 25/11/2016.
 */
function getParameterByName(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
}

function sortNumber(a, b) {
    return a - b;
}

function formatMoney(nStr) {
    nStr += '';
    x = nStr.split('.');
    x1 = x[0];
    x2 = x.length > 1 ? '.' + x[1] : '';
    var rgx = /(\d+)(\d{3})/;
    while (rgx.test(x1)) {
        x1 = x1.replace(rgx, '$1' + ',' + '$2');
    }
    return x1 + x2;
}

function makeId() {
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    for (var i = 0; i < 15; i++)
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    return text;
}

function validateEmail(emailAddress) {
    var pattern = /^([a-z\d!#$%&'*+\-\/=?^_`{|}~\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]+(\.[a-z\d!#$%&'*+\-\/=?^_`{|}~\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]+)*|"((([ \t]*\r\n)?[ \t]+)?([\x01-\x08\x0b\x0c\x0e-\x1f\x7f\x21\x23-\x5b\x5d-\x7e\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]|\\[\x01-\x09\x0b\x0c\x0d-\x7f\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]))*(([ \t]*\r\n)?[ \t]+)?")@(([a-z\d\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]|[a-z\d\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF][a-z\d\-._~\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]*[a-z\d\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])\.)+([a-z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]|[a-z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF][a-z\d\-._~\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]*[a-z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF])\.?$/i;
    return pattern.test(emailAddress);
}

function facilityIcon(facilityCode) {
    switch (facilityCode) {
        case 'BUC':
            return 'fa fa-user';
            break;
        case 'SAN':
            return 'fa fa-';
            break;
        case 'BBS':
            return 'fa fa-';
            break;
        case 'LUD':
            return 'fa fa-';
            break;
        case 'SHP':
            return 'fa fa-';
            break;
        case 'DSF':
            return 'fa fa-';
            break;
        case 'ECI':
            return 'fa fa-';
            break;
        case 'GLF':
            return 'fa fa-';
            break;
        case 'GYM':
            return 'fa fa-';
            break;
        case 'CHP':
            return 'fa fa-';
            break;
        case 'INP':
            return 'fa fa-';
            break;
        case 'OUP':
            return 'fa fa-';
            break;
        case 'CDP':
            return 'fa fa-';
            break;
        case 'BTQ':
            return 'fa fa-';
            break;
        case 'ELV':
            return 'fa fa-';
            break;
        case 'LBB':
            return 'fa fa-';
            break;
        case 'PTG':
            return 'fa fa-';
            break;
        case 'RMS':
            return 'fa fa-';
            break;
        case 'SNA':
            return 'fa fa-';
            break;
        case 'SUM':
            return 'fa fa-';
            break;
        case 'CPK':
            return 'fa fa-';
            break;
        case 'FLS':
            return 'fa fa-';
            break;
        case 'TNS':
            return 'fa fa-';
            break;
        case 'CAR':
            return 'fa fa-';
            break;

    }
}

function setCookie(data, cookieName) {
    //Delete the old cookie
    document.cookie = cookieName + "; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    var expireDate = 7 * 24 * 60 * 60 * 1000; //Expires within 7days
    var d = new Date();
    d.setTime(d.getTime() + expireDate);
    var expires = "expires=" + d.toUTCString();
    document.cookie = cookieName + "=" + data + ";" + expires + ";path=/";
}

function getCookie(cookieName) {
    var name = cookieName + "=";
    var decodedCookie = decodeURIComponent(document.cookie);
    var ca = decodedCookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

function toggle(id) {
    var element = document.getElementById(id);
    if(element.style.display === 'none') {
        element.style.display = 'block';
    } else {
        element.style.display = 'none';
    }
}