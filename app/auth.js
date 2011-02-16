var Client = require("ringo/httpclient").Client;
var Headers = require("ringo/utils/http").Headers;
var objects = require("ringo/utils/objects");

function getAuthUrl(request) {
    var url = java.lang.System.getProperty("app.proxy.geoserver");
    if (url) {
        if (url.charAt(url.length-1) !== "/") {
            url = url + "/";
        }
    } else {
        url = request.scheme + "://" + request.host + (request.port ? ":" + request.port : "") + "/geoserver/";
    }
    return url + "rest";
}

var getDetails = exports.getDetails = function(request) {
    var url = getAuthUrl(request);
    var status; 
    var headers = new Headers(objects.clone(request.headers));
    var token = headers.get("Cookie");
    if (token) {
        status = 401; // TODO: determine if authenticated
    } else {
        var client = new Client(undefined, false);
        var exchange = client.request({
            url: url,
            method: "GET",
            async: false,
            headers: request.headers
        });
        exchange.wait();
        var cookie = exchange.headers.get("Set-Cookie");
        if (cookie) {
            token = cookie.split(";").shift();
            status = 401;
        } else {
            status = 404;
        }
    }
    return {
        status: status, 
        token: token, 
        url: url
    };
};
