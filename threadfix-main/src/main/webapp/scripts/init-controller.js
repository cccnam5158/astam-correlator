var myAppModule = angular.module('threadfix')

myAppModule.controller('InitController', function ($scope, $window, $rootScope, $log) {

    var setToken = function(oldValue, newValue) {
        if (oldValue !== newValue) {
            $rootScope.csrfToken = $scope.csrfToken;
        }

        $log.info('Token is ' + $scope.csrfToken);
    }

    var setRoot = function(oldValue, newValue) {
        if (oldValue !== newValue) {
            $rootScope.urlRoot = $scope.urlRoot;
        }

        $log.info('Root is ' + $scope.urlRoot);
    }

    $scope.$watch('csrfToken', setToken);
    $scope.$watch('urlRoot', setRoot);

});