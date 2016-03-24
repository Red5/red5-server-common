#!/bin/bash

set -x
set -e

mvn clean

mvn -Dmaven.test.skip=true install

