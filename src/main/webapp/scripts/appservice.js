/**
 * Created by juniwang on 6/14/2017.
 */
(function () {
    var appService = (function () {
        var radioBlocks = {};
        var radios = document.getElementsBySelector("INPUT.radio-block-control[name$=publishType]");
        $(radios).each(function (radio) {
            var start = findAncestorClass(radio, "radio-block-start");
            // find the end node
            var end = (function () {
                var e = start;
                var cnt = 1;
                while (cnt > 0) {
                    e = $(e).next();
                    if (Element.hasClassName(e, "radio-block-start"))
                        cnt++;
                    if (Element.hasClassName(e, "radio-block-end"))
                        cnt--;
                }
                return e;
            })();
            radioBlocks[radio.value] = {
                start: start,
                end: end,
                radio: radio,
                show: radio.checked
            };
        });

        var showSingleRadioBlock = function (radioBlock, show) {
            var n = $(radioBlock.start);
            while (n != radioBlock.end) {
                n.style.display = show ? "" : "none";
                radioBlock.show = show;
                n = n.next();
            }
            layoutUpdateCallback.call();
        };

        var triggerClick = function () {
            var toClick = (function () {
                var to;
                for (var name in radioBlocks) {
                    if (radioBlocks.hasOwnProperty(name)) {
                        var r = radioBlocks[name];
                        if (r.show && r.radio.checked)
                            return r;
                        else if (!to && r.show)
                            to = r;
                    }
                }
                return to;
            })();
            if (toClick)
                toClick.radio.click();
        };

        var showAllRadioBlocks = function (show) {
            for (var name in radioBlocks) {
                if (radioBlocks.hasOwnProperty(name)) {
                    showSingleRadioBlock(radioBlocks[name], show);
                }
            }

            triggerClick();
        };

        var showRadioBlockByValues = function (values) {
            for (var name in radioBlocks) {
                if (radioBlocks.hasOwnProperty(name)) {
                    showSingleRadioBlock(radioBlocks[name], values.indexOf(name) >= 0);
                }
            }

            triggerClick();
        };

        return {
            showAllRadioBlocks: showAllRadioBlocks,
            showRadioBlockByValues: showRadioBlockByValues
        }
    })();

    var getElementValue = function (selector) {
        var n = document.getElementsBySelector(selector)[0];
        if (n)
            return n.value
    };

    Behaviour.specify("SELECT[name$=appService]", "azureAppService", 10000, function (app) {
        var oldChange = app.onchange;
        app.onclick = app.onchange = function () {
            if (oldChange) {
                oldChange();
            }
            if (app.value) {
                var azureCredentialsId = getElementValue("SELECT[name$=azureCredentialsId]")
                var resourceGroup = getElementValue("SELECT[name$=resourceGroup]")
                if (azureCredentialsId) {
                    descriptor.isWebAppOnLinux(azureCredentialsId, resourceGroup, app.value, function (t) {
                        if (t.responseObject()) {
                            appService.showAllRadioBlocks(true);
                        } else {
                            appService.showRadioBlockByValues(["git"], true);
                        }
                    })
                } else {
                    appService.showAllRadioBlocks(false);
                }
            } else {
                appService.showAllRadioBlocks(false);
            }
        }
    });

    Behaviour.specify("INPUT[name$=publishType]", "azureAppService", 10000, function () {
        var azureCredentialsId = getElementValue("SELECT[name$=azureCredentialsId]")
        var resourceGroup = getElementValue("SELECT[name$=resourceGroup]")
        var webApp = getElementValue("SELECT[name$=appService]")
        if (!azureCredentialsId || !resourceGroup || !webApp) {
            appService.showAllRadioBlocks(false);
        }
    });
})()