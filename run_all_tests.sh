#!/usr/bin/env bash

sbt clean coverage compile test it/test coverageOff coverageReport
