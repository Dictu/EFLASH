#!/bin/bash
# Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
# SPDX-License-Identifier: EUPL-1.2

set -e # We want to fail at each command, to stop execution
cd /data/ezoef.test
# drush sql-dump --gzip > ../../somewhere-safe/db/`date +%Y-%m-%d-%H%M`.sql.gz
git reset --hard
git checkout develop
git pull origin develop
~/bin/composer install --no-dev
~/.config/composer/vendor/bin/drush cc drush
cd /data/ezoef.test/www
~/.config/composer/vendor/bin/drush csim -y
~/.config/composer/vendor/bin/drush updb -y
~/.config/composer/vendor/bin/drush cr
