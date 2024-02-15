# Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
# SPDX-License-Identifier: EUPL-1.2

# DEFAULT
# Prepare the settings file for installation
if [ ! -f web/sites/default/settings.php ]
  then
    cp web/sites/default/default.settings.php web/sites/default/settings.php
    chmod ug+rw web/sites/default/settings.php
fi

# Prepare the services file for installation
if [ ! -f web/sites/default/services.yml ]
  then
    cp web/sites/default/default.services.yml web/sites/default/services.yml
    chmod ug+rw web/sites/default/services.yml
fi

# Prepare the settings file for installation
if [ ! -f web/sites/default/settings.local.php ]
  then
    cp web/sites/default/default.settings.local.php web/sites/default/settings.local.php
    chmod ug+rw web/sites/default/settings.local.php
fi

# Prepare the files directory for installation
if [ ! -d web/sites/default/files ]
  then
    mkdir -m775 web/sites/default/files
    mkdir -m775 private_files
    mkdir -m775 tmp
fi

# if [ -n $DRUPAL_DEVELOPMENT ]
# then
#   if [ -n $DRUPAL_DEVELOPMENT_OSX ]
#   then
#     chmod -R 777 web/sites/default/files
#     chmod -R 777 web/sites/denkmee/files
#     chmod -R 777 web/sites/spamklacht/files
#   else
#     chown -R web-data:web-data web/sites/default/files
#     chown -R web-data:web-data web/sites/denkmee/files
#     chown -R web-data:web-data web/sites/spamklacht/files
#   fi
# fi
