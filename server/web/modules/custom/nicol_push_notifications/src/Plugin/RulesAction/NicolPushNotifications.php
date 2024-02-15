<?php
// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


namespace Drupal\nicol_push_notifications\Plugin\RulesAction;

use Drupal\Core\Entity\EntityInterface;
use Drupal\rules\Core\RulesActionBase;
use Drupal\user\Entity\User;

/**
 * Send push notifications.
 *
 * @RulesAction(
 *   id = "rules_nicol_prepare_push_notifications",
 *   label = @Translation("Prepare push notifications"),
 *   category = @Translation("Nicol"),
 *   context = {
 *     "entity" = @ContextDefinition("entity",
 *       required = TRUE,
 *       label = @Translation("Entity"),
 *       description = @Translation("Specifies the entity, which should be pushed.")
 *     )
 *   }
 * )
 */
class NicolPushNotifications extends RulesActionBase {

  /**
   * Mode: node or comment.
   */
  protected $mode = 'node';

  /**
   * Sends the push notification.
   *
   * @param \Drupal\Core\Entity\EntityInterface $entity
   *   The entity to be pushed.
   */
  protected function doExecute(EntityInterface $entity) {

    // Check for comment entity (find entity)
    if ($entity->bundle() == 'berichten_updates') {
      // Save comment entity.
      $comment_entity = $entity;
      if ($commented_entity = $entity->getCommentedEntity()) {
        // Set entity to the commented node.
        $entity = $commented_entity;

        // Set modus for updates (comments)
        $this->mode = 'comment';
      }
    }

    // Check for incident nodes.
    if (preg_match('/incident|announcement/', $entity->bundle())) {

      $filters['message_type'] = [];

      // Get original entity.
      $original = $entity->original ? $entity->original : NULL;

      // Only create queue is entity is published.
      if (!$entity->isPublished()) {
        return;
      }

      // Only send push notifications for new or closed incidents.
      if (!$original || ($entity->get('field_status')->value == 'closed' && $original->get('field_status')->value == 'open')) {

        // Get all selected message_type for this incident node.
        $message_type = $entity->get('field_berichttype')->getvalue();

        // Get all children for parent terms (=services)
        foreach ($message_type as $term) {
          // Add this term to list of filter items.
          $filters['message_type'][$term['target_id']] = $term['target_id'];

          /*
          // Get all parent terms: services.
          $parents = \Drupal::entityTypeManager()->getStorage('taxonomy_term')->loadParents($term['target_id']);
          if (count($parents)) {
          $parent = reset($parents);

          // Add this parent to the list of filters.
          //$filters['message_type'][$parent->id()] = $parent->id();
          } else {
          $children = \Drupal::entityTypeManager()->getStorage('taxonomy_term')->loadChildren($term['target_id']);
          foreach ($children as $child) {
          $filters['message_type'][$child->id()] = $child->id();
          }
          }
           */
        }

        // Get all tokens from the system.
        // $result = push_notifications_get_tokens(array());
        $result_apns = push_notifications_get_tokens(["networks" => ["apns"]]);
        $result_gcm = push_notifications_get_tokens(["networks" => ["gcm"]]);
        $networks = ["gcm" => $result_gcm, "apns" => $result_apns];

        foreach ($networks as $network => $result) {
          $token_list = [];

          foreach ($result as $record) {
            if ($account = User::load($record->uid)) {

              // Skip blocked users.
              if (user_is_blocked($account->get('name')->value)) {
                continue;
              }

              // Prevent duplicates.
              if (in_array($account->id(), $token_list) ) {
                continue;
              }

              // Check for receive update notifications flag.
              if ($this->mode == 'comment') {
                if (!isset($record->nicol_notifications) || empty($record->nicol_notifications) || $record->nicol_notifications == 0) {
                  continue;
                }
              }

              // At least one of message_type is in user list.
              if ((count(array_intersect($filters['message_type'], unserialize($record->nicol_data))) > 0) || count($message_type) === 0) {
                // Add recipient for push notifications.
                // separate array for EZoef and Eflash app users
                $token_list[] = $record;
              }
            }
          }

          // Create queues to send push notifications.
          if ($this->mode == 'node') {
            $this->createQueue($entity, $token_list, "eflash_" . $network);
          }
          elseif ($this->mode == 'comment') {
            $this->createQueue($comment_entity, $token_list, "eflash_" . $network);
          }
        }
      }
    }

  }

  /**
   * Create queue item, based on push target, Ezoef or EZFlash
   *
   * @param \Drupal\Core\Entity\EntityInterface $entity
   * @param $token_list, $type
   * @param $type
   */
  private function createQueue(EntityInterface $entity, $token_list, $type) {

    // TODO: make this param configurable in GUI or settings.php
    // tokens per queue items
    $tokens_per_queue = 1000;

    // Set message for new or closed.
    $payload = $this->createPayload($entity);

    if ($this->mode == 'comment') {
      if ($commented_entity = $entity->getCommentedEntity()) {
        // Set entity to the commented node.
        $entity = $commented_entity;
      }
    }

    /** @var \Drupal\Core\Queue\QueueFactory $queue_factory */
    $queue_factory = \Drupal::service('queue');

    /** @var \Drupal\Core\Queue\QueueInterface $queue */
    $queue = $queue_factory->get('nicol_push_notifications_queue_' . $type);

    // statistics
    $total_push_tokens = count($token_list);
    if ($total_push_tokens > 0) {
      // split complete tokenlist in $tokens_per_queue
      $queue_items = array_chunk($token_list, $tokens_per_queue);
      foreach ($queue_items as $recipients) {
        $count = 0;
        // Add recipient to the queue.
        $item = new \stdClass();
        $item->tokens = $recipients;
        $item->title = $entity->getTitle();
        $item->payload = $payload;
        $item->items = $total_push_tokens;
        $item->counter = ($count * $tokens_per_queue + count($recipients));
        $item->timestamp = time();
        $item->type = $type;
        $queue->createItem($item);
        $count++;
      }
      $this->messenger()->addMessage(t('For %type, there are %recipients push-notifications ready to be send to the mobile devices.', ['%type' => $type, '%recipients' => $total_push_tokens]), 'status');
      // Logs a notice.
      \Drupal::logger('nicol_api')->notice('Sending push notification \'%title\' to a total of %recipients recipients.', ['%title' => $entity->getTitle(), '%recipients' => $total_push_tokens]);
    }
    else {
      $this->messenger()->addMessage(t('For %type, there are no devices registered for this message_type.', ['%type' => $type]), 'warning');
    }
  }

  /**
  * Private function strip_html
  * @param string $html_string
  *
  * @return string
  *   Returns string without html tags or entities
  */
  private function strip_html($html_string) {
    $string = strip_tags(html_entity_decode($html_string));
    $string = trim($string);
    return $string;
  }

  /**
  * Create payload message, Ezoef or EZFlash
  *
  * @param \Drupal\Core\Entity\EntityInterface $entity
  *   The entity to create payload for..
  *
  * @return array
  *   Returns payload or empty.
  */
  private function createPayload(EntityInterface $entity) {
    $payload = [];

    if ($this->mode == 'node') {
      // Set message for new or closed incidents.
      if ($entity->get('field_status')->value == 'closed') {
        $body = $this->strip_html($entity->get('body')->value);
        $title_type = ($entity->bundle() == 'incident') ? t('Closed incident') : t('Closed notification');
        $payload["title"] = $title_type . ': ' . $entity->getTitle();
      }
      else {
        $title_type = ($entity->bundle() == 'incident') ? t('New incident') : t('New notification');
        $payload["title"] = $title_type . ': ' . $entity->getTitle();
        $body = $entity->getTitle();
      }
    }
    elseif ($this->mode == 'comment') {
      $commented_entity = $entity->getCommentedEntity();

      $body = $this->strip_html($entity->get('comment_body')->value);
      $payload["title"] = t('Update') . ': ' . $commented_entity->getTitle();
    }
    $s = substr($body, 0, 1000);
    $payload["body"] = strlen($body) > 1000 ? substr($s, 0, strrpos($s, ' ')): $body;

    return $payload;
  }

  /**
  * Gets the messenger.
  *
  * @return \Drupal\Core\Messenger\MessengerInterface
  *   The messenger.
  */
  public function messenger() {
    if (!isset($this->messenger)) {
      $this->messenger = \Drupal::messenger();
    }
    return $this->messenger;
  }

}
