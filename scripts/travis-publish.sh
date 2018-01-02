#!/bin/bash

eval "sbt coverage test coverageReport && codecov"