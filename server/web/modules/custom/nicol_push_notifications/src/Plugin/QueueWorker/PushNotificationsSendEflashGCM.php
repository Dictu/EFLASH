<?php
// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


/**
 * @file
 * Contains Drupal\nicol_push_notifications\Plugin\QueueWorker\PushNotificationsSendEflashGCM.php
 */

namespace Drupal\nicol_push_notifications\Plugin\QueueWorker;

use Drupal\Core\Queue\QueueWorkerBase;
use Drupal\push_notifications\PushNotificationsDispatcher;

/**
 * Processes Tasks: send push notifications.
 *
 * @QueueWorker(
 *   id = "nicol_push_notifications_queue_eflash_gcm",
 *   title = @Translation("Cron Send Push Notifications"),
 *   cron = {"time" = 90}
 * )
 */
class PushNotificationsSendEflashGCM extends QueueWorkerBase {

  /**
   * {@inheritdoc}
   */
  public function processItem($data) {

    // FOR DEBUGGING DURING TEST PERIOD
    // get user id's for logging purposes
    $tokens_flat = $data->tokens;
    $id_list = array(PUSH_NOTIFICATIONS_NETWORK_ID_IOS => array(), PUSH_NOTIFICATIONS_NETWORK_ID_ANDROID => array());
    foreach ($tokens_flat as $token) {
      if ($token->network == PUSH_NOTIFICATIONS_NETWORK_ID_ANDROID) {
        $id_list[PUSH_NOTIFICATIONS_NETWORK_ID_ANDROID][] = $token->uid;
      }
    }
    // Logs a notice.
    \Drupal::logger('nicol_api')->notice('eFlash_GCM: Sending notifications: total: #%items, queue-counter: #%counter -> %title.', array(
      '%counter' => $data->counter, '%items' => $data->items, '%title' => $data->title)
    );
    if (count($id_list[PUSH_NOTIFICATIONS_NETWORK_ID_ANDROID]) > 0) {
      $id_str = implode(", ", $id_list[PUSH_NOTIFICATIONS_NETWORK_ID_ANDROID]);
      \Drupal::logger('nicol_api')->notice('eFlash_GCM: Sending Android notifications to: %gcm_ids', array('%gcm_ids' => $id_str));
    }
    // END FOR DEBUGGING DURING TEST PERIOD

    // $recipients = is_array($data->tokens) ? $data->token : array($data->token);
    // Initialise push notification.
    $push = new PushNotificationsDispatcher();
    // set tokens to which push message must be send
    $push->setTokens($data->tokens);
    // Set message.
    $push->setMessage($data->payload);
    // dispatch messages
    $push->dispatch();
    // $result from dispatch action
    $result = $push->getResults();
    // dd(date("Y/m/d"));
    // dd($result);
    \Drupal::logger('nicol_api')->notice('eFlash_GCM: GCM count_attempted: %count_attempted, count_success: %count_success, overall: %success',
      array('%count_attempted' => $result['gcm']['count_attempted'],
            '%count_success' => $result['gcm']['count_success'],
            '%success' => $result['gcm']['success']));
    unset($push);
  }
}
