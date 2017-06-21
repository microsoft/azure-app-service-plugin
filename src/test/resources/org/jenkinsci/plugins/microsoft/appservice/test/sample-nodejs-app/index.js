/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

var http = require('http');

var server = http.createServer(function(request, response) {

    response.writeHead(200, {"Content-Type": "text/plain"});
    response.end("Hello NodeJS!");

});

var port = process.env.PORT || 1337;
server.listen(port);

console.log("Server running at http://localhost:%d", port);