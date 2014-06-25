/*
 * Copyright 2014 the original author or authors.
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
 * Definition of the Job Deployment Details controller
 *
 * @author Gunnar Hillert
 */
define([], function () {
  'use strict';
  return ['$scope', 'XDUtils', '$state', '$stateParams', 'JobDefinitions', 'JobDefinitionService',
    function ($scope, utils, $state, $stateParams, jobDefinitions, jobDefinitionService) {
      $scope.$apply(function () {
        console.log($stateParams);
        console.log('ass');
        $scope.definitionName = $stateParams.definitionName;
        var singleJobDefinitionPromise = jobDefinitions.getSingleJobDefinition($scope.definitionName);
        utils.$log.info(singleJobDefinitionPromise);
        utils.addBusyPromise(singleJobDefinitionPromise);
        singleJobDefinitionPromise.then(
          function (result) {
            utils.$log.info(result);
            $scope.definitionDeployRequest = {
              jobDefinition: result.data,
              moduleCriteria: '',
              containerCount: 0
            };
          },function (error) {
            utils.growl.addErrorMessage('Error fetching job definition. ' + error.data[0].message);
          });
      });
      $scope.cancelDefinitionDeploy = function () {
        utils.$log.info('Cancelling Job Definition Deployment');
        $state.go('home.jobs.tabs.definitions');
      };
      $scope.deployDefinition = function (definitionDeployRequest) {
        utils.$log.info('Deploying Job Definition ' + definitionDeployRequest);
        utils.$log.info('Deploying Job Definition ' + definitionDeployRequest.jobDefinition.name);
        utils.$log.info(jobDefinitionService);
        var properties = [];

        if (definitionDeployRequest.moduleCriteria) {
          properties.push('criteria=' + definitionDeployRequest.moduleCriteria);
        }
        if (definitionDeployRequest.containerCount) {
          properties.push('count=' + definitionDeployRequest.containerCount);
        }

        jobDefinitionService.deploy(definitionDeployRequest.jobDefinition, properties).$promise.then(
            function () {
              utils.growl.addSuccessMessage('Deployment Request Sent.');
              definitionDeployRequest.jobDefinition.deployed = true;
              $state.go('home.jobs.tabs.definitions');
            },
            function (error) {
              utils.growl.addErrorMessage('Error Deploying Job. ' + error.data[0].message);
            }
          );
      };
    }];
});
