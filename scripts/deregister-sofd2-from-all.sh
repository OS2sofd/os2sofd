#!/bin/bash

elbArn="arn:aws:elasticloadbalancing:eu-west-1:711926434486:loadbalancer/app/SOFD/02fa122c1792c4d2"
ec2Id="i-039774ce3098e5a34"

# basic error handling enabled
set -euo pipefail

log_info() {
  MESSAGE=$1

  echo "$(date +'%F %H:%M:%S') - INFO - $MESSAGE"
}

get_target_groups() {
  local result=$(/usr/bin/aws elbv2 describe-target-groups --load-balancer-arn $elbArn)
  echo "$result"
}

deregister() {
  arn=$1

  $(/usr/bin/aws elbv2 deregister-targets --target-group-arn $arn --targets Id=$ec2Id)
}

log_info "Running deregister script"

tg=$(get_target_groups)
len=$(echo $tg | jq -r '.[]' | jq length)

for ((i = 0; i < $len; i++))
do
  jqArg1=".TargetGroups[$i].TargetGroupArn"
  jqArg2=".TargetGroups[$i].TargetGroupName"
  targetGroupArn=$(echo $tg | jq -r $jqArg1)
  targetGroupName=$(echo $tg | jq -r $jqArg2)

  if [[ $targetGroupName == SOFD* ]] &&
     [[ $targetGroupName != *test* ]] &&
     [[ $targetGroupName != SOFD-Signaturcentral ]] &&
     [[ $targetGroupName != *Test* ]] &&
     [[ $targetGroupName != *odata* ]] &&
     [[ $targetGroupName != SOFD-Dragoer ]] &&
     [[ $targetGroupName != SOFD-demo ]] &&
     [[ $targetGroupName != SOFD-glostrup ]] &&
     [[ $targetGroupName != *OData* ]]
  then
    log_info "Deregistering $targetGroupName from SOFD #2"
    deregister $targetGroupArn
  fi
done
