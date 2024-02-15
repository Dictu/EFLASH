<?php
// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


namespace Drupal\nicol_api\Plugin\rest\resource;

use Drupal\comment\Entity\Comment;
use Drupal\Core\Session\AccountProxyInterface;
use Drupal\rest\Plugin\ResourceBase;
use Drupal\rest\ResourceResponse;
use Symfony\Component\DependencyInjection\ContainerInterface;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;

use Psr\Log\LoggerInterface;

/**
 * Provides a resource to get view modes by entity and bundle.
 *
 * @RestResource(
 *   id = "nicol_announcements",
 *   label = @Translation("Nicol announcements"),
 *   serialization_class = "",
 *   uri_paths = {
 *     "canonical" = "/api/v1/announcements",
 *     "https://www.drupal.org/link-relations/create" = "/api/v1/announcements"
 *   }
 * )
 */
class NicolAnnouncements extends ResourceBase {

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
   * @param $data
   *
   * @return \Drupal\rest\ResourceResponse Throws exception expected.
   *
   * @throws \Symfony\Component\HttpKernel\Exception\HttpException
   *   Throws exception expected.
   */
  public function post(array $data) {

    if ($data == NULL) {
      throw new BadRequestHttpException('No parameters received.');
    }

    // Use current user after pass authentication to validate access.
    if (!$this->currentUser->hasPermission('access content')) {
      throw new AccessDeniedHttpException('Your device is not authorized.');
    }

    // Load user object.
    $user = user_load_by_name($this->currentUser->getAccountName());

    // Check if user is blocked.
    if ($user->isBlocked()) {
      throw new AccessDeniedHttpException('Your device is blocked.');
    }

    $status = $data['state'];
    $message_types = $data['messagetypes'];

    if (empty($message_types)) {
      return new ResourceResponse([]);
    }

    $result = $this->getAnnouncements($status, $message_types);

    return new ResourceResponse($result);
  }

  /**
   * Private function getAnnouncements
   * @param array $status
   *   A configuration array containing information about the plugin instance.
   *
   * @param array $filter
   *   The term_ids which are  the location_ids or service_ids from vocabulary locations
   *
   * @return array
   *   Returns a list announcements filtered optional status and service/location data
   */
  private function getAnnouncements($status = ['open', 'closed'], $filter = []) {

    // Get services and locations from the requested filter array
    $result = [];

    $query = \Drupal::entityQuery('node')
      ->condition('type', 'announcement')
      ->condition('status', 1)
      ->condition('field_status', $status, 'IN');
    $group = $query->orConditionGroup()
      ->condition('field_berichttype.entity.tid', $filter, 'IN')
    // is case an announcement has no messagetype
      ->condition('field_berichttype', NULL, 'IS NULL');
    $nids = $query->condition($group)->sort('created', 'DESC')->execute();

    if (!empty($nids)) {

      // Load an array of node objects keyed by node ID.
      $nodes = \Drupal::entityTypeManager()->getStorage('node')->loadMultiple($nids);

      // build return_data array with only the values the app needs
      // nid, title, created, changed, body, status, locations, comments
      foreach ($nodes as $node) {
        $data = [
          'id'      => $node->id(),
          'title'   => $node->title->value,
          'body'    => $node->body->value,
          'created' => \Drupal::service('date.formatter')->format($node->created->value, 'custom', 'Y-m-d\TH:i:sO'),
          'changed' => \Drupal::service('date.formatter')->format($node->changed->value, 'custom', 'Y-m-d\TH:i:sO'),
          'status'  => $node->field_status->value
        ];

        $message_types = $this->getMessageTypes($node->get('field_berichttype')->referencedEntities());
        $data['messagetypes'] = $message_types['messagetypes'];
        $data['messagestreams'] = $message_types['messagestreams'];
        $data['field_updates'] = $this->getComments($node->id());

        $result[] = $data;
      }
    }

    return $result;
  }

  /**
   * Private function getMessageTypes
   * @param array $data
   *   The term_ids from the locations vocabulary
   *
   * @return array
   *   Returns a array with separated services and locations.
   */
  private function getMessageTypes(array $data) {
    $stream = [];
    $types = [];

    foreach ($data as $value) {
      // check if term_parent == 0 --> it is a service
      // if parent_term_id <> 0 --> it is a location.
      // Term has only one parent!!!!
      $parent = \Drupal::entityTypeManager()->getStorage('taxonomy_term')->loadParents($value->id());
      // only one parent is possible
      $parent = reset($parent);

      if (empty($parent)) {
        $stream[$value->id()] = ["tid" => $value->id(), "name" => $value->name->value];
      }
      else {
        $types[$value->id()] = ["tid" => $value->id(), "name" => $value->name->value];
        $stream[$parent->id()] = ["tid" => $parent->id(), "name" => $parent->name->value];
        // also get the parent stuf for this node_id
      }

    }

    $streams = array_values($stream);
    $types   = array_values($types);

    return [
      'messagetypes' => $types,
      'messagestreams' => $streams
    ];

  }

  /**
   * Private function getComments
   * @param int $node_id
   *
   * @return array
   *   Returns a list comments (updates) for given entity_id
   */
  private function getComments($node_id) {
    $comments = [];

    $cids = \Drupal::entityQuery('comment')
      ->condition('entity_id', $node_id)
      ->condition('entity_type', 'node')
      ->sort('cid', 'DESC')
      ->execute();

    foreach ($cids as $cid) {
      $comment = Comment::load($cid);
      $comments[] = [
        'cid' => $cid,
        //  'uid' => $comment->getOwnerId(),
        //  'subject' => $comment->get('subject')->value,
        'update' => $comment->comment_body->value,
        'created' => \Drupal::service('date.formatter')->format($comment->get('created')->value, 'custom', 'Y-m-d\TH:i:sO')
      ];
    }

    return $comments;
  }

  /**
   * Private function strip_html
   * @param string $html_string
   *
   * @return array
   *   Returns string without html tags or entities
   */
  private function strip_html($html_string) {
    $string = strip_tags(html_entity_decode($html_string));
    $string = trim($string);
    return $string;
  }

}
