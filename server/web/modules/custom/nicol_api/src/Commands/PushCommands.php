<?php
// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


namespace Drupal\nicol_api\Commands;

use Drush\Commands\DrushCommands;
use Drupal\user\Entity\User;

/**
 * A Drush commandfile.
 *
 * In addition to this file, you need a drush.services.yml
 * in root of your module, and a composer.json file that provides the name
 * of the services file to use.
 *
 * See these files for an example of injecting Drupal services:
 *   - http://cgit.drupalcode.org/devel/tree/src/Commands/DevelCommands.php
 *   - http://cgit.drupalcode.org/devel/tree/drush.services.yml
 */
class PushCommands extends DrushCommands {

  /**
   * Convert all accounts.
   *
   * @command abby:convert-accounts
   * @usage drush abby:convert-accounts
   *   Send test message to Sentry.
   */
  public function convertPushNotifications() {
    $this->output()->writeln('Convert account in push_notification to user entity');
    $token_list = [];
    // get all records in push_notifications table
    $result = push_notifications_get_tokens();
    foreach ($result as $record) {
      if ($account = User::load($record->uid)) {
        $this->output()->writeln("User: ". $record->uid);
        $this->output()->writeln("token: ". $record->token);
        // prevent duplicates and blocked users
        if (!in_array($account->id(), $token_list) ) {
          // At least one of message_type is in user list.

          if ($incident_data = unserialize($record->nicol_data)) {
            // $this->output()->writeln("incidents_data: ". $incident_data);
            $token_list[] = $record;
            // add message_data to user entity
            unset($account->field_push_messagetypes);
            if (count($incident_data) > 0) {
              $account->set("field_push_messagetypes", $incident_data);
            }
            $account->set("field_push_device_id", $record->token);
            $account->set("field_push_device_type", $record->network);
            $account->set("field_push_enabled", 1);
            $account->set("field_push_announcements", 1);
            $account->set("field_push_updates", $record->update_notifications);
            // add custom properties to user entity
            $account->save();
          }
        }
      }
    }

    // loop through and insert values for user in corresponding user entity

  }

}
