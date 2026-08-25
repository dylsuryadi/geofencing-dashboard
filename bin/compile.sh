#!/usr/bin/env bash

rm -r webapp/target/

cd webapp && ./mvnw package -Dmaven.test.skip=true
