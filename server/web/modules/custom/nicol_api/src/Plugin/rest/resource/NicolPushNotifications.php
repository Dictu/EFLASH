<?php
// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


namespace Drupal\nicol_api\Plugin\rest\resource;

use Drupal\Core\Session\AccountProxyInterface;
use Drupal\rest\Plugin\ResourceBase;
use Drupal\rest\ModifiedResourceResponse;
use Symfony\Component\DependencyInjection\ContainerInterface;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;
use Symfony\Component\HttpKernel\Exception\HttpException;
use Drupal\Core\Entity\EntityStorageException;
use Psr\Log\LoggerInterface;

/**
 * Provides a resource to get view modes by entity and bundle.
 *
 * @RestResource(
 *   id = "nicol_push_notifications",
 *   label = @Translation("Nicol push notifications"),
 *   serialization_class = "",
 *   uri_paths = {
 *     "canonical" = "/api/v1/push_notifications/{device}",
 *     "https://www.drupal.org/link-relations/create" = "/api/v1/push_notifications"
 *   }
 * )
 */
class NicolPushNotifications extends ResourceBase {

  /**
   * A current user instance.
   *
   * @var \Drupal\Core\Session\AccountProxyInterface
   */
  protected $currentUser;

  /**
   * Constructs a Drupal\rest\Plugin\ResourceBase object.
   *
   * @param array $configuration
   *   A configuration array containing information about the plugin instance.
   * @param string $plugin_id
   *   The plugin_id for the plugin instance.
   * @param mixed $plugin_definition
   *   The plugin implementation definition.
   * @param array $serializer_formats
   *   The available serialization formats.
   * @param \Psr\Log\LoggerInterface $logger
   *   A logger instance.
   * @param \Drupal\Core\Session\AccountProxyInterface $current_user
   *   A current user instance.
   */
  public function __construct(
    array $configuration,
    $plugin_id,
    $plugin_definition,
    array $serializer_formats,
    LoggerInterface $logger,
    AccountProxyInterface $current_user) {
    parent::__construct($configuration, $plugin_id, $plugin_definition, $serializer_formats, $logger);

    $this->currentUser = $current_user;
  }

  /**
   * {@inheritdoc}
   */
  public static function create(ContainerInterface $container, array $configuration, $plugin_id, $plugin_definition) {
    return new static(
      $configuration,
      $plugin_id,
      $plugin_definition,
      $container->getParameter('serializer.formats'),
      $container->get('logger.factory')->get('nicol_api'),
      $container->get('current_user')
    );
  }

  /**
   * Responds to entity POST requests and registers a device token.
   * For type, pass \'ios\' for iOS devices and \'android\' for Android devices.
   *
   * @param array $data
   *   The device entity.
   *
   * @return \Drupal\rest\ResourceResponse
   *   The HTTP response object.
   *
   * @throws \Symfony\Component\HttpKernel\Exception\HttpException
   */
  public function post(array $data) {
    if (($data == NULL) || (!is_array($data)) || (empty($data))) {
      throw new BadRequestHttpException('No device content received.');
    }

    // You must to implement the logic of your REST Resource here.
    // Use current user after pass authentication to validate access.
    if (!$this->currentUser->hasPermission('access content')) {
      throw new AccessDeniedHttpException('Your device is not authorized.');
    }

    // Load user object.
    if ($user = user_load_by_name($this->currentUser->getAccountName())) {

      // Use current user after pass authentication to validate access.
      if ($user->isBlocked()) {
        throw new AccessDeniedHttpException('Your device is blocked.');
      }

      // Validate the e-mail before saving.
      if (empty($data['token'])) {
        throw new BadRequestHttpException('Invalid or unknown push token.');
      }

      // vaidate push token in case of iOS for APNs service
      if ( ($data['type'] == 'ios') && (!\preg_match('/^[0-9a-fA-F]{64}$/', $data['token'])) ) {
          throw new BadRequestHttpException('Invalid or unknown push token.');
      }

      // Validate the e-mail before saving.
      if (empty($data['type'])) {
        throw new BadRequestHttpException('Invalid or unknown device type (ios/android).');
      }

      // Convert type to integer value.
      if ($data['type'] != 'ios' && $data['type'] != 'android') {
        throw new BadRequestHttpException('Device type not supported.');
      }
      else {
        $type_id = ($data['type'] == 'ios') ? PUSH_NOTIFICATIONS_NETWORK_ID_IOS : PUSH_NOTIFICATIONS_NETWORK_ID_ANDROID;
      }

      // Validate the receive update notifications flag before saving.
      if (isset($data['notifications']) && is_array($data['notifications'])) {
        if (isset($data['notifications']['announcements']) && $data['notifications']['announcements'] == 1) {
          $notifications['announcements'] = 1;
        }
        else {
          $notifications['announcements'] = 0;
        }
        // Receive notifcations for updates.
        if (isset($data['notifications']['updates']) && $data['notifications']['updates'] == 1) {
          $notifications['updates'] = 1;
        }
        else {
          $notifications['updates'] = 0;
        }
      }
      else {
        $notifications['announcements'] = 0;
        $notifications['updates'] = 0;
      }
    }
    else {
      throw new AccessDeniedHttpException('Your device is not registered.');
    }

    // strip duplicate messagetypes
    $message_types = array_unique($data['messagetypes']);

    // Try to save device.
    try {
      // Get user id.
      $uid = $user->id();
      $uuid = $user->uuid();
      // add or replace user profile data
      // clear user applications and locations data
      unset($user->field_push_messagetypes);
      unset($user->field_push_services);
      if (count($message_types) > 0) {
        $user->set("field_push_messagetypes", $message_types);
      }

      $user->set("field_push_device_id", $data['token']);
      $user->set("field_push_device_type", $type_id);
      $user->set("field_push_enabled", 1);
      $user->set("field_push_announcements", $notifications['announcements']);
      $user->set("field_push_updates", $notifications['updates']);

      $user->save();

      if (\Drupal::moduleHandler()->moduleExists('push_notifications')) {

        // Retrieve push token.
        $token = $data['token'];

        // Insert or update token.
        $query = \Drupal::database()
          ->merge('push_notifications_tokens')
          ->key(['uid' => $uid])
          ->insertFields([
            'uid'                   => $uid,
            'uuid'                  => $uuid,
            'token'                 => $token,
            'network'               => $type_id,
            'langcode'              => \Drupal::languageManager()->getDefaultLanguage()->getId(),
            'created'               => time(),
            'nicol_data'            => serialize($message_types),
            'nicol_notifications'   => $notifications['updates']
          ])
          ->updateFields([
            'token'                 => $token,
            'network'               => $type_id,
            'nicol_data'            => serialize($message_types),
            'nicol_notifications'   => $notifications['updates']
          ]);
        $query->execute();

        // Logs a notice.
        \Drupal::logger('nicol_api')->notice('Push notification %token was successfully stored in the database for user %uid.', ['%token' => $token, '%uid' => $uid]);

        return new ModifiedResourceResponse([
          'success' => 1,
          'message' => 'Push notification was successfully stored in the database.',
          'token' => $token
        ]);
      }
    }
    catch (EntityStorageException $e) {
      \Drupal::logger('nicol_api')->error('Push notification could not be stored for user %uid.', ['%uid' => $uid]);

      throw new HttpException(500, 'Internal Server Error', $e);
    }
  }

  /**
   * Removes a registered a push token. Only needs the token.
   *
   * @param \Drupal\user\UserInterface $device
   *   The device entity.
   *
   * @return \Drupal\rest\ResourceResponse
   *   The HTTP response object.
   *
   * @throws \Symfony\Component\HttpKernel\Exception\HttpException
   */
  public function delete($device = NULL) {

    if ($device == NULL) {
      throw new BadRequestHttpException('No device content received.');
    }

    // Try to delete device.
    try {
      $user = user_load_by_name($this->currentUser->getAccountName());
      $user->set("field_push_enabled", 0);
      $user->save();

      $query = \Drupal::database()->delete('push_notifications_tokens');
      $query->condition('token', $device);
      $result = $query->execute();

      // Logs a notice.
      \Drupal::logger('nicol_api')->notice('Token %token was successfully removed from the database.', ['%token' => $device]);
      $return = [
        'success' => 1,
        'message' => 'Token was successfully removed from the database.'
      ];

      return new ModifiedResourceResponse(NULL, 204);

    }
    catch (EntityStorageException $e) {
      \Drupal::logger('nicol_api')->error('Token %token could not be removed.', ['%token' => $device]);

      throw new HttpException(500, 'Internal Server Error', $e);
    }
  }

}
