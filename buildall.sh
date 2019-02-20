#!/usr/bin/env bash
(cd contrib && mvn install)
(cd jprotoc && mvn install)
(cd demos && mvn install)