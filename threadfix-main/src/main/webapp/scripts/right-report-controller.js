var myAppModule = angular.module('threadfix');

myAppModule.controller('RightReportController', function ($scope, $window, threadfixAPIService, customSeverityService, $log) {

    // Using this controller is easy; just set up a parent controller with empty and reportQuery fields.
    $scope.empty = $scope.$parent.empty;
    $scope.noData = $scope.$parent.empty;
    $scope.rightMargin = {top: 20, right: 20, bottom: 30, left: 60};

    if (!$scope.empty) {
        $scope.loadingRight = true;
    }

    $scope.$on('seeMoreExtension', function(event, extension) {
        $scope.seeMoreExtension = extension;
    });

    var loadRightReport = function() {
        var start = new Date();
        threadfixAPIService.loadReport("/dashboard/rightReport", $scope.reportQuery).
            success(function(data, status, headers, config) {
                $log.info("Right report request to server took " + ((new Date()).getTime() - start.getTime()) + " ms");

                if (!data.object || !data.object.map) {
                    $scope.empty = true;
                }

                customSeverityService.setSeverities(data.object.genericSeverities);

                if (data.object.map) {
                    $scope.topAppsData = [];
                    data.object.map.forEach(function (application) {

                        if (application.cweId) { // top 10 vulns
                            application.genericSeverities = data.object.genericSeverities;
                            $scope.topAppsData.push(application);
                        } else { // top 10 apps
                            var innerData = {};
                            innerData[customSeverityService.getCustomSeverity("Info")] = application["Info"];
                            innerData[customSeverityService.getCustomSeverity("Low")] = application["Low"];
                            innerData[customSeverityService.getCustomSeverity("Medium")] = application["Medium"];
                            innerData[customSeverityService.getCustomSeverity("High")] = application["High"];
                            innerData[customSeverityService.getCustomSeverity("Critical")] = application["Critical"];
                            innerData.genericSeverities = data.object.genericSeverities;
                            innerData.appId = application.appId;
                            innerData.appName = application.appName;
                            innerData.teamId = application.teamId;
                            innerData.teamName = application.teamName;
                            innerData.title = application.title;
                            $scope.topAppsData.push(innerData);
                        }
                    });
                } else {
                    $scope.topAppsData = undefined;
                }

                $scope.loadingRight = false;

            }).
            error(function(data, status, headers, config) {

                // TODO improve error handling and pass something back to the users
                $scope.rightReportFailed = true;
                $scope.loadingRight = false;
            });
    };

    $scope.$on('rootScopeInitialized', function() {
        $scope.reportQuery = $scope.$parent.reportQuery;
        $scope.label = {
            teamId: $scope.$parent.teamId,
            appId: $scope.$parent.appId

        };
        $scope.rightReportTitle = $scope.$parent.rightReportTitle;
        if (!$scope.empty) {
            loadRightReport();
        }
    });

    var reload = function() {
        $scope.loadingRight = true;
        $scope.rightReport = null;
        $scope.rightReportFailed = false;
        $scope.empty = false;
        loadRightReport();
    };

    $scope.$on('scanUploaded', reload);

    $scope.$on('scanDeleted', function(event, shouldReload) {
        if (shouldReload) {
            reload();
        } else {
            $scope.rightReport = null;

            $scope.topAppsData = null;
            $scope.noData = true;
            $scope.allScans = null;

            $scope.empty = true;
        }
    });
});