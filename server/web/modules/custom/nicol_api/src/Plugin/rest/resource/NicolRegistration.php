<?php
// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


namespace Drupal\nicol_api\Plugin\rest\resource;

use Drupal\Core\Session\AccountProxyInterface;
use Drupal\rest\Plugin\ResourceBase;
use Drupal\rest\ResourceResponse;
use Symfony\Component\DependencyInjection\ContainerInterface;
use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;
use Psr\Log\LoggerInterface;
use Drupal\simple_oauth\AccessTokenInterface;
use Drupal\user\Entity\User;

/**
 * Provides a resource to get view modes by entity and bundle.
 *
 * @RestResource(
 *   id = "nicol_registration",
 *   label = @Translation("Nicol registration"),
 *   serialization_class = "",
 *   uri_paths = {
 *     "canonical" = "/api/v1/register",
 *     "https://www.drupal.org/link-relations/create" = "/api/v1/register"
 *   }
 * )
 */
class NicolRegistration extends ResourceBase {

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
   * Responds to POST requests.
   *
   * @param array $data
   *   A user-account object containing the post data.
   *
   * @return array
   *   Returns ResourceResponse success or failure.
   *
   * @throws \Symfony\Component\HttpKernel\Exception\HttpException
   *   Throws exception expected.
   */
  public function post(array $data) {

    // Use current user after pass authentication to validate access.
    // No user exists, so anonymous is logged.
    // if (!$this->currentUser->hasPermission('access content')) {
    //   throw new AccessDeniedHttpException();
    // }

    if ($data == NULL) {
      throw new BadRequestHttpException('No registration details received.');
    }

    // TODO: Validate accountname, 40 chars string [a-z-0-9] allowed.
    // Validate the device ID before saving.
    if (empty($data["name"])) {
      throw new BadRequestHttpException('Invalid or unkown device ID.');
    }

    // Validate the e-mail before saving.
    if (empty($data["mail"]) || !\Drupal::service('email.validator')->isValid($data["mail"])) {
      throw new BadRequestHttpException('Invalid or unknown email address.');
    }

    $client_domain = substr(strrchr($data["mail"], "@"), 1);

    if (!taxonomy_term_load_multiple_by_name($client_domain, 'app_domains')) {
      // throw new BadRequestHttpException('Domain for e-mail address \'' . $data["mail"] . '\' is not allowed.');
      return new ResourceResponse([
        'success' => 0,
        'message' => t("Domain for e-mail address \'@mail\' is not allowed.", ['@mail' => $data["mail"]])
      ]);
    }

    // Apple review account.
    if ($data["mail"] == 'apple_review@dictu.nl') {
      if ($account = user_load_by_name('apple_review')) {
        $data["name"] = 'apple_review';

        // Logs a notice.
        \Drupal::logger('nicol_api')->notice('Apple device (@name) registration.', ['@name' => $account->getAccountName()]);
      }
    }

    // ezoef app users get ezoef app role
    $user_role = (substr(strrchr($data["mail"], "@"), 1) == EZ_EFLASH_DOMAIN) ? EZ_EFLASH_DRUPAL_ROLE : EZ_EZOEF_DRUPAL_ROLE;
    // EZFLASH USERS don't have to be activated by email
    $status = ($user_role == EZ_EFLASH_DRUPAL_ROLE) ? 1 : 0;

    // Deactivate already registered device and resend activation mail.
    if ($account = user_load_by_name($data["name"])) {
      if (!in_array('administrator', $account->getRoles())) {

        // Don't block apple_review account.
        if ($data["mail"] <> 'apple_review@dictu.nl') {
          // Logs a notice.
          \Drupal::logger('nicol_api')
            ->notice('Blocked device (@name), already registered.', ['@name' => $account->getAccountName()]);

          // Block ezoef  users, resend activation email.
          if ($status == 0) {
            $account->block();
          }
        }

        // Reset init email address.
        $account->set('init', $data["mail"]);

        // Initialise push notification.
        // $push = new PushNotificationsController();

        // Purge old push token registrations.
        if ($tokens = push_notification_get_user_tokens($account->id())) {
          foreach ($tokens as $token) {
            $result = $this->push_notifications_purge_token($token->token);

            // Logs a notice.
            \Drupal::logger('nicol_api')->notice('Purged push notifications for device (@name).', ['@name' => $account->getAccountName()]);
          }
        }
      }
    }
    else {
      $account = User::create([
        'name' => $data["name"],
        'mail' => $data["name"] . '@nicolapp.nl',
        'init' => $data["mail"],
        'pass'  => user_password(),
        'roles' => [$user_role],
        'status' => $status,
        'access' => 0,
        'login' => 0
      ]);
    }
    // TODO: Do not send activation emails when registering for EZ Flash app.

    // TODO: Do not send mail when user has clicked in the confirmation mail. (EZOef app)

    // Username is unique for a mobile device.
    $real_email = $data["mail"];

    // Multiple devices can have the same email address, store mail address in custom field.
    $account->set('field_registered_email_address', $real_email);

    // Save (or update) user.
    $account->save();
    // logout again to clear the anonymous session
    $request = \Drupal::request();
    $request->getSession()->clear();

    // Account successfully created, generate the oAuth tokens.
    $oauth_tokens = $this->_get_oauth_tokens($account->id());

    // Send an activation mail only for eZoefapp
    // $user = user_load($account->id());
    $result = ($user_role == EZ_EZOEF_DRUPAL_ROLE) ? $this->nicol_mail_notify('register_confirmation_with_pass', $account) : NULL;

    // Logs a notice.
    \Drupal::logger('nicol_api')->notice('Successfully registered device (@name) for mail: %mail.', ['%mail' => $real_email, '@name' => $data["name"]]);

    return new ResourceResponse([
      'success' => 1,
      'message' => t('Successfully registered device for @mail, activation mail send.', ['@mail' => $real_email]),
      'data' => $oauth_tokens
    ]);

  }

  /**
   * Conditionally create and send a notification email when a certain
   * operation happens on the given user account.
   *
   * @param string $op
   *   The operation being performed on the account. Possible values:
   *   - 'register': Activation link for user created by the Nicol rest api.
   *
   * @param \Drupal\Core\Session\AccountInterface $account
   *   The user object of the account being notified. Must contain at
   *   least the fields 'uid', 'name', and 'mail'.
   * @param string $langcode
   *   (optional) Language code to use for the notification, overriding account
   *   language.
   *
   * @return array
   *   An array containing various information about the message.
   *   See \Drupal\Core\Mail\MailManagerInterface::mail() for details.
   *
   * @see user_mail_tokens()
   */
  private function nicol_mail_notify($op, $account, $langcode = NULL) {
    // By default, we always notify except for canceled and blocked.
    // $notify = \Drupal::config('user_registrationpassword')->get('notify.' . $op);
    $params['account'] = $account;
    $langcode = $langcode ? $langcode : $account->getPreferredLangcode();
    // Get the custom site notification email to use as the from email address
    // if it has been set.
    $site_mail = \Drupal::config('system.site')->get('mail_notification');
    // If the custom site notification email has not been set, we use the site
    // default for this.
    if (empty($site_mail)) {
      $site_mail = \Drupal::config('system.site')->get('mail');
    }
    if (empty($site_mail)) {
      $site_mail = ini_get('sendmail_from');
    }

    $mail = \Drupal::service('plugin.manager.mail')->mail('user_registrationpassword', $op, $account->get('field_registered_email_address')->value, $langcode, $params, $site_mail);

    return empty($mail) ? NULL : $mail['result'];
  }

  /**
   * Create oauth tokens for given user_id.
   *
   * @param string $uid
   *   A user-account object who gets the oauth tokens.
   *
   * @return bool|array
   *   contains the tokens
   */
  private function _get_oauth_tokens($uid) {

    if (\Drupal::moduleHandler()->moduleExists('simple_oauth')) {

      // Find / generate the access token for this refresh token.
      $extension = \Drupal::config('simple_oauth.settings')->get('refresh_extension') ?: static::REFRESH_EXTENSION_TIME;

      // If there is no token to be found, refresh it by generating a new one.
      $request_time = \Drupal::time()->getRequestTime();
      $values = [
        'expire' => [$request_time + $extension],
        'user_id' => $uid,
        'auth_user_id' => $uid,
        'resource' => 'global',
        'created' => $request_time,
        'changed' => $request_time,
      ];

      /* @var AccessTokenInterface $access_token */
      $access_token = \Drupal::entityTypeManager()
        ->getStorage('access_token')
        ->create($values);

      // Saving this token will generate a refresh token for that one.
      $access_token->save();

      return $this->normalize($access_token);
    }

    return FALSE;
  }

  /**
   * Serializes the token either using the serializer or manually.
   *
   * @param \Drupal\simple_oauth\AccessTokenInterface $token
   *   The token.
   *
   * @return string
   *   The serialized token.
   */
  protected function normalize(AccessTokenInterface $token) {
    $request_time = \Drupal::time()->getRequestTime();
    $storage = \Drupal::entityTypeManager()
      ->getStorage('access_token');

    $ids = $storage
      ->getQuery()
      ->condition('access_token_id', $token->id())
      ->condition('expire', $request_time, '>')
      ->condition('resource', 'authentication')
      ->range(0, 1)
      ->execute();

    if (empty($ids)) {
      // TODO: Add appropriate error handling. Maybe throw an exception?
      return [];
    }

    $refresh_token = $storage->load(reset($ids));

    if (!$refresh_token || !$refresh_token->isRefreshToken()) {
      // TODO: Add appropriate error handling. Maybe throw an exception?
      return [];
    }

    return [
      'access_token' => $token->get('value')->value,
      'token_type' => 'Bearer',
      'expires_in' => $token->get('expire')->value - $request_time,
      'refresh_token' => $refresh_token->get('value')->value,
    ];
  }

  /**
  * purge push_notification tokens
  *
  * @param string $token
  *   The token.
  *
  * @return string
  *   query result.
  */
  private function push_notifications_purge_token($token = '', $type_id = '') {
    if ($token == '' || !is_string($token)) {
      return FALSE;
    }
    $query = db_delete('push_notifications_tokens');
    $query->condition('token', $token);
    $result = $query->execute();
    return $result;
  }

}
