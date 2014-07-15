/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Definition of Job Schedule controller
 *
 * @author Ilayaperumal Gopinathan
 */
define(['moment'], function () {
  'use strict';
  return ['$scope', 'JobScheduleService', 'XDUtils', '$state', '$stateParams', '$filter',
    function ($scope, jobScheduleService, utils, $state, $stateParams) {
      $scope.$apply(function () {
        $scope.jobScheduleRequest = {
          jobName: $stateParams.jobName,
          triggerType: 'fixed-delay'
        };
        $scope.scheduleJob = function (jobScheduleRequest) {
          utils.$log.info('Scheduling Job ');
          if (jobScheduleRequest.triggerType === 'fixed-delay') {
            $scope.jobScheduleRequest.triggerOption = '--fixedDelay=' + $scope.jobScheduleRequest.fixedDelay;
          }
          else if (jobScheduleRequest.triggerType === 'date') {
            console.log('Using UTC date: ' + $scope.jobScheduleRequest.utcDate);
            $scope.jobScheduleRequest.triggerOption = '--date=' + '\'' + $scope.jobScheduleRequest.utcDate + '\'';
          }
          else if (jobScheduleRequest.triggerType === 'cron') {
            $scope.jobScheduleRequest.triggerOption = '--cron=' + '\'' + $scope.jobScheduleRequest.cron + '\'';
          }
          jobScheduleService.scheduleJob(jobScheduleRequest);
          $state.go('home.jobs.tabs.deployments');
        };
        $scope.cancelJobSchedule = function () {
          utils.$log.info('Canceling Job schedule ');
          $state.go('home.jobs.tabs.deployments');
        };
        $scope.isFixedDelay = function () {
          return ($scope.jobScheduleRequest.triggerType === 'fixed-delay');
        };
        $scope.isDate = function () {
          return ($scope.jobScheduleRequest.triggerType === 'date');
        };
        $scope.isCron = function () {
          return ($scope.jobScheduleRequest.triggerType === 'cron');
        };
        $scope.$watch('jobScheduleRequest.date', function() {
          if ($scope.jobScheduleRequest.date) {
            var dateMoment = moment($scope.jobScheduleRequest.date);
            dateMoment.utc();
            $scope.jobScheduleRequest.utcDate = dateMoment.format('MM/DD/YY HH:mm:ss');
          }
        }, true);
      });
    }];
});
