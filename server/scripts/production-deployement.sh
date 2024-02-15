#!/bin/bash
# Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
# SPDX-License-Identifier: EUPL-1.2

set -e # We want to fail at each command, to stop execution
cd /data/ezoef.prod
# drush sql-dump --gzip > ../../somewhere-safe/db/`date +%Y-%m-%d-%H%M`.sql.gz
git reset --hard
git checkout master
git pull origin master
~/bin/composer install --no-dev
cd /data/ezoef.prod/www
../vendor/drush/drush/drush updb -y
../vendor/drush/drush/drush cc drush
../vendor/drush/drush/drush csim -y
../vendor/drush/drush/drush cr
